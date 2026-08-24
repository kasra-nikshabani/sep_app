# Backend — Spring Boot Modular Monolith

**وضعیت:** قابل‌اجرا (Phase 16). Spring Boot 3.5.16 / Java 21 / Maven (با Maven Wrapper) — [ADR-0009](../docs/adr/0009-spring-boot-baseline.md). ماژول‌های واقعاً پیاده‌شده تا این فاز: `auth`, `users`, `fan`, `ticketing`, `payments`, `shop`, `news`, `loyalty`, `notifications`, `partners` (بقیه‌ی جدول زیر هنوز فقط برنامه‌ریزی‌شده‌اند، در فازهای خودشان ساخته می‌شوند).

**نکته‌ی Phase 16:** Push واقعی با FCM مستقیم اضافه شد -- `FirebaseCloudMessagingPushProvider` (پشت `sepahan.notifications.push.provider=fcm`، پیش‌فرض همچنان `fake`) با کتابخانه‌ی رسمی `firebase-admin`. یک نقص واقعی هم رفع شد: بدون تشخیص کد خطای `UNREGISTERED` FCM (اپ حذف‌شده)، `NotificationService` دوباره و دوباره به همان Device Token مرده تلاش می‌کرد -- حالا آن Token خودکار غیرفعال می‌شود. **فعال‌سازی واقعی هنوز نیازمند یک Service Account از یک پروژه‌ی Firebase واقعی است** (`FCM_SERVICE_ACCOUNT_PATH`) -- هم‌الگوی Zibal/SMTP: پیاده‌سازی آماده، پیکربندی واقعی قبل از Production لازم.
**مهم‌تر، یک نقص عمیق‌تر و مستقل کشف و رفع شد:** `REQUIRES_NEW` در JIT Provisioning (`UserProvisioningService`/`LoyaltyAccountService`) از Phase 5 هرگز واقعاً اثر نداشت -- Self-Invocation (فراخوانی از داخل همان کلاس) طبق رفتار مستندشده‌ی اسپرینگ کاملاً از Proxy عبور می‌کند. با یک تست هم‌زمانی واقعی (`CountDownLatch`) کشف شد؛ رفع با انتقال آن متدها به دو Bean جدا (`JitInsertHelper`, `LoyaltyJitInsertHelper`). اثر جانبی: چون REQUIRES_NEW حالا واقعاً یک Connection دوم لازم دارد، `spring.datasource.hikari.maximum-pool-size` به ۳۰ افزایش یافت. جزئیات کامل در [ADR-0018](../docs/adr/0018-real-push-notifications.md).

**نکته‌ی Phase 15:** بدون Endpoint جدید -- فقط دو تغییر واقعی روی کد موجود. (۱) افزودن CORS محدود (فقط `/api/v1/**`، فقط Originهای صراحتاً پیکربندی‌شده) چون اپ موبایل (`mobile/`، برخلاف Admin Panel) مستقیم از مرورگر/Client به این API وصل می‌شود، نه از پشت یک لایه‌ی سروری میانی. (۲) رفع یک باگ واقعی Race Condition در JIT Provisioning: `UserProvisioningService` و `LoyaltyAccountService` از `save()` به `saveAndFlush()` تغییر کردند، چون `save()` ساده Insert را تا Commit تراکنش به تعویق می‌انداخت و `DataIntegrityViolationException` بیرون از try/catch پرتاب می‌شد -- اولین‌بار با درخواست‌های واقعاً هم‌زمان اپ موبایل در اولین ورود یک کاربر رخ داد. جزئیات کامل در [ADR-0017](../docs/adr/0017-mobile-app.md).

**نکته‌ی Phase 14:** فقط دو Endpoint Read-only کوچک جدید اضافه شد -- `GET /api/v1/users/admin` و `GET /api/v1/payments/admin` -- تا Admin Panel جدید (`admin-panel/`) بتواند فهرست کاربران/پرداخت‌ها را نشان دهد. کار اصلی این فاز در `admin-panel/` است؛ جزئیات در [ADR-0016](../docs/adr/0016-admin-panel.md).

