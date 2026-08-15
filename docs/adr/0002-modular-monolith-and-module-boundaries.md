# ADR-0002: Modular Monolith و مرزبندی ماژول‌ها

- **وضعیت:** پیشنهادی — نیازمند تأیید کارفرما (بند ۹ بریف: «بدون تأیید من ساختار نهایی را قطعی نکن»)
- **تصمیم‌گیرندگان:** کارفرما + Architect

## Context

بریف اولیه (بخش ۹) فهرست ماژول‌ها را این‌طور پیشنهاد داده بود:

```
auth, users, fan, news, sports, matches, ticketing, shop, products, cart,
orders, payments, wallet, loyalty, entertainment, insurance, vehicle,
travel, notifications, partners, integrations, audit
```

با یافته‌های Phase 0 و تصمیم‌های تازه‌ی کارفرما، دو نکته نیاز به شفاف‌سازی مرز ماژول‌ها دارند:

1. **بلیط فوتبال هنوز در Django است** (حفظ می‌شود)، اما ماژول `ticketing` طبق بند ۱۴ بریف باید یک موتور *عمومی* Venue/Seat باشد. اگر این ماژول را برای فوتبال هم بسازیم، دو منبع حقیقت (Source of Truth) برای بلیط فوتبال خواهیم داشت — تناقض با بند ۶ بریف.
2. **بلیط تئاتر باید داخلی/خودمان ساخته شود** (تصمیم صریح کارفرما، نه از طریق Provider)، که دقیقاً همان موتور عمومی `ticketing` است.
3. **`sepahan-shop` بازسازی کامل می‌شود** — یعنی ماژول‌های `shop/products/cart/orders` از صفر داخل Spring Boot ساخته می‌شوند، نه Import از پروژه‌ی قبلی.
4. **کیف‌پول دوگانه**: Django از قبل یک `Wallet` سخت‌شده و Race-condition-safe برای فوتبال دارد. ماژول جدید `wallet` هم برای Shop/Theater/سایر سرویس‌ها لازم است. یکی‌کردن این دو، ریسک بالایی به سیستم Production زنده تحمیل می‌کند.

## Decision

### 4.1 Modular Monolith تأیید می‌شود

Backend به‌صورت یک Artifact واحد Spring Boot با پکیج‌بندی ماژولار (نه Microservice) ساخته می‌شود. مهاجرت به Microservice فقط در صورت نیاز واقعی (طبق بند ۴ بریف) در آینده بررسی می‌شود.

### 4.2 مرزبندی اصلاح‌شده‌ی ماژول‌ها

