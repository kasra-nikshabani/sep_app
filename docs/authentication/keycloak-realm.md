# Keycloak Realm — `sepahan`

مرجع تصمیم‌های سطح بالا: [ADR-0003](../adr/0003-keycloak-central-sso-fan-id.md). این سند پیاده‌سازی واقعی (Phase 4) را توضیح می‌دهد.

## فایل Import

`infra/keycloak/import/sepahan-realm.json` — با بالا آمدن Compose (که در Phase 3 پیکربندی شد) این فایل به‌صورت خودکار در مسیر `/opt/keycloak/data/import` داخل Container قرار می‌گیرد و Keycloak با فلگ `--import-realm` آن را Import می‌کند.

> **تأیید شده با یک جریان کامل End-to-end واقعی (Phase 6).** یک کاربر تست ساخته شد، از `mobile-app` توکن واقعی گرفته شد، Claimهای `national_code`/`phone_number`/`realm_access.roles` در توکن بررسی شدند، و همان توکن مستقیماً به Endpoint واقعی Backend (`GET /api/v1/users/me`) زده شد — نه فقط بررسی دیتابیس. جزئیات باگ‌هایی که در این مسیر پیدا و رفع شدند، پایین همین سند — دو مورد از آن‌ها (شماره‌های ۵ و ۶) واقعاً جدی بودند و بدون تست End-to-end واقعی هرگز کشف نمی‌شدند.

## پروفایل کاربر: داخل همین فایل Import؛ `fix-user-profile.sh` فقط برای Realm از قبل موجود

پروفایل کاربر (Declarative User Profile) -- کد ملی به‌عنوان نام کاربری ([ADR-0024](../adr/0024-national-code-as-fan-id-username.md))، شماره تلفن، و اعتبارسنجی‌های ثبت‌نام -- به‌صورت Component `declarative-user-profile` داخل `sepahan-realm.json` است و `--import-realm` آن را همراه بقیه‌ی Realm می‌سازد. با یک Import تازه‌ی واقعی روی Keycloak 26.7.1 تأیید شد: پروفایل Import‌شده دقیقاً همان پروفایل فایل بود و نام کاربری غیر ۱۰ رقمی رد شد.

Import، Realmی را که از قبل وجود دارد بازنویسی نمی‌کند. برای یکی کردن پروفایل یک Realm قدیمی (مثلاً Volume محلی قدیمی) با همین فایل:

```bash
cd infra
./keycloak/fix-user-profile.sh
```

این اسکریپت همان پروفایل داخل `sepahan-realm.json` را اعمال می‌کند (تنها منبع) و اجرای دوباره‌اش بی‌اثر است. ⚠️ روی Realmی که کاربر قدیمی دارد، اول نام کاربری هر کاربر را به کد ملی‌اش تغییر دهید ([ADR-0024](../adr/0024-national-code-as-fan-id-username.md)).

## چه چیزی در این Realm ساخته می‌شود

### نقش‌ها (Realm Roles)

`fan` (پیش‌فرض)، `vip`، `admin`، `gate-operator`، `partner` — سطح اول طبق ADR-0003؛ نگاشت دقیق‌تر Permission به هر نقش موضوع Phase 5 است.

### Clientها

| Client ID | نوع | Flow | یادداشت |
|---|---|---|---|
| `mobile-app` | Public | Authorization Code + PKCE (`S256`) | بدون Secret؛ `redirectUris` فعلاً Placeholder (`sepahan://auth/callback`, `http://localhost:8081/*`) — **باید در Phase 15 با Deep Link واقعی اپ React Native جایگزین شود** |
| `admin-panel` | Confidential | Authorization Code استاندارد | `redirectUris` فرض بر NextAuth-style callback (`/api/auth/callback/*`) روی پورت ۳۰۰۰ — **باید در Phase 14 با آدرس واقعی Deploy تطبیق داده شود** |
| `backend-service` | Confidential، Service Account فعال | — | برای اعتبارسنجی توکن (Resource Server) در Spring Boot؛ Service Account فقط اگر در آینده فراخوانی سرویس‌به‌سرویس لازم شد استفاده می‌شود |
| `django-ticketing` | Confidential | Authorization Code + PKCE (`S256`) | دکمه‌ی «ورود با Fan ID» سایت بلیط ([ADR-0023](../adr/0023-fanid-account-linking-ticket-site.md))؛ `redirectUris` فقط `/accounts/fanid/callback/` (`localhost:8000` و `ticket.sepahansc.com`)؛ Secret فقط در `.env` سرور سایت (`FANID_CLIENT_SECRET`) |

### Client Scope اختصاصی: `fan-identity`

