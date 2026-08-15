# Backend — Spring Boot Modular Monolith

**وضعیت:** اسکلت ساختاری فقط (Phase 2). بوت‌استرپ واقعی پروژه (Build Tool، Spring Boot Starterها، اولین ماژول قابل‌اجرا) موضوع **Phase 6 — Spring Boot Core** است و پیش از آن هیچ Dependency‌ای اضافه نمی‌شود (قانون ۲۶ بریف).

## مرجع تصمیم‌ها

- ساختار ماژول‌ها و مرز هرکدام: [ADR-0002](../docs/adr/0002-modular-monolith-and-module-boundaries.md)
- استراتژی دیتابیس (Schema-per-module): [ADR-0007](../docs/adr/0007-database-strategy.md)
- الگوی Integration Provider: [ADR-0005](../docs/adr/0005-integration-provider-pattern.md)

## نقشه‌ی ماژول‌های برنامه‌ریزی‌شده (Phase 6 به بعد)

هرکدام به‌صورت یک Package مستقل داخل یک Artifact واحد Maven/Gradle (نه Microservice جدا):

| ماژول | مسئولیت خلاصه |
|---|---|
| `auth` | اعتبارسنجی توکن Keycloak، نگاشت نقش‌ها |
| `users` | پروفایل کاربر (آینه‌ی Keycloak Subject) |
| `fan` | Fan ID، کارت عضویت |
| `news` | CMS اخبار |
| `sports` / `matches` | محتوای ورزشی + متادیتای نمایشی فوتبال (بدون صندلی/بلیط — آن در Django است) |
| `ticketing` | موتور عمومی Venue/Event/Seat — فعلاً فقط برای **تئاتر** ([ADR-0006](../docs/adr/0006-inhouse-theater-ticketing.md)) |
| `shop` / `products` / `cart` / `orders` | فروشگاه اینترنتی (بازسازی کامل) |
| `payments` | ماژول مرکزی پرداخت + Provider Interface |
| `wallet` | کیف‌پول Fan (جدا از Wallet داخلی Django) |
| `loyalty` | امتیاز/سطح/جوایز، Configurable |
| `entertainment` / `insurance` / `vehicle` / `travel` | دامنه‌های Provider-محور |
| `notifications` | SMS/Push/Email |
| `partners` | مدیریت Partnerها |
| `integrations` | Adapterهای همه‌ی Providerها + پل ارتباطی با Django |
| `audit` | لاگ ممیزی |

انتخاب نهایی Build Tool (Maven در برابر Gradle) و اسکلت اجرایی اولیه در Phase 6 با ذکر دلیل مطرح و با تأیید کارفرما نصب می‌شود.