| ماژول | مسئولیت | نکته‌ی مهم |
|---|---|---|
| `auth` | اعتبارسنجی توکن Keycloak، نگاشت نقش‌ها به داخل سیستم | خودِ Keycloak IdP است؛ این ماژول فقط لایه‌ی ادغام است |
| `users` | پروفایل کاربر داخل Spring Boot (آینه‌ای از Keycloak Subject + فیلدهای اپلیکیشنی) | — |
| `fan` | Fan ID، کارت عضویت، پروفایل هواداری | نگاشت `national_code`/`phone` به Keycloak Subject در این‌جا مستند می‌شود ([ADR-0003](0003-keycloak-central-sso-fan-id.md)) |
| `news` | CMS اخبار | مطابق بند ۱۷ بریف |
| `sports` | محتوای عمومی ورزشی (غیر از بلیط) | ممکن است در Phase 2 با `matches` ادغام شود — تصمیم قطعی در Phase 2 |
| `matches` | **فقط متادیتای فیکسچر/نتیجه‌ی مسابقات فوتبال برای نمایش** (Home، اخبار، اطلاع‌رسانی) | **موجودی صندلی و صدور بلیط فوتبال همچنان در Django است.** این ماژول هیچ صندلی/رزروی مدیریت نمی‌کند؛ صرفاً از طریق Ticketing Integration Layer داده‌ی نمایشی را می‌خواند یا Cache می‌کند |
| `ticketing` | موتور عمومی **جدید و اول‌شخص** Venue → Section/Block → Row → Seat → Event → Reservation → Ticket (QR) | طبق تصمیم کارفرما: **فقط تئاتر** فعلاً از این موتور استفاده می‌کند (سینما/کنسرت همچنان Provider-based). طراحی مدل باید از ابتدا عمومی باشد تا تئاتر/سینما/کنسرت/فوتبال آینده را بدون بازنویسی پوشش دهد — جزئیات در [ADR-0006](0006-inhouse-theater-ticketing.md) |
| `shop`, `products`, `cart`, `orders` | فروشگاه اینترنتی — **بازسازی کامل**، `sepahan-shop` فقط مرجع UI/UX | طبق تصمیم کارفرما؛ دیتابیس/بک‌اند مستقل، بدون وابستگی به Prisma/SQLite قبلی |
| `payments` | ماژول مرکزی پرداخت + Payment Provider Interface | مصرف‌کننده‌ها: `orders` (Shop) و `ticketing` (تئاتر) و `wallet` (شارژ کیف‌پول) |
| `wallet` | کیف‌پول Fan برای Shop/Theater/سایر سرویس‌ها | **جدا از Wallet داخلی Django** (که فقط برای فوتبال است) — یکی‌سازی این دو در این فاز تصمیم‌گیری نمی‌شود؛ به‌عنوان ریسک باز ثبت شده (بخش Consequences) |
| `loyalty` | امتیاز/سطح/جوایز، Configurable (نه Hardcode) | منبع رویداد: خرید Shop، خرید بلیط تئاتر؛ اتصال به رویدادهای خرید بلیط فوتبال از Django موضوع Phase 12 است |
| `entertainment`, `insurance`, `vehicle`, `travel` | دامنه‌های Provider-محور | طبق بند ۷/۸ بریف، از طریق `integrations/*` |
| `notifications` | ارسال SMS/Push/Email با Provider قابل تعویض | — |
| `partners` | مدیریت سیستم‌های Partner/Whitelabel | — |
| `integrations` | همه‌ی Adapterهای Provider (شامل Django Ticketing Integration Layer) | جزئیات در [ADR-0005](0005-integration-provider-pattern.md) |
| `audit` | لاگ ممیزی | مصرف‌شونده توسط همه‌ی ماژول‌ها |

## Alternatives Considered

**یکی‌کردن `matches` و `ticketing` در یک ماژول واحد:** رد شد — باعث می‌شد مرز «چه چیزی در Django می‌ماند و چه چیزی جدید است» داخل کد کدر شود. تفکیک صریح، ریسک دست‌کاری تصادفی منطق فوتبال (که در Django است) را از بین می‌برد.

**یکی‌کردن کیف‌پول از روز اول (Single Wallet Source of Truth):** رد شد برای این فاز — کیف‌پول Django به‌تازگی Race-condition آن رفع و تست شده (طبق گزارش موجود سیستم). دست‌کاری آن برای یکی‌شدن با سیستم جدید، ریسک مالی مستقیم به سیستم Production دارد و اولویت Correctness/Security را نقض می‌کند. این تصمیم به‌صراحت به‌عنوان **باز** برای Phase 9 (Payment Architecture) ثبت می‌شود.

## Consequences

**مثبت:** مرز روشن بین «چیزی که در Django می‌ماند» و «چیزی که تازه ساخته می‌شود» از همان ابتدا در معماری صریح است؛ ریسک دست‌کاری ناخواسته‌ی سیستم زنده کاهش می‌یابد.

**منفی / ریسک باز:**
- کاربر نهایی ممکن است دو موجودی کیف‌پول جدا (یکی برای فوتبال در Django، یکی برای بقیه‌ی سرویس‌ها در Fan Wallet جدید) ببیند تا زمانی‌که در Phase 9 تصمیم یکسان‌سازی گرفته شود. این باید به‌صراحت در UI به کاربر نمایش داده شود (نه پنهان)، تا گیج‌کننده نباشد.
- ماژول `sports`/`matches` احتمالاً در Phase 2 بازبینی جزئی می‌شود.

## Open Questions برای Phase 2/Phase 9

- آیا `sports` و `matches` یک ماژول شوند؟
- آیا در آینده (Phase 9) کیف‌پول Django و Fan Wallet یکی می‌شوند یا Reconciliation Layer ساخته می‌شود؟
