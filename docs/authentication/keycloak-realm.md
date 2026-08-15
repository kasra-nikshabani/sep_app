# Keycloak Realm — `sepahan`

مرجع تصمیم‌های سطح بالا: [ADR-0003](../adr/0003-keycloak-central-sso-fan-id.md). این سند پیاده‌سازی واقعی (Phase 4) را توضیح می‌دهد.

## فایل Import

`infra/keycloak/import/sepahan-realm.json` — با بالا آمدن Compose (که در Phase 3 پیکربندی شد) این فایل به‌صورت خودکار در مسیر `/opt/keycloak/data/import` داخل Container قرار می‌گیرد و Keycloak با فلگ `--import-realm` آن را Import می‌کند.

> **تأیید شده با اجرای واقعی.** بعد از حل مشکل دسترسی Docker (نیاز به Log out/Log in کامل، نه فقط ترمینال جدید)، این Stack واقعاً بالا آمد و Import مستقیماً از دیتابیس Keycloak تأیید شد: Realm `sepahan` فعال، هر ۴ Client با نوع درست (`mobile-app` Public، `django-ticketing` Bearer-only، ...)، ۵ نقش سفارشی، و Client Scope `fan-identity` با هر دو Protocol Mapper. جزئیات باگ‌هایی که در این مسیر پیدا و رفع شدند، پایین همین سند.

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

## باگ‌هایی که در تست واقعی پیدا و رفع شدند

این‌ها همه در Commit همین فاز اصلاح شده‌اند؛ برای شفافیت ثبت می‌شوند:

1. **فیلد جعلی `loginWithUserNameAllowed`** در JSON — چنین فیلدی اصلاً در `RealmRepresentation` کیکلوک ۲۶ وجود ندارد؛ Import را کامل Fail می‌کرد (`Unrecognized field`). حذف شد.
2. **فلگ `--import-realm` جا افتاده بود** در `docker-compose.yml` — بدون آن، Keycloak فایل‌های پوشه‌ی `import/` را کلاً نادیده می‌گیرد، حتی اگر Volume درست Mount شده باشد. اضافه شد.
3. **Image نسخه‌ی `26.0` از Registry حذف شده بود**، و نسخه‌ی جدیدتر (`26.7.1`) روی `quay.io` هم به‌طور مداوم ۴۰۳ Forbidden می‌داد (به نظر می‌رسد یک محدودیت شبکه‌ای برای `quay.io` وجود دارد) — سوییچ به همان Image رسمی از **Docker Hub** (`keycloak/keycloak:26.7.1`) که بدون مشکل Pull شد.
4. **Healthcheck نداشت** — چون Image فاقد `curl`/`wget` است، از ترفند `/dev/tcp` خود Bash روی مسیر مدیریتی Health (پورت ۹۰۰۰) استفاده شد.

## ⚠️ نکته‌ی مهم درباره‌ی رمز Admin

`KEYCLOAK_ADMIN_PASSWORD` فقط **یک‌بار**، در همان اولین Boot که Master Realm ساخته می‌شود، به‌عنوان رمز کاربر Admin موقت تنظیم می‌شود. اگر بعداً مقدار آن را در `.env` عوض کنید ولی Volume دیتابیس (`postgres_data`) پاک نشود، رمز واقعی همچنان همان مقدار اولیه می‌ماند — تغییر `.env` به‌تنهایی رمز را عوض نمی‌کند. برای عوض کردن واقعی رمز Admin: یا از خود Admin Console (بعد از ورود با رمز فعلی) عوض کنید، یا کامل Volume را با `docker-compose down -v` پاک و از نو Import کنید (در محیط Dev بی‌خطر است، چون داده‌ی واقعی هنوز وجود ندارد).

**یادآوری امنیتی:** اگر رمز Admin را در یک پیام/ترمینال به‌اشتراک‌گذاشته‌شده Paste کرده‌اید (مثلاً برای دیباگ)، آن را تغییردهنده کنید — به هر مقداری که در یک مکالمه تایپ شده، به‌عنوان لو‌رفته نگاه کنید.

## چک‌لیست تأیید دستی (اختیاری، تکمیلی)

موارد اصلی از طریق دیتابیس تأیید شده‌اند؛ اگر خواستید از طریق Admin Console هم چشمی بررسی کنید:

۱. باز کردن `http://localhost:8080` → ورود با `KEYCLOAK_ADMIN` / رمز فعلی (طبق نکته‌ی بالا)
۲. Realm sepahan → **Clients** → برای `admin-panel`/`backend-service`/`django-ticketing`: تب **Credentials** → کپی Secret تولیدشده (لازم برای Phase 6/14 — این را فقط در `.env` سرویس مربوطه بگذارید، نه در پیام یا Git)
۳. (اختیاری، برای تست End-to-end) **Users** → **Add user** → یک کاربر تست بسازید، در تب **Attributes** مقدار `national_code`/`phone_number` را اضافه کنید — تست کامل جریان Authorization Code نیاز به یک صفحه‌ی Callback واقعی دارد که در Phase 14 ساخته می‌شود.
