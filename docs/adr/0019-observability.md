# ADR-0019: Observability — Prometheus + Grafana + Loki خودمیزبان، Correlation ID سبک

- **وضعیت:** پذیرفته‌شده (Phase 17)
- **تصمیم‌گیرندگان:** Architect + کارفرما (دو تصمیم صریح از طریق AskUserQuestion)

## Context

طبق تصمیم صریح کارفرما، فاز ۱۷ «Observability و Monitoring» تعیین شد. تا این فاز، هیچ Metric ساختاریافته، Dashboard، Log ساختاریافته یا شناسه‌ی دنبال‌کردن درخواست وجود نداشت -- فقط `/actuator/health` ساده و خروجی متنی پیش‌فرض Logback به Console. با توجه به اولویت صریح بریف (`Security > Correctness > Maintainability > Scalability > Observability > ...`)، این فاز مستقیماً همان لایه‌ی چهارم اولویت را عملی می‌کند.

## Decision 1: پشته‌ی خودمیزبان (Prometheus + Grafana + Loki)، نه SaaS

با AskUserQuestion صریح، سه مسیر به کارفرما ارائه شد: پشته‌ی کامل خودمیزبان، حداقلی (فقط Log + Actuator بدون Dashboard)، یا SaaS (Datadog/Grafana Cloud). کارفرما **پشته‌ی خودمیزبان** را انتخاب کرد -- دقیقاً هم‌الگوی تصمیم‌های قبلی این پروژه (Postgres/Redis/Keycloak خودشان در `infra/docker-compose.yml`، و صریحاً در ADR-0018 برای Push: «بدون واسطه‌ی شخص‌ثالث»). هیچ داده‌ای (Metric/Log) این‌جا به یک سرویس خارجی ارسال نمی‌شود.

## Decision 2: Correlation ID سبک، نه OpenTelemetry کامل

با AskUserQuestion دوم، عمق Tracing صریح پرسیده شد. کارفرما **Correlation ID سبک** را انتخاب کرد به‌جای OpenTelemetry کامل + Trace Backend (Tempo/Zipkin) -- سرویس/Container جدید و Instrumentation بیشتر برای این مقیاس پروژه توجیه نداشت.

**پیاده‌سازی:** `CorrelationIdFilter` (یک `OncePerRequestFilter` با `@Order(HIGHEST_PRECEDENCE)`) یک `X-Request-Id` از فراخوانی‌کننده می‌پذیرد یا می‌سازد، در MDC می‌گذارد (پس در هر خط Log ساختاریافته حاضر است)، و در Header پاسخ برمی‌گرداند. **عمداً به سیستم‌های خارجی (Django/Zibal/SMS.ir/FCM) منتشر نشد** -- بدون امکان واقعی دیدن Logهای داخلی آن سیستم‌ها، این Header فقط نادیده گرفته می‌شد؛ افزودن یک Header بدون مصرف‌کننده‌ی واقعی، دقیقاً همان «ساخت برای نیاز فرضی» است که این پروژه از آن پرهیز می‌کند. ارزش واقعی این ایده وقتی معنا پیدا می‌کند که یا Django هم همین الگو را بپذیرد (تصمیم آینده، نیازمند تغییر کد Django) یا پروژه به OpenTelemetry کامل ارتقا یابد.

## پیاده‌سازی

### Metrics (بدون Dependency جدید غیرضروری)
- `io.micrometer:micrometer-registry-prometheus` -- تنها Dependency جدید. با همین یکی، Micrometer/Actuator خودکار Meterهای زیر را بدون هیچ کد اضافه صادر می‌کنند:
  - `http_server_requests_seconds_*` (نرخ/تأخیر/کد وضعیت هر Endpoint)
  - `hikaricp_connections_*` (Active/Idle/Pending/Max) -- **مستقیماً مرتبط با کشف Phase 16**؛ بدون این نمودار، تمام‌شدن Pool فقط از طریق ۵۰۰ واقعی کاربر دیده می‌شد.
  - `jvm_memory_used_bytes`, `process_cpu_usage`
  - `resilience4j_circuitbreaker_*` -- برای هر سه Provider واقعی که از قبل `@CircuitBreaker` دارند (Zibal، SMS.ir، FCM از Phase 16)، بدون تغییر آن کد.
- دو Metric سفارشی، فقط همین دو -- چون هیچ منبع دیگری آن‌ها را نمی‌داد:
  - `sepahan.jit.race.conflicts{entity}` -- شمارنده‌ی باخت مسابقه در JIT Provisioning (users/loyalty)، دقیقاً همان الگویی که در Phase 15/16 کشف شد؛ افزایش آن لزوماً خطا نیست (Recovery بدون خطا انجام می‌شود)، فقط دید عملیاتی واقعی به فرکانس این هم‌زمانی می‌دهد.
  - `sepahan.notifications.sent{channel,provider,status}` -- در `NotificationService.saveLog` (تنها نقطه‌ی مشترک همه‌ی کانال‌ها)، چون `fake`/`smtp` هیچ Circuit Breaker ندارند و بدون این، دید یکپارچه‌ای روی همه‌ی کانال‌ها وجود نداشت.
- `/actuator/prometheus` روی همان Port عمومی (۸۰۸۱، نه Port جدا) اضافه شد -- تصمیم آگاهانه، نه سهل‌انگاری: Port جدای Actuator در اسپرینگ بوت یک Context امنیتی جدا و کمتر شناخته‌شده برای این پروژه می‌سازد که رفتار دقیق آن با Spring Security بدون تست مستقیم قابل‌اعتماد نبود؛ به‌جایش این مسیر با `permitAll()` صریح در همان `SecurityConfig` موجود (که کاملاً شناخته‌شده است) اضافه شد. **این یک TODO امنیتی صریح است:** پیش از هر Deployment عمومی باید یا از پشت یک Reverse Proxy فقط به IP سرور Monitoring محدود شود یا Basic Auth بگیرد -- بررسی کامل آن موضوع Phase 19 (Security Audit) است.