دو Protocol Mapper که Claimهای `national_code` و `phone_number` را داخل ID Token/Access Token/UserInfo قرار می‌دهند — دقیقاً همان مکانیزمی که ADR-0003 برای نگاشت به رکورد کاربر موجود در Django پیشنهاد داده بود. `national_code` از **نام کاربری** خوانده می‌شود (نام کاربری همان کد ملی ۱۰ رقمی است، [ADR-0024](../adr/0024-national-code-as-fan-id-username.md))؛ `phone_number` از User Attribute هم‌نام.

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
5. **حضور کلید `clientScopes` در JSON، ساخت خودکار Scopeهای استاندارد Keycloak را کلاً غیرفعال می‌کرد.** وقتی Realm JSON خودش یک آرایه‌ی `clientScopes` می‌دهد (حتی اگر فقط شامل یک Scope سفارشی مثل `fan-identity` باشد)، Keycloak فرض می‌کند این آرایه کامل و نهایی است و دیگر Scopeهای درونی خودش (`profile`, `roles`, `web-origins`, `acr`, `basic`, `email`, ...) را نمی‌سازد. نتیجه: توکن صادرشده حتی فاقد `sub` و `realm_access` بود. **کشف شد با گرفتن یک توکن واقعی و نگاه به Claimهای آن — نه با بررسی Import Log یا دیتابیس.** رفع: همه‌ی Scopeهای استاندارد از یک Instance سالم Export و همراه با `fan-identity` در `clientScopes` نهایی قرار گرفتند.
6. **Declarative User Profile به‌صورت پیش‌فرض یک Allowlist سخت‌گیرانه است.** تا وقتی `national_code`/`phone_number` صریحاً در Schema این Profile تعریف نشوند، Keycloak این Attributeها را هنگام ساخت کاربر **بی‌صدا نادیده می‌گیرد** (بدون خطا، فقط ذخیره نمی‌شوند) — یعنی Protocol Mapperهای `fan-identity` همیشه مقدار خالی برمی‌گرداندند چون خودِ Attribute هرگز در دیتابیس نبود. علاوه بر آن، همان Profile پیش‌فرض `email`/`firstName`/`lastName` را Required می‌کند که باعث خطای «Account is not fully set up» (VERIFY_PROFILE) در ورود می‌شد. **این تنظیم بخشی از RealmRepresentation قابل Import نیست** (به‌صورت Component داخلی جدا ذخیره می‌شود)، پس نمی‌توانست مثل بقیه در `sepahan-realm.json` باشد — رفع آن در اسکریپت جدید `infra/keycloak/fix-user-profile.sh` (که باید بعد از هر Import تازه یک‌بار اجرا شود) قرار گرفت. **به‌روزرسانی:** این برداشت درست نبود -- پروفایل به‌صورت Component (`declarative-user-profile`) در فایل Import می‌نشیند و Import می‌شود؛ از Commit `ba3525a` داخل `sepahan-realm.json` است و اسکریپت فقط برای Realm از قبل موجود لازم است (بالا).

## ⚠️ نکته‌ی مهم درباره‌ی رمز Admin

`KEYCLOAK_ADMIN_PASSWORD` فقط **یک‌بار**، در همان اولین Boot که Master Realm ساخته می‌شود، به‌عنوان رمز کاربر Admin موقت تنظیم می‌شود. اگر بعداً مقدار آن را در `.env` عوض کنید ولی Volume دیتابیس (`postgres_data`) پاک نشود، رمز واقعی همچنان همان مقدار اولیه می‌ماند — تغییر `.env` به‌تنهایی رمز را عوض نمی‌کند. برای عوض کردن واقعی رمز Admin: یا از خود Admin Console (بعد از ورود با رمز فعلی) عوض کنید، یا کامل Volume را با `docker-compose down -v` پاک و از نو Import کنید (در محیط Dev بی‌خطر است، چون داده‌ی واقعی هنوز وجود ندارد).

**یادآوری امنیتی:** اگر رمز Admin را در یک پیام/ترمینال به‌اشتراک‌گذاشته‌شده Paste کرده‌اید (مثلاً برای دیباگ)، آن را تغییردهنده کنید — به هر مقداری که در یک مکالمه تایپ شده، به‌عنوان لو‌رفته نگاه کنید.

## چک‌لیست تأیید دستی (اختیاری، تکمیلی)

جریان اصلی (Import → fix-user-profile.sh → ساخت کاربر → توکن → `/api/v1/users/me`) در Phase 6 به‌طور کامل و واقعی تست شد. اگر خواستید از طریق Admin Console هم چشمی بررسی کنید:

۱. باز کردن `http://localhost:8080` → ورود با `KEYCLOAK_ADMIN` / رمز فعلی (طبق نکته‌ی بالا)
۲. Realm sepahan → **Clients** → برای `admin-panel`/`backend-service`/`django-ticketing`: تب **Credentials** → کپی Secret تولیدشده (لازم برای Phase 14 — این را فقط در `.env` سرویس مربوطه بگذارید، نه در پیام یا Git)
۳. **Users** → **Add user** → یک کاربر تست بسازید (نام کاربری = یک کد ملی ۱۰ رقمی)، مقدار `phone_number` را پر کنید، در تب **Credentials** رمز بگذارید (`temporary: off`) — چون `mobile-app` به‌صورت پیش‌فرض `directAccessGrantsEnabled: false` دارد (طبق ADR-0003، فقط PKCE)، برای تست دستی با Postman/curl باید موقتاً این فلگ را روی `mobile-app` فعال کنید و بعد از تست خاموش کنید؛ تست واقعی جریان Authorization Code کامل نیاز به یک صفحه‌ی Callback واقعی دارد که در Phase 14/15 ساخته می‌شود.
