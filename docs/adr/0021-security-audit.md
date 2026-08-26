# ADR-0021: ممیزی امنیتی — Backend/Frontend/Infra (Phase 19)

- **وضعیت:** پذیرفته‌شده (Phase 19)
- **تصمیم‌گیرندگان:** Architect (بدون AskUserQuestion جدید این فاز — طبق اولویت اول بریف «Security»، رفع نقص‌های مشخص و مستقل قابل‌تأیید نیازمند تصمیم دوگانه‌ی معماری نبود)

## Context

بعد از تکمیل استراتژی تست ([ADR-0020](0020-test-strategy.md))، این فاز یک ممیزی امنیتی هدفمند روی کل سطح پروژه بود: سه Agent مستقل و موازی (بدون تغییر کد، فقط گزارش) هرکدام یک دامنه را بررسی کردند — Backend (Auth/RBAC/Injection/Secrets/Logging)، Frontend (Admin Panel + Mobile)، و Infra (پیکربندی Realm Keycloak/Dependencyها/سرویس‌های در معرض دید). هر گزارش صریحاً شامل بخش «چه چیزی بررسی و **تأیید سالم** شد» بود، نه فقط مشکلات — طبق اصل «Trust but verify»، هر یافته‌ی مشخص/قابل‌اقدام قبل از رفع، مستقل بازبینی شد (خواندن مستقیم فایل واقعی، یا Query از Keycloak/Postgres زنده)، نه فقط پذیرفتن گزارش Agent.

نتیجه: چند نقص واقعی و مستقل کشف و رفع شد (با تست/تأیید زنده)، و چند یافته‌ی کم‌اولویت‌تر یا معماری‌محور با دلیل مستند به تعویق افتاد.

## یافته‌ها و رفع‌ها

### ۱. IDOR در لغو Device Token (شدت: بالا)

`NotificationController.unregister(token)` قبلاً بدون هیچ بررسی مالکیتی، هر Token را با مقدار خودش پیدا و غیرفعال می‌کرد — هر کاربر احراز هویت‌شده‌ای که مقدار Token شخص دیگری را (مثلاً از یک لاگ افشاشده یا حدس) می‌دانست، می‌توانست اعلان‌های او را بی‌صدا خاموش کند. رفع: امضای Endpoint به `unregister(token, @AuthenticationPrincipal Jwt jwt)` تغییر کرد و قبل از غیرفعال‌سازی، مالکیت (`userId` استخراج‌شده از JWT در برابر `DeviceToken.userId`) بررسی می‌شود. پاسخ عمداً همچنان `204 No Content` در هر دو حالت (موفق/رد به‌خاطر عدم مالکیت) است — تا وجود/عدم‌وجود یک Token برای شخص دیگر افشا نشود (الگوی مشابه IDORهای دیگر پروژه). پوشش تست: `deviceTokenUnregister_byDifferentUser_doesNotDeactivateToken` (رد) و `deviceTokenUnregister_byOwner_deactivatesToken` (موفق) در `SecurityRbacIntegrationTest` — از طریق یک درخواست HTTP واقعی، نه فقط Service Layer.

الگوی تثبیت‌شده‌ی بقیه‌ی پروژه (`findByIdAndUserId` در همه‌جای دیگر) از قبل درست بود؛ این تک‌مورد از قلم افتاده بود.

### ۲. `/actuator/prometheus` بدون محدودیت شبکه‌ای (شدت: متوسط)

