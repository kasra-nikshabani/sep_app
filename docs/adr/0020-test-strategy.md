# ADR-0020: استراتژی تست — Testcontainers، پوشش HTTP/RBAC، CI

- **وضعیت:** پذیرفته‌شده (Phase 18)
- **تصمیم‌گیرندگان:** Architect + کارفرما (چهار تصمیم صریح از طریق AskUserQuestion)

## Context

از Phase 8 به بعد، هر ADR جدیدی که به تست هم‌زمانی واقعی نیاز داشت («انتخاب نهایی استراتژی تست خودکار کل پروژه موضوع Phase 18 است» -- تکرارشده در ReservationServiceTest، ADR-0004، ADR-0008) این تصمیم را عمداً به همین فاز موکول می‌کرد. تا این فاز:
- همه‌ی تست‌های `@SpringBootTest` مستقیم به Postgres/Redis واقعی روی پورت‌های ثابت `infra/docker-compose.yml` وصل می‌شدند -- نیازمند روشن‌بودن کل Stack زیرساخت و Export دستی سه Env Var (`BACKEND_DB_PASSWORD`, `INFRA_REDIS_PASSWORD`, `LOYALTY_DJANGO_SERVICE_TOKEN`) قبل از هر اجرا. این اصطکاک بارها در همین نشست (Phase 16/17) واقعاً وقت گرفت.
- هیچ تستی از طریق یک درخواست HTTP واقعی اجرا نمی‌شد -- یعنی زنجیره‌ی کامل Spring Security (JWT، `KeycloakRoleConverter`، `@PreAuthorize`/RBAC، CORS، مسیرهای permitAll) هرگز به‌طور خودکار تست نشده بود؛ فقط با متد مستقیم Service.
- هیچ CI وجود نداشت؛ این پروژه اصلاً یک Remote Git (GitHub/GitLab) ندارد.
- تست خودکار Frontend (Admin Panel/Mobile) صفر بود -- همه‌چیز دستی با مرورگر تأیید شده (طبق روال ثابت این پروژه).

با AskUserQuestion، چهار تصمیم صریح از کارفرما گرفته شد: **Testcontainers**، **بله به پوشش Controller/RBAC**، **نوشتن CI همین الان**، **فقط Backend** (بدون زیرساخت تست Frontend).

## Decision 1: Testcontainers به‌جای Stack مشترک

`org.springframework.boot:spring-boot-testcontainers` + `org.testcontainers:{junit-jupiter,postgresql}` اضافه شد (نسخه از BOM خودِ `spring-boot-starter-parent`، بدون Pin دستی). یک `TestcontainersConfig` مشترک (`@TestConfiguration` + دو Bean با `@ServiceConnection`) به هر ۶ کلاس `@SpringBootTest` موجود اضافه شد (`@Import(TestcontainersConfig.class)` + `@ActiveProfiles("test")`). یک فایل جدید `src/test/resources/application-test.yml` فقط یک مقدار بی‌خطر برای `sepahan.loyalty.django.service-token` می‌دهد (تنها Propertyی که در `application.yml` اصلی بدون Default اجباری است و در تست واقعاً استفاده نمی‌شود؛ Django اصلاً در تست بالا نیست، Polling با خطای اتصال بی‌اثر شکست می‌خورد -- دقیقاً طراحی Self-healing موجود ADR-0014).

`@ServiceConnection` خودکار `spring.datasource.*`/`spring.data.redis.*` را با جزئیات Containerها جایگزین می‌کند -- نیازی به `@DynamicPropertySource` دستی یا خواندن `BACKEND_DB_PASSWORD`/`INFRA_REDIS_PASSWORD` در تست نیست.

**تأیید زنده (نه فقط منطقی):** کل Suite (به‌جز ZibalPaymentProviderTest که به Sandbox خارجی وصل می‌شود) با Postgres/Redis مشترک کاملاً **متوقف** و بدون هیچ Env Var صادرشده، سبز اجرا شد -- استقلال کامل از `infra/` اثبات شد، نه فقط ادعا.

