# ADR-0008: Provisioning خودکار کاربر جدید Fan ID در Django (JIT)

- **وضعیت:** پیشنهادی — نیازمند تأیید کارفرما
- **تصمیم‌گیرندگان:** کارفرما + Architect
- **مرجع:** پاسخ به Open Question بازمانده از [ADR-0004](0004-django-ticketing-sso-integration.md)

## Context

طبق ADR-0004، Django یک Authentication Backend جدید دریافت می‌کند که Bearer Token صادرشده توسط Keycloak را اعتبارسنجی و به رکورد `User` موجود در Django (بر اساس `national_code`) نگاشت می‌کند. سؤال بازمانده: **اگر کاربری فقط از طریق اپ موبایل جدید در Fan ID ثبت‌نام کرده و هرگز در Django ثبت‌نام نکرده باشد، وقتی بخواهد بلیط فوتبال بخرد چه اتفاقی می‌افتد؟**

## Decision

**Just-In-Time (JIT) Provisioning** داخل همان Authentication Backend جدید Django:

هنگام اعتبارسنجی موفق یک Bearer Token، اگر هیچ `User`ی با `national_code` موجود در Claim توکن پیدا نشد، به‌صورت اتمیک (`get_or_create` روی `national_code` که از قبل `unique=True` است — پس Race Condition توسط خود Constraint دیتابیس مهار می‌شود، نه فقط منطق اپلیکیشن) یک رکورد جدید ساخته می‌شود:

| فیلد Django | مقدار |
|---|---|
| `national_code` | از Claim `national_code` توکن |
| `phone_number` | از Claim `phone_number` توکن |
| `user_type` | `'normal'` (کاربر عادی — همان محدودیت‌های `PhoneBackend` موجود روی این کاربر هم اعمال می‌ماند) |
| `is_phone_verified` | `True` — چون شماره از قبل توسط Keycloak/OTP در مسیر ثبت‌نام Fan ID تأیید شده |
| `username` | `national_code` (مطابق رفتار فعلی سیستم: «کد ملی را به‌عنوان Username استفاده کن») |
| فیلد جدید `fan_id_subject` (Nullable) | `sub` claim از Keycloak — برای جلوگیری از Lookup تکراری بر اساس `national_code` و امکان Audit/Revoke دقیق‌تر در آینده |

این یعنی یک **Migration کوچک و کاملاً Backward-compatible** به Django اضافه می‌شود: یک ستون Nullable جدید (`fan_id_subject`) روی مدل `User` موجود. هیچ فیلد/رفتار موجودی تغییر نمی‌کند؛ کاربرانی که از قبل هستند این ستون را `NULL` می‌بینند تا اولین ورودشان با Fan ID.

## Alternatives Considered

| گزینه | توضیح | چرا رد شد |
|---|---|---|
| **B — بدون Auto-provisioning** | کاربر Fan-only تا وقتی به‌صورت جدا در Django ثبت‌نام نکند، نمی‌تواند بلیط فوتبال بخرد | مستقیماً نقض هدف اصلی SSO در بند ۵ بریف («کاربر نباید برای هر سرویس Login/ثبت‌نام جداگانه داشته باشد») |
| **C — Provisioning پیشگیرانه از طریق Webhook** | سرویس `fan` در Spring Boot به‌محض ساخت Fan ID جدید، یک Endpoint جدید در Django را برای ساخت User فراخوانی می‌کند | نیازمند یک API Surface تازه در Django (خلاف اصل «تغییر حداقلی»)؛ مشکل Race مشابه را حل نمی‌کند (اگر Webhook Fail/Delay شود، کاربر همچنان ممکن است زودتر از تکمیل Webhook به Django مراجعه کند)؛ پیچیدگی اضافه بدون کاهش ریسک واقعی |

**چرا JIT (گزینه‌ی A) انتخاب شد:** ساده‌ترین راهی که همیشه صحیح است (Correctness) — دقیقاً لحظه‌ای که کاربر واقعاً به Django نیاز دارد، رکورد ساخته می‌شود، بدون افزودن API Surface یا زیرساخت Sync جدید. Race Condition با همان Unique Constraint موجود دیتابیس (نه منطق اپلیکیشن) مهار می‌شود که از قبل هم در Django برای `national_code`/`phone_number` وجود دارد.

## Consequences

**مثبت:** کاربر Fan ID بدون هیچ گام دستی اضافه می‌تواند بلیط فوتبال بخرد؛ صفر تغییر در منطق موجود Django، فقط یک افزودنی محدود.

**ریسک/نیازمند توجه در Phase 7 (پیاده‌سازی واقعی):**
- کاربر تازه‌ساخته‌شده Wallet داخلی Django ندارد به‌صورت خودکار — باید بررسی شود که مسیر ساخت `Wallet` هم (اگر روی `post_save` سیگنال User در Django تعریف شده) برای این مسیر جدید هم درست فعال شود.
- این Migration باید دقیقاً طبق بند ۱۱ بریف: قابل بازگشت (Reversible) و پیش از اجرا با کارفرما/تیم فعلی هماهنگ شود — طبق ADR-0004، من دسترسی مستقیم Deploy روی سرور Production ندارم؛ این Migration هم مثل بقیه‌ی تغییرات Django به‌صورت Patch/PR تحویل داده می‌شود.
- تست این مسیر (کاربر کاملاً جدید + خرید هم‌زمان از دو دستگاه) باید بخشی از Test Plan Phase 18 باشد چون دقیقاً همان الگوی Race Condition است که گزارش موجود سیستم قبلاً یک‌بار در کیف‌پول/VIP/کد تخفیف پیدا و رفع کرده بود.
