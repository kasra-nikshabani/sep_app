# ADR-0005: الگوی Integration Provider

- **وضعیت:** پیشنهادی — نیازمند تأیید کارفرما (پیاده‌سازی واقعی از Phase 9 به بعد)
- **تصمیم‌گیرندگان:** کارفرما + Architect

## Context

طبق بند ۷/۸ بریف، هیچ Business Logic ای نباید مستقیم به API یک شرکت خاص وابسته شود. الگوی خواسته‌شده:

```
Service (مثلاً OrderService)
    -> Domain Service (مثلاً PaymentService)
        -> Provider Interface
            -> Adapter پیاده‌سازی (APProvider, PSPProvider, ...)
```

و برای هر Provider باید این‌ها در نظر گرفته شود: Provider Interface، Adapter، Configuration، Authentication، Timeout، Retry، Circuit Breaker، Logging، Metrics، Error Mapping، Webhook Handling، Idempotency. هیچ Providerی حدس زده نمی‌شود — پیش از پیاده‌سازی هر Adapter واقعی، مستندات رسمی همان Provider بررسی می‌شود.

## Decision

### 5.1 قرارداد یکسان برای همه‌ی دامنه‌ها

این الگو **بدون استثنا** برای همه‌ی دامنه‌های Provider-محور به‌کار می‌رود: `payments`, `banking`, `vehicle`, `insurance`, `travel`, `entertainment` (سینما/کنسرت — نه تئاتر، طبق [ADR-0006](0006-inhouse-theater-ticketing.md))، `sms`.

هر Adapter داخل `integrations/<domain>/<provider-name>/` قرار می‌گیرد و باید این قرارداد را پیاده کند (نام‌گذاری دقیق کلاس‌ها در Phase 6 نهایی می‌شود، این‌جا فقط مسئولیت‌ها مشخص است):

- **Provider Interface** — قرارداد دامنه (مثلاً `PaymentProvider.createPayment/verify/inquiry/refund`)
- **Adapter** — پیاده‌سازی مخصوص یک Provider واقعی
- **Configuration** — از طریق Spring `@ConfigurationProperties`، مقادیر حساس فقط از Environment Variable (هرگز در کد)
- **Authentication** — مطابق مستندات رسمی همان Provider (OAuth، API Key، HMAC، …)
- **Timeout/Retry** — مقدار پیش‌فرض محافظه‌کارانه + Backoff، قابل تنظیم به‌ازای هر Provider
- **Circuit Breaker** — برای جلوگیری از Cascading Failure وقتی یک Provider خارجی از کار می‌افتد
- **Logging/Metrics** — Correlation ID یکسان با بقیه‌ی سیستم (بند ۲۰ بریف)، بدون لاگ کردن داده‌ی حساس (شماره کارت، رمز، توکن)
- **Error Mapping** — تبدیل خطای خاص Provider به یک مدل خطای داخلی یکنواخت
- **Webhook Handling** — اعتبارسنجی امضا (Signature Validation) اجباری برای هر Webhook ورودی
- **Idempotency** — برای عملیات حساس (خصوصاً Payment) اجباری، طبق بند ۱۰/۱۳ بریف

### 5.2 قبل از هر Provider واقعی

پیش از پیاده‌سازی هر Adapter واقعی (آسان‌پرداخت/آپ، PSPها، Open Banking، آی‌تول، سابیم، CityNet، iHotelHub، CinemaTicket، IranTicket، SMS Providers)، مستندات رسمی و API Contract همان سرویس بررسی و به کارفرما گزارش می‌شود؛ **هیچ API ای حدس زده نمی‌شود** (طبق تأکید صریح بند ۸ بریف). این یعنی هر Adapter واقعی، موضوع یک Task/Phase جدا با تأیید مجزا خواهد بود.

## Alternatives Considered

| گزینه | چرا رد شد |
|---|---|
| فراخوانی مستقیم API هر Provider از داخل Service دامنه (مثال BAD در بریف) | نقض صریح بند ۷ بریف؛ تعویض Provider در آینده باعث بازنویسی منطق تجاری می‌شود |
| یک کتابخانه‌ی عمومی Third-party برای مدیریت Retry/Circuit Breaker (مثل Resilience4j) بدون تأیید | طبق قانون ۲۶ بریف، هیچ Dependency جدیدی بدون تأیید صریح کارفرما اضافه نمی‌شود — این‌جا فقط الگو مستند می‌شود؛ انتخاب کتابخانه‌ی واقعی (Resilience4j در برابر پیاده‌سازی دستی) در Phase 6 با ذکر دلیل و درخواست تأیید مطرح خواهد شد |

## Consequences

**مثبت:** تعویض Provider (مثلاً تغییر PSP) به یک Adapter جدید محدود می‌شود، بدون تأثیر روی `OrderService`/`PaymentService`. تست‌پذیری بالا (Provider Interface به‌راحتی Mock می‌شود).

**نیازمند توجه:** تعداد Providerهای فهرست‌شده در بند ۸ بریف زیاد است؛ باید اولویت‌بندی شود کدام Provider در کدام Phase واقعاً پیاده می‌شود (این اولویت‌بندی در Phase 13 با کارفرما مشخص می‌شود، نه این‌جا).