**نکته‌ی مهم درباره‌ی Phase 13:** طبق بریف، این فاز رسماً «External Integrations» (Banking/Vehicle/Insurance/Travel/Entertainment) بود و Notifications فاز جداگانه‌ی ۱۶ است. بررسی واقعی نشان داد هیچ‌کدام از Providerهای آن چهار حوزه (طبق بند ۸ بریف صراحتاً «مورد بررسی») در جایی از این پروژه استفاده نشده‌اند و ساختن حتی یک Interface خالی برایشان بدون API واقعی، خودش حدس‌زدن API بود؛ کارفرما این تصمیم را تأیید کرد و صریحاً خواست محتوای Phase 16 (که Providerهای واقعی SMS.ir/SMTP را دارد) به جای آن به این فاز منتقل شود، به‌همراه اسکلت Partners. جزئیات کامل در [ADR-0015](../docs/adr/0015-notifications-and-partners.md).

اجرای کامل Phase 12 نیازمند یک متغیر محیطی اضافه است: `LOYALTY_DJANGO_SERVICE_TOKEN` (طبق `infra/.env.example`) -- راز مشترک بین این Backend و `ticket.sepahansc/football_tickets` برای Polling رویداد خرید بلیط فوتبال (ADR-0014). بدون آن، Backend بالا می‌آید ولی هر Poll با خطای احراز هویت از Django شکست می‌خورد (بدون اثر روی بقیه‌ی سیستم -- طبق طراحی Self-healing همان ADR).

## اجرا (محلی، در برابر Stack زنده‌ی `infra/`)

```bash
# ۱. Stack زیرساخت باید بالا باشد (طبق infra/README.md)
cd infra && docker-compose --env-file .env up -d && ./keycloak/fix-user-profile.sh

# ۲. اجرای Backend
cd ../backend
export $(grep -E '^BACKEND_DB_PASSWORD=' ../infra/.env)
export INFRA_REDIS_PASSWORD=$(grep -E '^REDIS_PASSWORD=' ../infra/.env | cut -d= -f2-)
./mvnw spring-boot:run
```

سرور روی `http://localhost:8081` بالا می‌آید. `GET /actuator/health` بدون Auth در دسترس است؛ بقیه‌ی `/api/**` نیازمند یک Bearer Token معتبر از Keycloak Realm `sepahan` هستند (`iss: http://localhost:8080/realms/sepahan`).

