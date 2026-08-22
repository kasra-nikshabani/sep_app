# Backend — Spring Boot Modular Monolith

**وضعیت:** قابل‌اجرا (Phase 9). Spring Boot 3.5.16 / Java 21 / Maven (با Maven Wrapper) — [ADR-0009](../docs/adr/0009-spring-boot-baseline.md). ماژول‌های واقعاً پیاده‌شده تا این فاز: `auth`, `users`, `fan`, `ticketing`, `payments` (بقیه‌ی جدول زیر هنوز فقط برنامه‌ریزی‌شده‌اند، در فازهای خودشان ساخته می‌شوند).

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
- `/api/v1/ticketing/**` — مرور رویداد/صندلی، رزرو، لغو، تکمیل خرید (ساده‌شده، بدون پرداخت واقعی هنوز)؛ نقش `admin` برای ساخت سالن/رویداد اجباری است (۴۰۳ برای نقش `fan`)؛ رزرو هم‌زمان دو کاربر روی یک صندلی — دقیقاً یکی موفق می‌شود (ADR-0010، تست با ۲۵ Thread واقعاً هم‌زمان).

## مرجع تصمیم‌ها

- ساختار ماژول‌ها و مرز هرکدام: [ADR-0002](../docs/adr/0002-modular-monolith-and-module-boundaries.md)
- استراتژی دیتابیس (Schema-per-module): [ADR-0007](../docs/adr/0007-database-strategy.md)
- الگوی Integration Provider: [ADR-0005](../docs/adr/0005-integration-provider-pattern.md)
- Baseline نسخه‌ها: [ADR-0009](../docs/adr/0009-spring-boot-baseline.md)
- مدل داده‌ی users/fan: [docs/database/erd-users-fan.md](../docs/database/erd-users-fan.md)
- مدل داده‌ی ticketing: [docs/database/erd-ticketing.md](../docs/database/erd-ticketing.md)
- قفل هم‌زمانی رزرو صندلی: [ADR-0010](../docs/adr/0010-ticketing-redis-locking.md)

## نقشه‌ی ماژول‌ها

| ماژول | مسئولیت خلاصه | وضعیت |
|---|---|---|
| `auth` | اعتبارسنجی توکن Keycloak، نگاشت نقش‌ها | ✅ Phase 6 |
| `users` | پروفایل کاربر (آینه‌ی Keycloak Subject) | ✅ Phase 6 |
| `fan` | Fan ID، کارت عضویت | ✅ Phase 6 (`fan_profile`)؛ `membership_card` فقط در Migration، بدون Entity/Service (منطق صدور هنوز تصمیم‌گیری نشده) |
| `news` | CMS اخبار | — Phase 11 |
| `sports` / `matches` | محتوای ورزشی + متادیتای نمایشی فوتبال (بدون صندلی/بلیط — آن در Django است) | — |
| `ticketing` | موتور عمومی Venue/Event/Seat — فعلاً فقط برای **تئاتر** ([ADR-0006](../docs/adr/0006-inhouse-theater-ticketing.md)) | ✅ Phase 8 (Reservation کامل؛ Ticket فقط با مسیر ساده‌شده‌ی بدون پرداخت واقعی — Phase 9) |
| `shop` / `products` / `cart` / `orders` | فروشگاه اینترنتی (بازسازی کامل) | — Phase 10 |
| `payments` | ماژول مرکزی پرداخت + Provider Interface | — Phase 9 |
| `wallet` | کیف‌پول Fan (جدا از Wallet داخلی Django) | — Phase 9 |
| `loyalty` | امتیاز/سطح/جوایز، Configurable | — Phase 12 |
| `entertainment` / `insurance` / `vehicle` / `travel` | دامنه‌های Provider-محور | — Phase 13 |
| `notifications` | SMS/Push/Email | — Phase 16 |
| `partners` | مدیریت Partnerها | — Phase 13 |
| `integrations` | Adapterهای همه‌ی Providerها + پل ارتباطی با Django | — Phase 13/7 |
| `audit` | لاگ ممیزی | — |
