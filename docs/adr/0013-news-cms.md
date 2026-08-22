# ADR-0013: ماژول News / CMS

- **وضعیت:** پذیرفته‌شده (Phase 11)
- **تصمیم‌گیرندگان:** Architect + کارفرما (یک فرک واقعی از طریق AskUserQuestion قبل از شروع پیاده‌سازی تأیید شد)

## Context

طبق بند ۱۷ بریف، News باید از طریق Admin قابل مدیریت باشد و شامل Create/Edit/Publish/Schedule/Categories/Tags/Author/Media/SEO/Draft/Revision باشد. طبق Phase 0، هیچ سیستم News/CMS موجودی کشف نشده بود -- این ماژول کاملاً Greenfield است (برخلاف Shop که حداقل یک پروتوتایپ UI/UX داشت).

قبل از شروع پیاده‌سازی، یک فرک معماری واقعی که نمی‌شد بدون نظر کارفرما حدس زد از طریق AskUserQuestion مطرح شد: **ذخیره‌سازی رسانه (Media)**. هیچ سرویس Object Storage مشخصی در بریف/Discovery نیامده بود. کارفرما **«دیسک محلی + سرو استاتیک»** را انتخاب کرد.

## Decision

### 1) Media: دیسک محلی، پشت یک نقطه‌ی تعویض‌پذیر

`MediaService` تنها جایی است که فایل روی دیسک می‌نویسد و `MediaAsset` می‌سازد. برای مقیاس چند-Instance/CDN در آینده، فقط همین یک کلاس باید عوض شود (مثلاً به یک Provider Interface مثل `ShippingProvider`/`PaymentProvider` تبدیل شود اگر Provider دوم لازم شد) -- تصمیم صریح کارفرما، نه حدس یک API خارجی.

**نکته‌ی امنیتی:** نام فایل ذخیره‌شده روی دیسک همیشه توسط خود سیستم تولید می‌شود (`UUID` + پسوند از یک نگاشت ثابت Content-Type→پسوند)، **هرگز** مستقیم از نام فایل ورودی کاربر گرفته نمی‌شود -- جلوگیری از Path Traversal (مثلاً `../../etc/passwd.png`) یا آپلود فایل با پسوند اجرایی دلخواه. نوع فایل هم با یک Allowlist صریح (`image/jpeg|png|webp|gif`) کنترل می‌شود، نه صرفاً پسوند نام فایل که به‌سادگی قابل جعل است.

**نکته‌ی امنیتی دوم:** مسیر `/media/**` در `SecurityConfig` عمداً `permitAll` است -- مرورگر برای تگ `<img src>` هرگز Header سفارشی (از جمله `Authorization`) نمی‌فرستد؛ محتوای این مسیر هم صرفاً فایل رسانه‌ی عمومی خبر است، نه داده‌ی حساس. این دقیقاً هم‌الگوی استثنای Callback پرداخت (ADR-0011) است: یک مسیر عمومی مشخص با دلیل مستند، نه سهل‌انگاری در Auth.

### 2) هر ویرایش، Revision می‌سازد (نه فقط تاریخچه‌ی اختیاری)

`ArticleService.updateArticle` همیشه قبل از اعمال تغییر، حالت *قبلی* عنوان/خلاصه/متن را در `ArticleRevision` (Append-only، بدون Soft-delete) Snapshot می‌کند. طبق بند ۱۷ بریف («Revision» صریحاً فهرست شده)، نه یک ویژگی اختیاری.

### 3) چرخه‌ی عمر: draft → scheduled/published → archived، با انتشار خودکار زمان‌بندی‌شده

`autoPublishScheduledArticles` (هم‌الگوی `releaseExpiredOrders`/`releaseExpiredReservations`) هر ۶۰ ثانیه خبرهای `scheduled` که زمانشان رسیده را `published` می‌کند -- `publishedAt` برابر همان `scheduledAt` درخواستی ثبت می‌شود (نه لحظه‌ی واقعی اجرای Job)، تا تاریخ نمایشی دقیقاً همان چیزی باشد که ادمین درخواست کرده بود.

`publishNow` Idempotent است: اگر خبر از قبل `published` باشد، `publishedAt` تغییر نمی‌کند -- تاریخ اولین انتشار حفظ می‌شود.

### 4) `slug` بعد از ساخت غیرقابل‌ویرایش است

برای پایداری URL/SEO -- تغییر بعدی Slug لینک‌های موجود (از جمله لینک‌های خارجی/Share شده) را می‌شکند. `ArticleWriteCommand` (مشترک بین Create/Update) عمداً فیلد `slug` ندارد؛ فقط در لحظه‌ی ساخت جدا پاس داده می‌شود.

### 5) `author_id`/تعیین نام نویسنده -- بدون JOIN مستقیم Cross-schema

طبق conventions.md، `news.article.author_id` فقط UUID عمومی است، بدون FK فیزیکی به `users.app_user`. نمایش نام نویسنده در Response با یک فراخوانی جدای `AppUserRepository.findById` در لایه‌ی Controller/Service انجام می‌شود -- ترکیب در سطح اپلیکیشن، نه JOIN مستقیم دیتابیسی؛ دقیقاً همان الگویی که ticketing/shop برای `user_id` استفاده می‌کنند.

## Alternatives Considered

| گزینه | چرا رد شد |
|---|---|
| اتصال به یک سرویس Object Storage واقعی (S3/MinIO) از همین فاز | هیچ سرویس مشخصی در بریف/Discovery نیامده -- نقض بند ۸ بریف («هیچ API ای نباید حدس زده شود»)؛ کارفرما هم صریحاً «فقط دیسک محلی» را انتخاب کرد |
| فقط یک فیلد `coverImageUrl` بدون Endpoint آپلود واقعی (مشابه `Product.imageUrl` در Shop) | برخلاف Shop، «Media» صریحاً یکی از امکانات فهرست‌شده‌ی بند ۱۷ بریف است -- نیازمند یک مسیر آپلود واقعی، نه فقط مرجع URL |
| ذخیره‌ی فقط آخرین نسخه، بدون Revision کامل | نقض صریح بند ۱۷ بریف («Revision» به‌عنوان یک امکان مجزا فهرست شده) |

## Consequences

**مثبت:** بدون هیچ Dependency جدید (Multipart Upload از قبل بخشی از `spring-boot-starter-web` است)؛ بدون نیاز به قرارداد/هزینه‌ی یک سرویس Object Storage خارجی برای این فاز.

**ریسک/نیازمند توجه:**
- ذخیره‌سازی روی دیسک محلی یعنی محتوای رسانه به همان Instance/Volume وابسته است -- اگر بعداً Backend روی چند Instance/Container موازی Deploy شود، این مسیر باید به یک Volume مشترک (یا یک Provider واقعی Object Storage) مهاجرت کند؛ `MediaService` تنها نقطه‌ای است که باید عوض شود.
- بدون CDN، بارگذاری تصاویر خبر مستقیماً از خود Backend سرو می‌شود -- برای ترافیک بالا در آینده باید بازبینی شود.
