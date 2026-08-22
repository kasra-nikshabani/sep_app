# ADR-0011: معماری Payment

- **وضعیت:** پذیرفته‌شده (Phase 9)
- **تصمیم‌گیرندگان:** Architect (در چهارچوب ADR-0005 از قبل تأییدشده)

## Context

طبق بند ۱۳ بریف، Payment باید ماژول مستقلی باشد که Order (این‌جا: `ticketing`) هرگز مستقیم به Provider وابسته نباشد، و باید Create/Redirect/Callback/Verify/Inquiry/Refund/Reconciliation را پوشش دهد؛ Idempotency برای عملیات حساس الزامی است. طبق بند ۸ بریف، هیچ API ای نباید حدس زده شود.

**تحقیق واقعی انجام‌شده (نه حدس):** مستندات رسمی Zibal (`help.zibal.ir/IPG/API`) بررسی شد؛ کد واقعی و به‌روز کتابخانه‌ی `zibal_payment` که همین الان در سیستم فوتبال Django استفاده می‌شود خوانده شد؛ و مهم‌تر از همه، **درخواست‌های واقعی به Sandbox عمومی Zibal زده شد** (`merchant: "zibal"` — یک حساب تستی که خودِ Zibal برای همین منظور منتشر کرده) تا شکل دقیق Request/Response تأیید شود.

## Decision

### 1) انتخاب اولین Provider واقعی: **Zibal**

نه چون تنها گزینه است، بلکه چون همین الان در سیستم فوتبال Django با موفقیت و در Production استفاده می‌شود (طبق گزارش موجود سیستم) — یکسان‌سازی Provider بین دو سیستم هزینه‌ی عملیاتی (قرارداد، پشتیبانی، آشنایی تیم) را کم می‌کند. Provider Interface (طبق ADR-0005) این تصمیم را در آینده کاملاً قابل تغییر نگه می‌دارد.

### 2) قرارداد واقعی Zibal (تأییدشده، نه حدسی)

| عملیات | Endpoint | ورودی کلیدی | خروجی کلیدی |
|---|---|---|---|
| Create | `POST /v1/request` | `merchant, amount, callbackUrl, description, orderId?` | `result, message, trackId` |
| Redirect | `GET /start/{trackId}` | — | صفحه‌ی پرداخت |
| Callback | Query String روی `callbackUrl` | — | `trackId, success, status, orderId` — **بدون امضا/توکن امنیتی** |
| Verify | `POST /v1/verify` | `merchant, trackId` | `result, message, amount` |
| Inquiry | `POST /v1/inquiry` | `merchant, trackId` | `result, message, refNumber, paidAt, verifiedAt, status, amount, orderId, description, cardNumber, iban, wage, shaparakFee, createdAt` (تأییدشده با یک درخواست واقعی به Sandbox) |

**نکته‌ی امنیتی حیاتی (طبق مستندات رسمی، نه فرض):** Callback هیچ امضا/HMAC ای ندارد — پارامتر `success` در URL **هرگز نباید مستقیم مبنای تصمیم قرار گیرد** (به‌سادگی توسط کاربر قابل جعل در URL Bar است). تنها راه معتبر، فراخوانی سرور-به-سرور `verify`/`inquiry` با همان `trackId` است. این دقیقاً همان انضباطی است که کد موجود Django هم رعایت می‌کند.

**محدودیت واقعی کشف‌شده:** Zibal در API عمومی خود **هیچ Endpoint مستندی برای Refund ندارد** — فقط در جدول وضعیت‌ها کدهای ۱۵/۱۶ (تراکنش استرداد شده/در حال استرداد) وجود دارند که احتمالاً فقط از طریق پنل Zibal یا هماهنگی دستی با پشتیبانی آن‌ها انجام می‌شود. `PaymentProvider.refund()` در این پیاده‌سازی برای Zibal `UnsupportedOperationException` می‌دهد — **این حدس نیست، نبود مستند رسمی است**؛ اگر بعداً روش واقعی Refund از Zibal پیدا شد، این ADR به‌روزرسانی می‌شود.

