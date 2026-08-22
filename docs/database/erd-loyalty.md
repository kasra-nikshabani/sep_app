# ERD — `loyalty`

مرجع تصمیم: [ADR-0014](../adr/0014-loyalty-module.md). قراردادهای پایه: [conventions.md](conventions.md).

## دیاگرام

```mermaid
erDiagram
    LEVEL ||--o{ ACCOUNT : "سطح فعلی"
    ACCOUNT ||--o{ TRANSACTION : "دفترکل"
    ACCOUNT ||--o{ REWARD_REDEMPTION : "مبادله‌ها"
    REWARD ||--o{ REWARD_REDEMPTION : "مبادله‌شده در"

    LEVEL {
        uuid id PK
        varchar name
        integer min_points UK "طبق بند ۱۶ بریف: Configurable، نه Enum ثابت"
        text benefits
    }

    EARNING_RULE {
        uuid id PK
        varchar source_type "shop_order|ticket_purchase|football_ticket_purchase -- حداکثر یکی فعال به‌ازای هر منبع"
        numeric points_per_amount
        boolean is_active
    }

    ACCOUNT {
        uuid id PK
        uuid user_id UK "-> users.app_user.id، بدون FK فیزیکی Cross-schema"
        integer points_balance "قابل‌خرج -- با مبادله کم می‌شود"
        integer lifetime_points "هرگز کم نمی‌شود -- مبنای محاسبه‌ی سطح (ADR-0014)"
        uuid level_id FK
    }

    TRANSACTION {
        uuid id PK
        uuid account_id FK
        varchar type "earn|redeem|adjust"
        integer points "مثبت=earn/adjust مثبت، منفی=redeem/adjust منفی"
        varchar source_type "فقط برای earn پر می‌شود"
        uuid source_reference_id "Idempotency رویدادهای بیرونی (UK با source_type)"
        text description
    }

    REWARD {
        uuid id PK
        varchar name
        integer points_cost
        integer stock_quantity "NULL=نامحدود"
        boolean is_active
    }

    REWARD_REDEMPTION {
        uuid id PK
        uuid account_id FK
        uuid reward_id FK
        integer points_spent
        varchar redemption_code UK
        varchar status "requested|fulfilled|cancelled"
        uuid fulfilled_by
        timestamptz fulfilled_at
    }

    EXTERNAL_POLL_CURSOR {
        varchar source PK "مثلاً django_football_ticket"
        timestamptz last_cursor_at
    }
```

## تصمیم‌های طراحی

### چرا `lifetime_points` جدا از `points_balance` است

طبق ADR-0014: سطح عضویت روی `lifetime_points` (هرگز کم نمی‌شود) محاسبه می‌شود، نه `points_balance` (با خرج‌کردن کم می‌شود) -- تا خرج‌کردن امتیاز باعث افت غیرمنصفانه‌ی سطح نشود.

### چرا `TRANSACTION` یک دفترکل Append-only است، نه فقط دو ستون روی `ACCOUNT`

طبق بند ۱۶ بریف («Transactions» صریحاً فهرست شده) -- تاریخچه‌ی کامل کسب/خرج/اصلاح امتیاز باید قابل مرور باشد، مشابه انضباط `ArticleRevision`/`OrderItem` در فازهای قبل.

### چرا `source_reference_id` روی `TRANSACTION` است، نه یک جدول Idempotency جدا

هم‌الگوی `payments.payment` (ADR-0011): یک Partial Unique Index روی `(source_type, source_reference_id)` مستقیماً جلوی کسب امتیاز تکراری برای یک رویداد را می‌گیرد -- بدون نیاز به جدول/سرویس جدا فقط برای همین منظور.

### فیلدهای عمداً حذف‌شده در این فاز

- هیچ انقضای زمانی برای امتیاز -- در بند ۱۶ بریف ذکر نشده.
- هیچ تحویل خودکار جایزه (پستی/دیجیتال) -- `REWARD_REDEMPTION` فقط یک کد صادر می‌کند؛ تحویل کاملاً دستی توسط ادمین است (ADR-0014).
