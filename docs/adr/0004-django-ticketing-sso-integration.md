# ADR-0004: الگوی اتصال SSO به Django Ticketing

- **وضعیت:** پیاده‌سازی شده (Phase 7 — Branch محلی، هنوز Merge/Deploy نشده)
- **تصمیم‌گیرندگان:** کارفرما + Architect

## Context

`ticket.sepahansc` یک سیستم Django Production زنده است که:
- طبق بند ۶ بریف **نباید بازنویسی شود**.
- دسترسی SSH/Deploy مستقیم به سرور Production آن **در اختیار Architect نیست** (تأیید صریح کارفرما در این گفتگو) — یعنی هر تغییری باید به‌صورت Patch/PR آماده شود و تیم/کارفرما آن را روی سرور واقعی Deploy کنند.
- در حال حاضر کاملاً Session-based است (دو Backend: `PhoneBackend` برای OTP کاربران عادی، `ModelBackend` برای ادمین/VIP)، بدون هیچ JWT/OAuth.
- یک سرویس هویت شخص‌ثالث دیگر هم دارد (`fans.footballeticket.ir` برای استعلام ثبت‌احوال) که طبق تصمیم کارفرما **داخلی/Legacy می‌ماند** و در این فاز به معماری Integration Provider منتقل نمی‌شود.

بریف دو الگوی ممکن برای اتصال مطرح کرده (بخش ۶): اعتبارسنجی مستقیم توکن در Django، یا عبور از یک Ticketing Integration Layer در Spring Boot.

## Decision

**الگوی اصلی: اعتبارسنجی مستقیم OIDC در Django، به‌صورت افزودنی (Additive) نه جایگزین.**

```
Mobile App
    |  (Bearer JWT از Keycloak)
    v
Django Ticketing
    |  اعتبارسنجی JWT در برابر JWKS Endpoint کیکلوک (client: django-ticketing)
    |  + یک Authentication Backend جدید (مثلاً KeycloakBearerBackend)
    |  که کنار PhoneBackend و ModelBackend موجود اضافه می‌شود، نه جایگزین آن‌ها
    v
نگاشت به User موجود Django بر اساس national_code (Claim از Keycloak)
```

نکات کلیدی این تصمیم:

1. **هیچ‌چیزی از منطق فعلی Django حذف یا بازنویسی نمی‌شود.** ورود ادمین/VIP با رمز عبور و ورود عادی با OTP دقیقاً به همان شکل امروز باقی می‌مانند (مثلاً برای کاربرانی که هنوز اپ موبایل جدید را نصب نکرده‌اند یا از وب سایت فعلی استفاده می‌کنند).
2. یک Authentication Backend **جدید و مجزا** اضافه می‌شود که فقط وقتی فعال است که درخواست از اپ موبایل جدید با Bearer Token معتبر Keycloak برسد.
3. **Ticketing Integration Layer** (لایه‌ی دوم، `integrations/ticketing/` در Spring Boot) فقط برای موارد Admin/Aggregation استفاده می‌شود — مثلاً وقتی Admin Panel جدید بخواهد آمار سفارش‌های Shop + بلیط فوتبال را کنار هم نمایش دهد، یا وقتی ماژول `loyalty` بخواهد رویداد «خرید بلیط» را از Django دریافت کند (Webhook یا Polling — تصمیم دقیق در Phase 12/13).
4. چون دسترسی مستقیم به سرور نداریم، تحویل این تغییر به‌صورت **Pull Request / Patch مستند** روی همان Repository (`ticket.sepahansc`) خواهد بود، با توضیح دقیق نحوه‌ی تنظیم Environment Variable جدید (`KEYCLOAK_JWKS_URL`, `KEYCLOAK_CLIENT_ID`) — بدون هیچ راه‌حل موقتی یا Bypass امنیتی (طبق تأکید صریح بند ۶ بریف).

## Alternatives Considered

