# Backend — Spring Boot Modular Monolith

**وضعیت:** قابل‌اجرا (Phase 12). Spring Boot 3.5.16 / Java 21 / Maven (با Maven Wrapper) — [ADR-0009](../docs/adr/0009-spring-boot-baseline.md). ماژول‌های واقعاً پیاده‌شده تا این فاز: `auth`, `users`, `fan`, `ticketing`, `payments`, `shop`, `news`, `loyalty` (بقیه‌ی جدول زیر هنوز فقط برنامه‌ریزی‌شده‌اند، در فازهای خودشان ساخته می‌شوند).

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
- `GET /api/v1/users/me` — با اولین توکن معتبر یک کاربر، رکورد `users.app_user` + `fan.fan_profile` به‌صورت خودکار ساخته می‌شود (JIT، معادل [ADR-0008](../docs/adr/0008-new-user-jit-provisioning.md) برای Django) و در فراخوانی‌های بعدی همان رکورد برگردانده می‌شود — نه رکورد تکراری.
- `/api/v1/ticketing/**` — مرور رویداد/صندلی، رزرو، لغو؛ نقش `admin` برای ساخت سالن/رویداد اجباری است (۴۰۳ برای نقش `fan`)؛ رزرو هم‌زمان دو کاربر روی یک صندلی — دقیقاً یکی موفق می‌شود (ADR-0010، تست با ۲۵ Thread واقعاً هم‌زمان). صدور بلیط دیگر مسیر ساده‌شده نیست — فقط از طریق `payments` (زیر) اتفاق می‌افتد.
- `/api/v1/payments/**` — پرداخت واقعی (Zibal Sandbox، تأیید سرور-به-سرور، Idempotency) + `FakePaymentProvider` برای تست بدون شبکه؛ جریان کامل رزرو→پرداخت→Callback→صدور بلیط با هر دو Provider تست شده (ADR-0011).
- `/api/v1/shop/**` — کاتالوگ/سبد/Checkout/کد تخفیف/مرجوعی؛ کاهش اتمی موجودی زیر بار هم‌زمانی واقعی (۲۰ Thread روی آخرین واحد موجودی — دقیقاً یکی موفق می‌شود)، RBAC (`admin` برای کاتالوگ/گردش‌کار مرجوعی)، جریان کامل Checkout→پرداخت→`paid` از طریق همان رویداد Payment (ADR-0012).
- `/api/v1/news/**` — کاتالوگ/برچسب/خبر/رسانه؛ RBAC (Draft فقط برای admin قابل مشاهده، ۴۰۴ برای fan)، آپلود واقعی تصویر روی دیسک محلی + سرو عمومی از `/media/**` (بدون Auth، چون `<img>` مرورگر Header نمی‌فرستد)، هر ویرایش یک Revision Snapshot می‌سازد، انتشار خودکار خبر زمان‌بندی‌شده با یک Job دوره‌ای (تأیید شده با یک خبر واقعی زمان‌بندی‌شده که بعد از رسیدن موعد خودکار published شد) (ADR-0013).
- `/api/v1/loyalty/**` — حساب/امتیاز/سطح/جوایز؛ سطح/نرخ امتیازدهی کاملاً Configurable (بدون Restart قابل تغییر)، کاهش اتمی موجودی امتیاز/جایزه زیر بار هم‌زمانی واقعی (۲۰ Thread، دقیقاً یکی موفق می‌شود)، کسب امتیاز خودکار از رویداد Payment موجود (Shop/تئاتر) بدون هیچ تغییری در آن ماژول‌ها، و Polling دوره‌ای از یک Endpoint جدید در `ticket.sepahansc` برای امتیاز خرید بلیط فوتبال (تأیید زنده‌ی لایه‌ی Auth/Routing با درخواست واقعی HTTP؛ تست کامل زنجیره با دیتابیس واقعی Django به‌خاطر یک ناهماهنگی از قبل موجود در آن دیتابیس محلی ممکن نشد — جزئیات در ADR-0014) (ADR-0014).

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

## نقشه‌ی ماژول‌ها

| ماژول | مسئولیت خلاصه | وضعیت |
|---|---|---|
| `auth` | اعتبارسنجی توکن Keycloak، نگاشت نقش‌ها | ✅ Phase 6 |
| `users` | پروفایل کاربر (آینه‌ی Keycloak Subject) | ✅ Phase 6 |
| `fan` | Fan ID، کارت عضویت | ✅ Phase 6 (`fan_profile`)؛ `membership_card` فقط در Migration، بدون Entity/Service (منطق صدور هنوز تصمیم‌گیری نشده) |
| `news` | CMS اخبار | ✅ Phase 11 |
| `sports` / `matches` | محتوای ورزشی + متادیتای نمایشی فوتبال (بدون صندلی/بلیط — آن در Django است) | — |
| `ticketing` | موتور عمومی Venue/Event/Seat — فعلاً فقط برای **تئاتر** ([ADR-0006](../docs/adr/0006-inhouse-theater-ticketing.md)) | ✅ Phase 8 (Reservation)، ✅ Phase 9 (پرداخت واقعی و صدور بلیط از طریق `payments`، جایگزین مسیر ساده‌شده) |
| `shop` / `products` / `cart` / `orders` | فروشگاه اینترنتی (بازسازی کامل) | ✅ Phase 10 |
| `payments` | ماژول مرکزی پرداخت + Provider Interface | ✅ Phase 9 |
| `wallet` | کیف‌پول Fan (جدا از Wallet داخلی Django) | — بدون فاز اختصاصی مشخص در بریف؛ فعلاً فقط `PaymentPurpose.wallet_topup` به‌عنوان Placeholder وجود دارد |
| `loyalty` | امتیاز/سطح/جوایز، Configurable | ✅ Phase 12 |
| `entertainment` / `insurance` / `vehicle` / `travel` | دامنه‌های Provider-محور | — Phase 13 |
| `notifications` | SMS/Push/Email | — Phase 16 |
| `partners` | مدیریت Partnerها | — Phase 13 |
| `integrations` | Adapterهای همه‌ی Providerها + پل ارتباطی با Django | — Phase 13/7 |
| `audit` | لاگ ممیزی | — |