### اثر جانبی روی تست هم‌زمانی موجود Loyalty

تست ۲۰-Threadی `concurrentRedemptions_onLastUnitOfStock_onlyOneSucceeds` (از Phase 12) بازطراحی شد: بذر امتیاز هر کاربر (که خودش JIT حساب را با REQUIRES_NEW واقعی -- ADR-0018 -- فعال می‌کرد) از *قبل* خط شروع هم‌زمانی منتقل شد. آن‌چه این تست واقعاً می‌سنجد رقابت روی موجودی جایزه است، نه ساخت هم‌زمان حساب؛ این جداسازی هیچ ربطی به Testcontainers ندارد و صرفاً یک بهبود ساختاری تست بود که حین این بررسی مشخص شد.

## Decision 2: پوشش واقعی HTTP/RBAC (شکاف صفر قبلی)

`SecurityRbacIntegrationTest` جدید -- `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` روی یک Port واقعی، نه صدازدن مستقیم متد. پوشش: `/actuator/health`/`/actuator/prometheus` بدون Token (permitAll)، `/api/v1/users/me` بدون Token (۴۰۱)، با Token `fan` (۲۰۰، JIT Provisioning واقعی هم از طریق HTTP)، `/api/v1/users/admin` با `fan` (۴۰۳) و با `admin` (۲۰۰)، یک ۴۰۴ واقعی پشت Token معتبر (رگرسیون مستقیم نقص Phase 14 -- بدون permitAll روی `/error`، این ۴۰۴ به یک ۴۰۱ گمراه‌کننده تبدیل می‌شد)، و CORS Preflight (مجاز از Origin پیکربندی‌شده، رد از Origin ناشناس).

### چگونه JWT واقعی ساخته شد بدون یک Keycloak واقعی

یک `JwtDecoder` تست (`TestJwtDecoderConfig`) با یک کلید RSA محلی (تولیدشده فقط برای همان JVM تست، نه Keycloak) جایگزین شد -- `TestJwtSupport` توکن‌هایی دقیقاً هم‌شکل خروجی واقعی Keycloak امضا می‌کند (`realm_access.roles`). با این جایگزینی، تنها بخش تعویض‌شده «از کجا کلید عمومی می‌آید» است؛ اعتبارسنجی امضا/انقضا و `KeycloakRoleConverter` (نگاشت نقش) دقیقاً همان کد واقعی Production اجرا می‌شوند. جایگزین‌های رد‌شده در بخش Alternatives پایین توضیح داده شده‌اند.

### کشف واقعی حین همین کار: `@AutoConfigureObservability`

`/actuator/prometheus` زیر `@SpringBootTest` همیشه ۴۰۴ برمی‌گرداند -- حتی با `management.endpoints.web.exposure.include` درست تنظیم‌شده و همان Dependency که در Phase 17 زنده کار می‌کرد. ریشه: در Spring Boot 3.x، `ObservabilityContextCustomizerFactory` (در `spring-boot-test-autoconfigure`) به‌طور پیش‌فرض **تمام** Exporterهای Metrics/Observability را زیر هر `@SpringBootTest` غیرفعال می‌کند -- مستند مستقیم و آسان‌یاب نبود؛ با بررسی محتوای واقعی جار (`spring.factories`) و آزمایش زنده (نه حدس) کشف و با `@AutoConfigureObservability` رفع شد.

## Decision 3: نوشتن CI همین الان (`.github/workflows/ci.yml`)

این پروژه هیچ Remote Git ندارد -- این Workflow **تا Push شدن به یک Repository واقعی روی GitHub غیرفعال می‌ماند**؛ نمی‌شد آن را زنده روی یک Runner واقعی GitHub Actions تأیید کرد. آن‌چه واقعاً تأیید شد: دقیقاً همان دستورهایی که هر Job اجرا می‌کند (`./mvnw -B test -Dtest='!ZibalPaymentProviderTest'`، `npm run typecheck` در admin-panel و mobile) به‌صورت محلی، جدا از فایل Workflow، دستی اجرا و سبز تأیید شدند -- تا حداکثر اطمینان ممکن بدون یک Runner واقعی.