| گزینه | مزیت | عیب | چرا رد شد (فعلاً) |
|---|---|---|---|
| فقط از طریق Spring Boot Proxy (همه‌ی درخواست‌های بلیط فوتبال از اپ موبایل از Spring Boot عبور کنند) | یک نقطه‌ی کنترل مرکزی؛ Django هیچ تغییری نمی‌بیند | یک Hop اضافه برای هر درخواست بلیط فوتبال؛ باید کل API Surface بلیط فوتبال را در Spring Boot Mirror/Proxy کنیم که عملاً بازسازی جزئی منطق موجود Django است | نقض روح بند ۶ بریف («بدون بازنویسی»)؛ برای فاز اول رد می‌شود، ولی به‌عنوان الگوی مکمل برای موارد Admin/Aggregation نگه داشته می‌شود |
| جایگزینی کامل Auth فعلی Django با OIDC | یکپارچگی کامل | ریسک بسیار بالا روی سیستم زنده؛ نقض صریح «نباید بازنویسی شود» و «هیچ راه‌حل ناامن/موقت» | رد شد |

## Consequences

**مثبت:** ریسک صفر برای کاربران فعلی/جریان‌های موجود Django؛ مسیر مهاجرت تدریجی به SSO فراهم می‌شود بدون Downtime یا Big-bang Migration.

**ریسک/نیازمند توجه:**
- دو مسیر ورود موازی (Session قدیمی + Bearer جدید) یعنی تیم توسعه باید هر دو را در تست‌های Phase 18 پوشش دهد.
- نگاشت `national_code` باید حالت «کاربر در Keycloak هست ولی در Django نیست» (کاربر کاملاً جدید که فقط از اپ موبایل ثبت‌نام کرده) را هم پوشش دهد — **تصمیم گرفته شد، [ADR-0008](0008-new-user-jit-provisioning.md): JIT Provisioning**، پیاده‌سازی واقعی در Phase 7.
- چون Deploy روی سرور واقعی در اختیار کارفرما/تیم فعلی است، هماهنگی زمان‌بندی Deploy این تغییر باید از قبل هماهنگ شود تا با تغییرات دیگر روی همان سیستم (که طبق گزارش موجود، هنوز فعالانه توسعه می‌یابد) تداخل نکند.

## Open Questions برای Phase 7

- ~~سناریوی دقیق کاربر جدید موبایل بدون رکورد Django چیست؟~~ پاسخ داده شد — [ADR-0008](0008-new-user-jit-provisioning.md).
- آیا Webhook از Django به Spring Boot برای رویداد خرید بلیط (جهت Loyalty) پیاده می‌شود یا Polling کافی است؟ (همچنان باز، موضوع Phase 12/13)

## پیاده‌سازی واقعی (Phase 7)

Branch محلی `feature/keycloak-sso` روی Repository `ticket.sepahansc/football_tickets` (Commit `0bf63d4`) — **فقط Commit محلی، بدون Push/Merge/Deploy**، طبق تصمیم صریح کارفرما در همین گفتگو.

دو تفاوت جزئی نسبت به طراحی اولیه‌ی بالا که در حین کدنویسی واقعی مشخص شدند:

1. **Middleware، نه `AuthenticationBackend`.** بند ۲ بالا از عبارت «Authentication Backend جدید» استفاده کرده بود؛ در عمل، چون این پروژه از DRF استفاده نمی‌کند (Views ساده‌ی Django) و نیاز به احراز هویت Bearer در سطح هر درخواست (نه فقط لحظه‌ی `login()`) دارد، الگوی درست‌تر یک Middleware اضافی (`KeycloakBearerAuthenticationMiddleware`) است که بعد از `AuthenticationMiddleware` استاندارد جنگو ثبت می‌شود: فقط وقتی `request.user` هنوز Anonymous است (یعنی Session موجود نیست) تلاش می‌کند از روی Bearer Token کاربر را تشخیص دهد. نتیجه‌ی نهایی («افزودنی، نه جایگزین») دقیقاً همان چیزی است که این ADR خواسته بود؛ فقط مکانیزم پیاده‌سازی دقیق‌تر شد.
2. **بررسی `azp` (Authorized Party)، نه فقط امضا/صادرکننده.** برای جلوگیری از این‌که یک Client ناشناس داخل همان Realm بتواند کاربر Django بسازد، توکن‌ها فقط اگر `azp` آن‌ها در فهرست `KEYCLOAK_TRUSTED_CLIENTS` (پیش‌فرض: `mobile-app,admin-panel`) باشد پذیرفته می‌شوند.

جزئیات کامل، دلیل هر تصمیم، و ۱۳ تست (شامل رد توکن با صادرکننده/Client نامعتبر و منقضی) در Commit بالا مستند شده‌اند.
