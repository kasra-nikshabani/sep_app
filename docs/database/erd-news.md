# ERD — `news` (News / CMS)

مرجع تصمیم: [ADR-0013](../adr/0013-news-cms.md). قراردادهای پایه: [conventions.md](conventions.md).

## دیاگرام

```mermaid
erDiagram
    CATEGORY ||--o{ ARTICLE : "دسته‌بندی"
    ARTICLE ||--o{ ARTICLE_TAG : "برچسب‌ها"
    TAG ||--o{ ARTICLE_TAG : "استفاده در"
    ARTICLE ||--o{ ARTICLE_REVISION : "تاریخچه‌ی ویرایش"

    CATEGORY {
        uuid id PK
        varchar name
        varchar slug UK
    }

    TAG {
        uuid id PK
        varchar name
        varchar slug UK
    }

    ARTICLE {
        uuid id PK
        uuid category_id FK
        uuid author_id "-> users.app_user.id، بدون FK فیزیکی Cross-schema"
        varchar title
        varchar slug UK "غیرقابل‌ویرایش بعد از ساخت -- پایداری URL/SEO"
        text summary
        text content
        varchar cover_image_url
        varchar meta_title "SEO"
        varchar meta_description "SEO"
        varchar status "draft|scheduled|published|archived"
        timestamptz published_at
        timestamptz scheduled_at
    }

    ARTICLE_TAG {
        uuid article_id FK
        uuid tag_id FK
    }

    ARTICLE_REVISION {
        uuid id PK
        uuid article_id FK
        varchar title "Snapshot قبل از ویرایش"
        text summary "Snapshot قبل از ویرایش"
        text content "Snapshot قبل از ویرایش"
        uuid edited_by
        timestamptz edited_at
    }

    MEDIA_ASSET {
        uuid id PK
        varchar file_name "فقط نمایشی -- هرگز مسیر ذخیره‌سازی واقعی نیست"
        varchar storage_path "مسیر فیزیکی دیسک -- هرگز به کاربر نشان داده نمی‌شود"
        varchar url "مسیر عمومی Serve، زیر /media/**"
        varchar content_type
        bigint size_bytes
    }
```

## تصمیم‌های طراحی

### چرا `slug` بعد از ساخت غیرقابل‌ویرایش است

پایداری URL/SEO -- تغییر بعدی لینک‌های موجود (از جمله لینک‌های Share شده در شبکه‌ی اجتماعی) را می‌شکند. جزئیات در [ADR-0013](../adr/0013-news-cms.md).

### چرا `ARTICLE_REVISION` جدول جدا و Append-only است

طبق بند ۱۷ بریف («Revision»)، هر ویرایش باید قابل بازیابی باشد. حالت *قبلی* (نه جدید) در این جدول Snapshot می‌شود، پیش از اعمال تغییر روی خود `ARTICLE` -- مشابه انضباط `OrderItem`/`Payment.amount` در فازهای قبل (هیچ داده‌ی تاریخی با تغییر بعدی رکورد اصلی عوض نمی‌شود).

### چرا `MEDIA_ASSET` هیچ FK ای به `ARTICLE` ندارد

یک تصویر می‌تواند قبل از اتصال به هیچ خبری آپلود شود (مثلاً از یک کتابخانه‌ی رسانه‌ی مشترک در آینده)، یا در چند خبر/جای مختلف استفاده شود. `cover_image_url` روی `ARTICLE` فقط یک رشته‌ی URL است، نه یک رابطه‌ی رابطه‌ای سخت.

### فیلدهای عمداً حذف‌شده در این فاز

- هیچ اتصال واقعی به یک سرویس Object Storage (S3/MinIO) -- دیسک محلی، تصمیم صریح کارفرما (ADR-0013).
- هیچ سیستم Comment/نظر روی خبر -- در بریف ذکر نشده.
- کتابخانه‌ی رسانه‌ی قابل‌مرور/جست‌وجو (Media Library UI) -- موضوع Admin Panel، Phase 14.