از Phase 17 (ADR-0019) این مسیر عمداً `permitAll()` بود چون Prometheus بدون Bearer Token اسکرِیپ می‌کند — با یک TODO امنیتی صریح برای همین فاز. رفع: به‌جای `permitAll()`، یک `AuthorizationManager<RequestAuthorizationContext>` اختصاصی (`MonitoringNetworkAuthorizationManager`) فقط Loopback + محدوده‌های خصوصی RFC 1918 (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`) را مجاز می‌کند — دقیقاً همان بازه‌هایی که Docker Bridge (Prometheus داخل Compose)، یک Host محلی، یا یک VPC داخلی از آن‌ها استفاده می‌کنند؛ یک IP عمومی واقعی رد می‌شود.

**چرا کلاس جدا، نه یک Lambda داخل `SecurityConfig`:** تا هر دو مسیر (مجاز/رد) مستقیم و بدون بالا آوردن یک Server واقعی با `MockHttpServletRequest.setRemoteAddr(...)` تست شوند — `MonitoringNetworkAuthorizationManagerTest` (۵ تست: Loopback IPv4/IPv6، Docker Bridge، LAN خصوصی، و یک IP عمومی واقعی از بازه‌ی مستندسازی TEST-NET-3 طبق RFC 5737). `SecurityRbacIntegrationTest` فقط مسیر مجاز را از طریق `TestRestTemplate` واقعی اثبات می‌کند (چون آن درخواست همیشه از Loopback می‌آید).

**نکته‌ی فنی کشف‌شده حین پیاده‌سازی:** `hasIpAddress()` دیگر مستقیم داخل DSL جدید `authorizeHttpRequests()` اسپرینگ سکیوریتی ۶.x قابل استفاده نیست (قبل از پیاده‌سازی با WebSearch تأیید شد، نه حدس) — جایگزین صحیح، `IpAddressMatcher` + یک `AuthorizationManager`/`.access()` سفارشی است. `IpAddressMatcher` هرگز IPv4 و IPv6 را باهم مطابقت نمی‌دهد، پس `::1/128` صریح در کنار `127.0.0.1/32` لازم بود (چون `localhost` گاهی به IPv6 Resolve می‌شود).

**محدودیت شناخته‌شده:** بدون Reverse Proxy جلوی این Backend، `getRemoteAddr()` دقیقاً همان IP واقعی TCP Peer است. اگر در Phase 20 (Deployment) یک Reverse Proxy اضافه شود، این بررسی باید با اعتماد صریح و پیکربندی‌شده به `X-Forwarded-For` (نه پیش‌فرض، چون این Header به‌سادگی توسط Client قابل جعل است) بازنگری شود.

### ۳. نشت اطلاعات شخصی (PII) در لاگ Providerهای آزمایشی (شدت: پایین-متوسط)

`FakeSmsProvider`/`FakeEmailProvider` شماره‌موبایل/ایمیل کامل گیرنده را مستقیم در لاگ ثبت می‌کردند. رفع: هر دو قبل از لاگ، شناسه‌ی گیرنده را Mask/Truncate می‌کنند (مثلاً `0912***۶۷۸۹`) — کافی برای Debug (تشخیص گیرنده‌ی کلی) بدون افشای کامل PII در فایل لاگ (که در Phase 17 حالا به Loki هم ارسال می‌شود — ADR-0019).

### ۴. عدم اعتبارسنجی `X-Request-Id` ورودی از Client (شدت: پایین)

`CorrelationIdFilter` (Phase 17) هر مقدار دلخواه Client را مستقیم در MDC/Header پاسخ/لاگ قرار می‌داد — یک Client بدخواه می‌توانست یک رشته‌ی بسیار بلند یا حاوی کاراکترهای کنترلی/تزریق لاگ ارسال کند. رفع: یک الگوی `^[a-zA-Z0-9-]{1,64}$` اضافه شد — هر مقدار نامعتبر رد و یک UUID جدید (رفتار قبلی برای موارد بدون Header) جایگزین می‌شود.

## تغییرات Infra (Keycloak + Docker Compose)

### Realm `sepahan`

- **Redirect URI اشتباه Client موبایل:** یک ورودی نادرست/زائد (`8081/*`) به مقدار درست اپ موبایل (`8082/*`، مطابق ADR-0017) اصلاح شد.
- **Password Policy تقویت‌شده:** از یک سیاست حداقلی به `length(10) and notUsername() and passwordHistory(3) and upperCase(1) and lowerCase(1) and digits(1) and specialChars(1)` تغییر کرد.

هر دو تغییر هم در فایل Seed کامیت‌شده (`infra/keycloak/import/sepahan-realm.json` — تا یک Import تازه هم نتیجه‌ی یکسان بدهد) و هم زنده روی نمونه‌ی در حال اجرا (از طریق Keycloak Admin REST API — `PUT /admin/realms/sepahan/clients/{id}` و `PUT /admin/realms/sepahan`) اعمال و تأیید شد.

### پورت‌های Docker Compose

`postgres`, `redis`, `prometheus`, `loki` از `"host:port"` به `"127.0.0.1:host:port"` تغییر کردند — یعنی این سرویس‌ها دیگر از ماشین‌های دیگر همان شبکه قابل‌دسترس نیستند (ریسک واقعی روی هر محیطی که این Compose روی یک Host با شبکه‌ی به‌اشتراک‌گذاشته‌شده اجرا شود). این تغییر هیچ اثری روی ارتباط بین‌Containeری ندارد (که همیشه از شبکه‌ی داخلی Docker با نام سرویس عبور می‌کند، مستقل از Publish شدن پورت روی Host) و هیچ اثری روی دسترسی از خودِ همان Host از طریق `localhost` ندارد.

**چرا `keycloak` و `grafana` عمداً کنار گذاشته شدند:** هر دو پشت صفحه‌ی ورود واقعی (Login) هستند — محدودسازی شبکه‌ای برایشان یک لایه‌ی دفاعی اضافه است، نه بستن یک شکاف واقعی مثل Postgres/Redis (بدون احراز هویت شبکه‌ای، فقط رمز) یا Prometheus/Loki (قبل از این فاز عملاً بدون هیچ احراز هویتی). این دو Service همچنان روی `0.0.0.0` باقی ماندند تا در محیط توسعه از یک ماشین دیگر همان شبکه هم (مثلاً برای دمو) قابل‌دسترس بمانند؛ اگر کارفرما محدودیت کامل‌تر بخواهد، می‌تواند این هم به همین الگو اضافه شود.

تأیید زنده: با کل Stack پایین آورده و دوباره بالا آمده (چون `docker-compose` نصب‌شده v1.29.2 است — طبق نکته‌ی شناخته‌شده‌ی `infra/README.md`، `docker rm -f` قبل از `up` لازم بود)، هر ۴ سرویس `Up (healthy)` و به‌درستی فقط روی `127.0.0.1` Bind شدند، و Scrape واقعی Prometheus از Backend (از طریق شبکه‌ی Docker) بدون تغییر همچنان کار می‌کرد.

## دفاع دومیه در Admin Panel (Server Action)

`admin-panel/src/lib/backend.ts` — `backendFetch`/`backendUpload` هر دو یک بررسی مستقل نقش اضافه کردند: `if (!session.roles?.includes("admin")) throw new BackendError(403, ...)`، بلافاصله بعد از بررسی موجود `accessToken`.

**چرا این تکرار به‌نظر لازم بود، نه صرفاً اضافی:** در معماری BFF این پروژه (ADR-0016)، `proxy.ts`/`layout.tsx` فقط ناوبری صفحه را کنترل می‌کنند — اما یک Server Action مسیر فراخوانی جداگانه‌ی خودش را دارد (یک Endpoint POST مجزا که Next.js می‌سازد) که می‌تواند ساختاری آن بررسی سطح Layout را دور بزند (مثلاً یک Client مخرب که مستقیم همان Endpoint داخلی Server Action را صدا بزند، بدون عبور از رندر صفحه). این خط دومین و مستقل‌ترین لایه‌ی دفاع است — حتی اگر Backend هم یک `@PreAuthorize` را جایی جا بیندازد.

**تأیید زنده در مرورگر (نه فقط Type-check):** یک کاربر آزمایشی موقت Keycloak (`phase19-adminverify`، نقش `admin`) ساخته شد؛ ورود واقعی از طریق صفحه‌ی Sign-in واقعی Admin Panel → Redirect به Keycloak → ورود موفق → بازگشت به Dashboard با داده‌ی واقعی (کاربران/هواداران، Notification ثبت‌شده و...) و صفحه‌ی `/users` با جدول واقعی (بدون هیچ ۴۰۳ غیرمنتظره) تأیید شد — یعنی بررسی جدید نقش، کارکرد واقعی Admin را خراب نکرده. کاربر آزمایشی بعد از تأیید حذف و صفر بودن باقیمانده تأیید شد (طبق روال ثابت این پروژه برای داده‌ی تست).

## چه چیزی بررسی و سالم تشخیص داده شد

- الگوی IDOR در همه‌جای دیگر پروژه (`findByIdAndUserId` و مشابه) از قبل درست و ثابت بود — فقط همان یک مورد Device Token از قلم افتاده بود.
- تزریق SQL: همه‌جا از Spring Data JPA/Query Method یا `@Param` پارامتری‌شده استفاده شده؛ هیچ Concatenation دستی رشته در یک Query پیدا نشد.
- رمزهای عبور/Secretها: هیچ مقدار واقعی در کد یا فایل کامیت‌شده پیدا نشد؛ همه از Env Var یا `.env` (خارج از Git) می‌آیند.
- CORS: پیکربندی محدود به Originهای صراحتاً تعریف‌شده، بدون Wildcard، از قبل درست بود (ADR-0017).
- RBAC روی Controllerها: پوشش `@PreAuthorize` هماهنگ با `docs/authentication/rbac-matrix.md` بود؛ Phase 18 هم این را از طریق یک درخواست HTTP واقعی (نه فقط بازبینی کد) اثبات کرده بود.
- Keycloak: جداسازی Database (Backend/Keycloak) و نقش Least-Privilege برای `sepahan_backend` (از Phase 6) از قبل درست بود.
- Dependencyهای Frontend (Admin Panel/Mobile): بدون یک ابزار CVE Scanning اختصاصی (خارج از دامنه‌ی این فاز)، بازبینی دستی `package.json` مورد مشکوک/منسوخ آشکاری نشان نداد — این یک تأیید Best-effort است، نه یک Scan خودکار کامل (به بخش «موکول‌شده‌ها» نگاه کنید).

## موکول‌شده‌ها (با دلیل مستند)

| مورد | چرا موکول شد |
|---|---|
| Scan خودکار CVE Dependencyها (`mvn dependency-check`, `npm audit` به‌طور ساختاریافته در CI) | ابزار/Dependency جدید نیازمند تصمیم و تأیید صریح جداگانه؛ بازبینی دستی این فاز Best-effort بود، نه جایگزین کامل |
| رمز عبور Django (`LOYALTY_DJANGO_SERVICE_TOKEN`) به‌صورت یک Secret مشترک ساده (نه چرخشی/کوتاه‌عمر) | طراحی شناخته‌شده و از قبل مستندشده (ADR-0014) — تغییر آن یک تصمیم معماری جدا با تأثیر روی هر دو سیستم است، نه یک نقص تازه‌کشف‌شده |
| تشخیص Content-Type واقعی رسانه‌ی News (فعلاً فقط بر اساس پسوند/Header ارسالی Client) | ریسک پایین (رسانه فقط توسط Admin آپلود می‌شود، نه کاربر عمومی — ADR-0013)؛ سخت‌گیری بیشتر (مثلاً Magic Byte Sniffing) دامنه‌ی جدا و بزرگ‌تری دارد |
| پیش‌فرض HTTP ساده (نه HTTPS) برای ارتباط با Django | موضوع صریح Phase 20 (Deployment/TLS)، نه یک نقص قابل‌رفع در سطح کد این فاز |
| طول Session آفلاین/Idle-Timeout پیش‌فرض Keycloak | یک تصمیم Product/UX (چقدر طولانی کاربر باید بدون ورود مجدد بماند) بیشتر از یک نقص امنیتی مشخص؛ نیازمند ورودی کارفرما، نه یک تصمیم یک‌طرفه‌ی امنیتی |
| نقش‌های عملیاتی درِ ورودی (Gate Operator) و دامنه‌ی دقیق دسترسی آن‌ها | خارج از یافته‌های مشخص این فاز؛ طراحی نقش‌ها از قبل در `rbac-matrix.md` مستند است و تغییر آن نیازمند بررسی Product جداست |

## Alternatives Considered

| گزینه | چرا رد شد |
|---|---|
| `permitAll()` کامل برای `/actuator/prometheus` باقی بماند، فقط مستندسازی ریسک | همان TODO امنیتی صریح Phase 17 که این فاز قرار بود ببندد؛ یک مسیر بدون هیچ احراز هویتی روی اینترنت باز می‌ماند اگر روزی این Backend مستقیم Expose شود |
| بستن کامل `/actuator/prometheus` با Bearer Token (به‌جای IP-based) | Prometheus (طبق طراحی ADR-0019) قابلیت ارسال Header سفارشی در Scrape Config دارد، ولی این یک Secret دیگر برای مدیریت اضافه می‌کرد؛ محدودیت شبکه‌ای هم‌راستا با این‌که این مسیر اساساً یک ابزار Infra داخلی است، نه یک API کاربری |
| حذف کامل Endpoint لغو Device Token به‌جای رفع IDOR | این قابلیت (کاربر بتواند اعلان دستگاه خودش را غیرفعال کند) واقعی و لازم است؛ مشکل فقط عدم بررسی مالکیت بود، نه خودِ قابلیت |
| بررسی نقش Admin فقط در `proxy.ts`/`layout.tsx` (سطح صفحه)، بدون تکرار در `backend.ts` | دقیقاً همان شکاف Server Action که این فاز کشف و بست؛ به‌تنهایی کافی نبود |
| محدودسازی شبکه‌ای Keycloak/Grafana هم مثل چهار سرویس دیگر | هر دو پشت Login واقعی هستند؛ ریسک نسبی پایین‌تر در برابر از‌دست‌دادن دسترسی دمو از ماشین‌های دیگر شبکه در محیط توسعه — قابل بازنگری اگر کارفرما بخواهد |

## Consequences

**مثبت:** یک IDOR واقعی (نه فرضی) با تست بسته شد؛ یک TODO امنیتی صریح دو فاز قبل (Prometheus) با یک راه‌حل قابل‌تست (نه فقط مستندسازی) بسته شد؛ یک لایه‌ی دفاع مستقل به نقطه‌ی واقعاً آسیب‌پذیر معماری BFF (Server Action) اضافه شد؛ Realm Keycloak هم در فایل هم به‌صورت زنده سخت‌تر شد؛ سطح حمله‌ی شبکه‌ای Docker Compose برای سرویس‌های بدون Login کاهش یافت؛ همه‌ی رفع‌ها هم با تست خودکار (۷۰/۷۰ سبز، شامل ۱۶ تست تازه/تغییریافته) و هم با تأیید دستی زنده (مرورگر واقعی + Exploit واقعی رد شد) اثبات شدند، نه فقط استدلال نظری.

**ریسک/نیازمند توجه:** بررسی IP در `MonitoringNetworkAuthorizationManager` بدون تنظیم جدید برای `X-Forwarded-For` است — اگر Phase 20 یک Reverse Proxy اضافه کند و این تنظیم به‌روز نشود، همه‌ی درخواست‌ها از دید این بررسی از یک IP داخلی (خودِ Proxy) می‌آیند و عملاً این محدودیت بی‌اثر می‌شود (نه ناامن‌تر از وضعیت قبلی، ولی بی‌فایده). Scan خودکار Dependency (CVE) هنوز جایی در CI نیست — یافته‌های این فاز درباره‌ی Dependencyها صرفاً دستی و Best-effort بودند. طراحی Secret مشترک Django همچنان به قوت خود باقی است (پذیرفته‌شده از ADR-0014، نه یک ریسک تازه).