تست شده تا این نقطه (هم با تست خودکار، هم با درخواست HTTP واقعی):
- `GET /api/v1/users/me` — با اولین توکن معتبر یک کاربر، رکورد `users.app_user` + `fan.fan_profile` به‌صورت خودکار ساخته می‌شود (JIT، معادل [ADR-0008](../docs/adr/0008-new-user-jit-provisioning.md) برای Django) و در فراخوانی‌های بعدی همان رکورد برگردانده می‌شود — نه رکورد تکراری؛ چند فراخوانی *واقعاً* هم‌زمان روی همان کاربر تازه هم دقیقاً یک رکورد می‌سازند، بدون ۵۰۰ (تست هم‌زمانی با CountDownLatch، ADR-0018 -- رفع نقص Self-Invocation).
- `/api/v1/ticketing/**` — مرور رویداد/صندلی، رزرو، لغو؛ نقش `admin` برای ساخت سالن/رویداد اجباری است (۴۰۳ برای نقش `fan`)؛ رزرو هم‌زمان دو کاربر روی یک صندلی — دقیقاً یکی موفق می‌شود (ADR-0010، تست با ۲۵ Thread واقعاً هم‌زمان). صدور بلیط دیگر مسیر ساده‌شده نیست — فقط از طریق `payments` (زیر) اتفاق می‌افتد.
- `/api/v1/payments/**` — پرداخت واقعی (Zibal Sandbox، تأیید سرور-به-سرور، Idempotency) + `FakePaymentProvider` برای تست بدون شبکه؛ جریان کامل رزرو→پرداخت→Callback→صدور بلیط با هر دو Provider تست شده (ADR-0011).
- `/api/v1/shop/**` — کاتالوگ/سبد/Checkout/کد تخفیف/مرجوعی؛ کاهش اتمی موجودی زیر بار هم‌زمانی واقعی (۲۰ Thread روی آخرین واحد موجودی — دقیقاً یکی موفق می‌شود)، RBAC (`admin` برای کاتالوگ/گردش‌کار مرجوعی)، جریان کامل Checkout→پرداخت→`paid` از طریق همان رویداد Payment (ADR-0012).
- `/api/v1/news/**` — کاتالوگ/برچسب/خبر/رسانه؛ RBAC (Draft فقط برای admin قابل مشاهده، ۴۰۴ برای fan)، آپلود واقعی تصویر روی دیسک محلی + سرو عمومی از `/media/**` (بدون Auth، چون `<img>` مرورگر Header نمی‌فرستد)، هر ویرایش یک Revision Snapshot می‌سازد، انتشار خودکار خبر زمان‌بندی‌شده با یک Job دوره‌ای (تأیید شده با یک خبر واقعی زمان‌بندی‌شده که بعد از رسیدن موعد خودکار published شد) (ADR-0013).
- `/api/v1/loyalty/**` — حساب/امتیاز/سطح/جوایز؛ سطح/نرخ امتیازدهی کاملاً Configurable (بدون Restart قابل تغییر)، کاهش اتمی موجودی امتیاز/جایزه زیر بار هم‌زمانی واقعی (۲۰ Thread، دقیقاً یکی موفق می‌شود)، ساخت هم‌زمان حساب برای یک کاربر تازه هم دقیقاً یک رکورد می‌سازد (تست هم‌زمانی با CountDownLatch، ADR-0018)، کسب امتیاز خودکار از رویداد Payment موجود (Shop/تئاتر) بدون هیچ تغییری در آن ماژول‌ها، و Polling دوره‌ای از یک Endpoint جدید در `ticket.sepahansc` برای امتیاز خرید بلیط فوتبال (تأیید زنده‌ی لایه‌ی Auth/Routing با درخواست واقعی HTTP؛ تست کامل زنجیره با دیتابیس واقعی Django به‌خاطر یک ناهماهنگی از قبل موجود در آن دیتابیس محلی ممکن نشد — جزئیات در ADR-0014) (ADR-0014).
- `/api/v1/notifications/**` — ارسال دستی/آزمایشی SMS (`FakeSmsProvider` پیش‌فرض، `SmsIrProvider` واقعی طبق مستندات رسمی sms.ir آماده و پشت Flag)، Email (`FakeEmailProvider` پیش‌فرض، `SmtpEmailProvider` واقعی روی `spring-boot-starter-mail`)، و Push (`FakePushProvider` پیش‌فرض، `FirebaseCloudMessagingPushProvider` واقعی روی FCM HTTP v1 آماده و پشت Flag -- Phase 16، ADR-0018؛ Device Token با خطای UNREGISTERED خودکار غیرفعال می‌شود)؛ هر تلاش (موفق/ناموفق) در دفترکل ثبت می‌شود؛ ثبت/لغو Device Token برای fan/vip. RBAC: ارسال/مشاهده‌ی دفترکل فقط admin.
- `/api/v1/partners/**` — مدیریت رکورد Partner توسط admin (کلید API فقط یک‌بار، در لحظه‌ی ساخت، نمایش داده می‌شود)؛ `GET /api/v1/partners/me` تنها مسیری که خودِ Partner (با کلید خودش، بدون Keycloak) می‌بیند (ADR-0015).
- `GET /api/v1/users/admin` و `GET /api/v1/payments/admin` — فهرست Read-only (فقط admin) برای Admin Panel؛ بدون منطق جدید، فقط نگاشت به DTO (ADR-0016).

## مرجع تصمیم‌ها