### Logging ساختاریافته (بدون Dependency جدید)
Spring Boot از نسخه‌ی ۳.۴ خروجی JSON ساختاریافته دارد (`logging.structured.format.file: logstash`، تأییدشده در همین فاز -- نه حدس). فقط خروجی **فایل** JSON است، نه Console -- ترمینال محلی برای توسعه/عیب‌یابی خواناتر می‌ماند. طبق مستندات رسمی، فرمت logstash تمام مقادیر MDC را خودکار به شیء JSON اضافه می‌کند -- پس `requestId` بدون هیچ کد اضافی در هر خط Log حاضر است (تأیید زنده در همین فاز: یک درخواست واقعی، همان Request Id در Header پاسخ و در خط Log متناظر).

### Log Shipping (Promtail -> Loki)
چون Backend روی خودِ Host اجرا می‌شود (نه Container -- طبق روال این پروژه از Phase 6)، Promtail از طریق یک Volume مشترک (`backend/logs` -> `/var/log/backend`) فایل را می‌خواند، نه از طریق Docker Socket. برچسب‌گذاری Promtail فقط سطح Job/Service است؛ تجزیه‌ی فیلدهای JSON در خودِ Grafana با LogQL (`| json`) انجام می‌شود -- تا با تغییر احتمالی شکل خروجی Structured Logging اسپرینگ نیازی به تغییر پیکربندی Promtail نباشد.

### شبکه: `host.docker.internal`
Prometheus (داخل Docker) باید به Port Host برسد. روی Docker Desktop این خودکار کار می‌کند؛ روی Docker Engine خالص لینوکس (این محیط) نیاز به `extra_hosts: host.docker.internal:host-gateway` دارد -- اضافه و تأیید شد.

### Dashboardها (Grafana، Provision‌شده خودکار)
دو Dashboard آماده در `infra/grafana/dashboards/` (بدون کلیک دستی -- طبق الگوی این پروژه از Import دستی Realm در Phase 4 که به IaC ترجیح داده شد):
- **Backend Overview:** نرخ/خطای HTTP، تأخیر p95، حافظه‌ی JVM، اتصالات HikariCP، CPU.
- **Business Metrics:** باخت مسابقه‌ی JIT، ارسال Notification بر اساس کانال/نتیجه، وضعیت Circuit Breaker، خطاهای اخیر (Panel مستقیم Loki).

## تست شده

- `/actuator/prometheus` زنده تأیید شد: `hikaricp_connections_max`, `http_server_requests_seconds_*`, `jvm_memory_used_bytes` همه واقعاً صادر می‌شوند.
- `CorrelationIdFilter` زنده تأیید شد: یک درخواست واقعی (ارسال SMS آزمایشی) دقیقاً همان `X-Request-Id` را هم در Header پاسخ و هم در خط JSON متناظر در `backend/logs/backend.json.log` نشان داد.
- `sepahan_notifications_sent_total{channel="sms",provider="fake",status="sent"}` بعد از همان درخواست واقعی در `/actuator/prometheus` ظاهر شد.
- Suite کامل تست (۵۴ تست غیرمرتبط با Zibal) بدون رگرسیون سبز ماند.
- Prometheus/Loki/Promtail/Grafana با `docker-compose up` واقعاً بالا آمدند؛ Datasourceها و هر دو Dashboard از طریق Provisioning خودکار (بدون کلیک دستی) ظاهر شدند.

## Alternatives Considered

| گزینه | چرا رد شد |
|---|---|
| SaaS (Datadog/Grafana Cloud) | ارسال Metric/Log به یک سرویس خارجی؛ برخلاف الگوی «بدون واسطه»ی این پروژه؛ هزینه‌ی دوره‌ای |
| OpenTelemetry کامل + Tempo/Zipkin | سرویس/Container جدید و Instrumentation بیشتر؛ کارفرما صریحاً عمق سبک‌تر را ترجیح داد |
| Port جدای Actuator (`management.server.port`) | رفتار امنیتی دقیق آن با Spring Security در این نسخه بدون تست مستقیم قابل‌اعتماد نبود؛ ماندن روی همان Port با `permitAll()` صریح، خطرش شناخته‌شده و مستند است |
| logstash-logback-encoder (Dependency خارجی) | Spring Boot 3.4+ همین قابلیت را بدون Dependency جدید دارد |
| انتشار X-Request-Id به Django/Zibal/SMS.ir/FCM | بدون مصرف‌کننده‌ی واقعی در آن سمت، صرفاً یک Header بی‌استفاده بود |

## Consequences

**مثبت:** برای اولین بار، این پروژه دید عملیاتی واقعی روی Pool اتصال، تأخیر HTTP، و فرکانس واقعی هم‌زمانی JIT Provisioning دارد -- دقیقاً همان چیزی که کشف باگ‌های Phase 15/16 را کند و دستی کرده بود.

**ریسک/نیازمند توجه:** `/actuator/prometheus` روی همان Port عمومی و بدون محدودیت شبکه‌ای است -- TODO امنیتی صریح برای Phase 19. Loki در حالت Filesystem تک‌باینری اجرا می‌شود (فقط توسعه؛ طبق مستندات رسمی خودش، نه برای نگه‌داری بلندمدت Production). Correlation ID فقط داخل این Backend معنا دارد، نه در مرز با Django/Providerهای خارجی.
