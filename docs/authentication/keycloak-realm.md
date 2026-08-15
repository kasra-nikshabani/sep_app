# Keycloak Realm — `sepahan`

مرجع تصمیم‌های سطح بالا: [ADR-0003](../adr/0003-keycloak-central-sso-fan-id.md). این سند پیاده‌سازی واقعی (Phase 4) را توضیح می‌دهد.

## فایل Import

`infra/keycloak/import/sepahan-realm.json` — با بالا آمدن Compose (که در Phase 3 پیکربندی شد) این فایل به‌صورت خودکار در مسیر `/opt/keycloak/data/import` داخل Container قرار می‌گیرد و Keycloak در `start-dev` آن را Import می‌کند.

> **مهم — اعتبارسنجی Runtime این فایل هنوز انجام نشده.** به دلیل عدم دسترسی من به Docker Daemon روی این ماشین (مستند در گزارش Phase 3)، فقط از نظر Syntax (`python -m json.load`) بررسی شده، نه با یک Keycloak واقعی. لطفاً پس از `docker-compose up` طبق چک‌لیست پایین همین سند تأیید کنید.

## چه چیزی در این Realm ساخته می‌شود

### نقش‌ها (Realm Roles)

`fan` (پیش‌فرض)، `vip`، `admin`، `gate-operator`، `partner` — سطح اول طبق ADR-0003؛ نگاشت دقیق‌تر Permission به هر نقش موضوع Phase 5 است.

### Clientها

| Client ID | نوع | Flow | یادداشت |
|---|---|---|---|
| `mobile-app` | Public | Authorization Code + PKCE (`S256`) | بدون Secret؛ `redirectUris` فعلاً Placeholder (`sepahan://auth/callback`, `http://localhost:8081/*`) — **باید در Phase 15 با Deep Link واقعی اپ React Native جایگزین شود** |
| `admin-panel` | Confidential | Authorization Code استاندارد | `redirectUris` فرض بر NextAuth-style callback (`/api/auth/callback/*`) روی پورت ۳۰۰۰ — **باید در Phase 14 با آدرس واقعی Deploy تطبیق داده شود** |
| `backend-service` | Confidential، Service Account فعال | — | برای اعتبارسنجی توکن (Resource Server) در Spring Boot؛ Service Account فقط اگر در آینده فراخوانی سرویس‌به‌سرویس لازم شد استفاده می‌شود |
| `django-ticketing` | Confidential، **Bearer-only** | — | طبق ADR-0004: Django فقط توکن دریافتی را اعتبارسنجی می‌کند، جریان OIDC کامل اجرا نمی‌کند |

### Client Scope اختصاصی: `fan-identity`

دو Protocol Mapper که مقادیر `national_code` و `phone_number` را (از User Attributes) به‌صورت Claim داخل ID Token/Access Token/UserInfo قرار می‌دهند — دقیقاً همان مکانیزمی که ADR-0003 برای نگاشت به رکورد کاربر موجود در Django پیشنهاد داده بود. این دو مقدار باید هنگام ساخت کاربر در Keycloak (Phase 5) به‌عنوان User Attribute پر شوند.

### تنظیمات امنیتی/نشست

- `bruteForceProtected: true`، ۵ تلاش ناموفق قبل از قفل موقت.
- `passwordPolicy`: حداقل ۱۰ کاراکتر، متفاوت از Username، ۳ رمز اخیر ممنوع (فقط برای کاربرانی که رمز عبور دارند — VIP/Admin؛ ورود Fan عادی طبق طرح Django می‌تواند OTP‌محور بماند، جزئیات دقیق در Phase 5).
- `accessTokenLifespan: 300s`، `ssoSessionIdleTimeout: 1800s`، `ssoSessionMaxLifespan: 36000s` (۱۰ ساعت)، `Refresh Token Rotation` فعال (`revokeRefreshToken: true`).
- **این مقادیر پیش‌فرض معقول‌اند، نه نهایی** — طبق ADR-0003 تنظیم دقیق TTL باید در Phase 19 (Security Audit) بازبینی شود.
- زبان پیش‌فرض فارسی (`fa`)، پشتیبانی از `en` هم فعال — چون صفحه‌ی Login میزبانی‌شده‌ی Keycloak مستقیماً توسط کاربر نهایی دیده می‌شود.

## چیزی که عمداً در این فایل نیست

- **هیچ User یا Client Secret‌ای در این فایل نیست.** برای Clientهای Confidential (`admin-panel`, `backend-service`, `django-ticketing`) چون فیلد `secret` مشخص نشده، Keycloak هنگام Import یک مقدار تصادفی خودش تولید می‌کند — این مقدار را باید بعد از بالا آمدن از Admin Console بخوانید (زیر) و در `.env` سرویس مربوطه بگذارید، هرگز در Git.
- ساخت کاربر تستی هم عمداً در این فایل نیست (چون یعنی رمز عبور در Git) — چک‌لیست پایین نحوه‌ی ساخت دستی یک کاربر تست را توضیح می‌دهد.

## چک‌لیست تأیید (لطفاً شما اجرا و نتیجه را گزارش کنید)

۱. `cd infra && docker-compose --env-file .env up -d` (طبق Phase 3)
۲. باز کردن `http://localhost:8080` → ورود با `KEYCLOAK_ADMIN`/`KEYCLOAK_ADMIN_PASSWORD` که در `.env` گذاشتید
۳. بررسی این‌که Realm به‌نام **sepahan** در لیست Realmها وجود دارد (یعنی Import موفق بوده)
۴. Realm sepahan → **Clients** → بررسی چهار Client بالا وجود دارند
۵. برای `admin-panel` و `backend-service` و `django-ticketing`: تب **Credentials** → کپی مقدار Secret تولیدشده (لازم برای Phase 6/14)
۶. Realm sepahan → **Realm roles** → بررسی ۵ نقش وجود دارند
۷. (اختیاری، برای تست دستی) **Users** → **Add user** → یک کاربر تست بسازید، در تب **Attributes** مقدار `national_code` و `phone_number` را دستی اضافه کنید، در تب **Credentials** یک رمز موقت بگذارید → سپس با Client `admin-panel` می‌توانید جریان Authorization Code را با مرورگر تست کنید (نیاز به یک صفحه‌ی Callback واقعی دارد که هنوز نساخته‌ایم — یک تست کامل End-to-end واقعی‌تر در Phase 14 ممکن می‌شود)

اگر هرکدام از این مراحل با خطا مواجه شد، خروجی/Screenshot را برایم بفرستید تا `sepahan-realm.json` را اصلاح کنم.