- ساختار ماژول‌ها و مرز هرکدام: [ADR-0002](../docs/adr/0002-modular-monolith-and-module-boundaries.md)
- استراتژی دیتابیس (Schema-per-module): [ADR-0007](../docs/adr/0007-database-strategy.md)
- الگوی Integration Provider: [ADR-0005](../docs/adr/0005-integration-provider-pattern.md)
- Baseline نسخه‌ها: [ADR-0009](../docs/adr/0009-spring-boot-baseline.md)
- مدل داده‌ی users/fan: [docs/database/erd-users-fan.md](../docs/database/erd-users-fan.md)
- مدل داده‌ی ticketing: [docs/database/erd-ticketing.md](../docs/database/erd-ticketing.md)
- قفل هم‌زمانی رزرو صندلی: [ADR-0010](../docs/adr/0010-ticketing-redis-locking.md)
- معماری Payment (Zibal + Event-driven decoupling): [ADR-0011](../docs/adr/0011-payment-architecture.md)
- ماژول Shop (Inventory اتمی، Provider Interface برای Shipping، گردش‌کار Returns): [ADR-0012](../docs/adr/0012-shop-module.md)
- مدل داده‌ی shop: [docs/database/erd-shop.md](../docs/database/erd-shop.md)
- ماژول News/CMS (Media دیسک محلی، Revision، انتشار زمان‌بندی‌شده): [ADR-0013](../docs/adr/0013-news-cms.md)
- مدل داده‌ی news: [docs/database/erd-news.md](../docs/database/erd-news.md)
- ماژول Loyalty (سطح/امتیاز Configurable، Polling بلیط فوتبال از Django): [ADR-0014](../docs/adr/0014-loyalty-module.md)
- مدل داده‌ی loyalty: [docs/database/erd-loyalty.md](../docs/database/erd-loyalty.md)
- Notifications (SMS.ir/SMTP/Push-Fake) و اسکلت Partners: [ADR-0015](../docs/adr/0015-notifications-and-partners.md)
- مدل داده‌ی notifications/partners: [docs/database/erd-notifications-and-partners.md](../docs/database/erd-notifications-and-partners.md)
- Admin Panel (Next.js 16 + Auth.js/Keycloak BFF + Ant Design): [ADR-0016](../docs/adr/0016-admin-panel.md)
- اپ موبایل (Expo + expo-auth-session/Keycloak PKCE، CORS برای فراخوانی مستقیم، رفع باگ Race در JIT Provisioning): [ADR-0017](../docs/adr/0017-mobile-app.md)

## نقشه‌ی ماژول‌ها

| ماژول | مسئولیت خلاصه | وضعیت |
|---|---|---|
| `auth` | اعتبارسنجی توکن Keycloak، نگاشت نقش‌ها | ✅ Phase 6 |
| `users` | پروفایل کاربر (آینه‌ی Keycloak Subject) | ✅ Phase 6 (+ فهرست Admin، Phase 14) |
| `fan` | Fan ID، کارت عضویت | ✅ Phase 6 (`fan_profile`)؛ `membership_card` فقط در Migration، بدون Entity/Service (منطق صدور هنوز تصمیم‌گیری نشده) |
| `news` | CMS اخبار | ✅ Phase 11 |
| `sports` / `matches` | محتوای ورزشی + متادیتای نمایشی فوتبال (بدون صندلی/بلیط — آن در Django است) | — |
| `ticketing` | موتور عمومی Venue/Event/Seat — فعلاً فقط برای **تئاتر** ([ADR-0006](../docs/adr/0006-inhouse-theater-ticketing.md)) | ✅ Phase 8 (Reservation)، ✅ Phase 9 (پرداخت واقعی و صدور بلیط از طریق `payments`، جایگزین مسیر ساده‌شده) |
| `shop` / `products` / `cart` / `orders` | فروشگاه اینترنتی (بازسازی کامل) | ✅ Phase 10 |
| `payments` | ماژول مرکزی پرداخت + Provider Interface | ✅ Phase 9 (+ فهرست Admin، Phase 14) |
| `wallet` | کیف‌پول Fan (جدا از Wallet داخلی Django) | — بدون فاز اختصاصی مشخص در بریف؛ فعلاً فقط `PaymentPurpose.wallet_topup` به‌عنوان Placeholder وجود دارد |
| `loyalty` | امتیاز/سطح/جوایز، Configurable | ✅ Phase 12 |
| `entertainment` / `insurance` / `vehicle` / `travel` | دامنه‌های Provider-محور | — کنارگذاشته‌شده در Phase 13 (بدون Provider واقعی تأییدشده -- طبق ADR-0015، حدس‌زدن API ممنوع است) |
| `notifications` | SMS/Push/Email، Provider قابل تعویض | ✅ Phase 13 (جایگزین برنامه‌ریزی اولیه‌ی Phase 16 -- ADR-0015) |
| `partners` | مدیریت Partnerها | ✅ Phase 13 (فقط اسکلت ساختاری -- طراحی API واقعی هر Partner موضوع فاز خودش) |
| `integrations` | Adapterهای همه‌ی Providerها + پل ارتباطی با Django | — منتظر تصمیم روی یک Provider واقعی برای هرکدام از حوزه‌های کنارگذاشته‌شده |
| `audit` | لاگ ممیزی | — |