سه Job: `backend-test` (روی `ubuntu-latest`، Docker از قبل نصب -- Testcontainers نیازی به تنظیم اضافه ندارد)، `admin-panel-typecheck`، `mobile-typecheck` (هر دو فقط `tsc --noEmit` -- بدون زیرساخت تست جدید Frontend، طبق Decision 4). یک Script `typecheck` جدید به هر دو `package.json` اضافه شد (فقط یک نام‌گذاری برای دستور از‌قبل‌موجود `tsc`، نه یک Dependency جدید).

`ZibalPaymentProviderTest` عمداً از CI کنار گذاشته شد -- طبق Javadoc خودِ همان فایل (از Phase 9)، این تست عمداً به Sandbox واقعی و خارجی Zibal وصل می‌شود و «اگر آن Sandbox در دسترس نبود، به‌طور طبیعی Fail می‌شود». بی‌ثباتی یک سرویس خارجی نباید کل CI را قرمز کند.

## Decision 4: بدون زیرساخت تست Frontend این فاز

Admin Panel/Mobile همچنان فقط با تست دستی مرورگر (طبق روال ثابت این پروژه) تأیید می‌شوند. تنها افزوده‌ی این فاز برای Frontend، `typecheck` (از‌قبل‌موجود، بدون Dependency جدید) در CI است. معرفی Vitest/Playwright/RNTL دامنه‌ی جدا و بزرگ‌تری دارد که کارفرما صریحاً به فاز دیگری موکول کرد.

## Alternatives Considered

| گزینه | چرا رد شد |
|---|---|
| ادامه‌ی وصل‌شدن به `infra/` مشترک برای تست | همان اصطکاک واقعی (Env Var دستی + وابستگی به Stack روشن) که این فاز حل کرد؛ روی CI هم اصلاً کار نمی‌کرد (بدون یک docker-compose زنده در Runner) |
| `dasniko/testcontainers-keycloak` (Keycloak واقعی موقت برای تست RBAC) | Dependency سنگین‌تر (Community، نه رسمی `org.testcontainers`) + Startup به‌مراتب کندتر از Postgres/Redis؛ کلید RSA محلی همان پوشش RBAC واقعی را بدون این هزینه می‌دهد |
| `spring-security-test` با `mockJwt()`/`@WithMockUser` | مستقیم SecurityContext را تزریق می‌کند -- دقیقاً همان مسیر واقعی اعتبارسنجی JWT/JwtDecoder را که این تست‌ها قرار است اثبات کنند، دور می‌زند |
| موکول‌کردن کامل CI به Phase 20 | چرخه‌ی بازخورد را برای بقیه‌ی این پروژه (فازهای ۱۹/۲۰) به تعویق می‌انداخت؛ نوشتن آن الان (حتی خاموش) هزینه‌ای نداشت |
| Vitest/Playwright برای Frontend همین فاز | دامنه‌ی بزرگ و جدا؛ کارفرما صریحاً رد کرد |

## Consequences

**مثبت:** برای اولین بار، `git clone` + `./mvnw test` به‌تنهایی (بدون هیچ Setup دیگر) کل Suite Backend را سبز اجرا می‌کند. شکاف امنیتی واقعی (RBAC/CORS هرگز خودکار تست نشده) بسته شد. CI آماده و محلی تأییدشده است، منتظر فقط یک Remote.

**ریسک/نیازمند توجه:** CI تا Push شدن به یک Remote واقعی هرگز به‌طور واقعی روی GitHub اجرا نشده -- ریسک کوچک ناسازگاری محیط Runner واقعی (نسخه‌ی Docker، منابع) که فقط بعد از اولین Push واقعی کاملاً روشن می‌شود. `ZibalPaymentProviderTest` هنوز در هیچ CI Gate ای نیست -- اگر رفتار Zibal روزی واقعاً بشکند (نه فقط Sandbox ناپایدار)، این تست به‌تنهایی هشدار نمی‌دهد.