### 3) جداسازی Order/Payment با Event، نه فراخوانی مستقیم

`ticketing` مستقیماً `TicketService`/`ReservationService` را بعد از پرداخت صدا نمی‌زند. `PaymentService` بعد از Verify موفق یک `PaymentSucceededEvent` (شامل `purpose`, `referenceId`, `userId`, `amount`) منتشر می‌کند؛ `ticketing` یک Listener مستقل دارد که فقط رویدادهای `purpose=ticket_purchase` را می‌شنود و بلیط صادر می‌کند. این یعنی در Phase 10 (Shop) اضافه‌کردن `purpose=shop_order` نیازی به تغییر کد `payments` ندارد.

### 4) Idempotency

هر (purpose, reference_id) فقط یک Payment در وضعیت `pending` می‌تواند داشته باشد (Partial Unique Index در دیتابیس) — اگر کاربر دکمه‌ی پرداخت را دوبار بزند یا درخواست تکرار شود، همان Payment/Redirect URL موجود برگردانده می‌شود، Payment تکراری ساخته نمی‌شود.

### 5) Circuit Breaker/Retry: **Resilience4j**

طبق ADR-0005، این تصمیم به این فاز موکول شده بود. **Resilience4j** انتخاب شد (نه پیاده‌سازی دستی) چون استاندارد فعلی اکوسیستم Spring Boot است (جایگزین رسمی Hystrix که Deprecated شده) و به‌صورت Annotation-based (`@CircuitBreaker`, `@Retry`, `@TimeLimiter`) با سربار پیاده‌سازی حداقلی قابل استفاده است — روی فراخوانی‌های HTTP خروجی `ZibalPaymentProvider` اعمال می‌شود.

### 6) Provider محلی برای Dev/Test: `FakePaymentProvider`

همیشه موفق (یا با پیکربندی، همیشه ناموفق) — بدون نیاز به شبکه/Zibal، برای تست خودکار Idempotency/Event و توسعه‌ی بدون وابستگی به سرویس خارجی. پیش‌فرض در `application.yml` همین است؛ `zibal` صریحاً باید فعال شود.

## Alternatives Considered

| گزینه | چرا رد شد |
|---|---|
| صدور مستقیم Ticket از `ReservationService.confirmPurchase` بعد از دریافت callback (رویکرد Phase 8) | این دقیقاً همان چیزی بود که Phase 8 آگاهانه به‌عنوان Placeholder موقت پیاده کرد؛ الان که Payment واقعی وجود دارد، این باعث می‌شد `ticketing` مستقیم به‌جزئیات Provider/Verify وابسته شود — نقض صریح بند ۱۳ بریف |
| اعتماد به پارامتر `success` در Callback URL | یک آسیب‌پذیری امنیتی واقعی و ساده (جعل URL) — رد شد، طبق مستندات رسمی Zibal هم صراحتاً توصیه به Verify سرور-به-سرور شده |

## Consequences

**مثبت:** افزودن Provider جدید (مثلاً یک PSP دیگر) یا افزودن مصرف‌کننده‌ی جدید (Shop در Phase 10، شارژ کیف‌پول در همین یا فاز بعد) هیچ‌کدام نیاز به تغییر در طرف مقابل ندارند.

**ریسک/نیازمند توجه:**
- بدون Merchant ID واقعی، تست‌های این فاز فقط با Sandbox عمومی Zibal (`merchant: zibal`) قابل اجراست — قبل از Production، Merchant ID واقعی باید در `.env` جایگزین شود.
- عدم وجود Refund API عمومی یعنی جریان استرداد وجه (اگر لازم شد) باید در فازهای بعد با تیم/پشتیبانی Zibal به‌صورت دستی/قراردادی حل شود؛ این محدودیت زیرساختی است، نه چیزی که کد ما بتواند دور بزند.
