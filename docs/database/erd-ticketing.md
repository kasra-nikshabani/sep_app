# ERD — `ticketing` (موتور داخلی بلیط تئاتر)

مرجع تصمیم: [ADR-0006](../adr/0006-inhouse-theater-ticketing.md). قراردادهای پایه: [conventions.md](conventions.md).

## دیاگرام

```mermaid
erDiagram
    VENUE ||--o{ VENUE_SEAT : "دارد"
    VENUE ||--o{ EVENT : "میزبان"
    EVENT ||--o{ EVENT_SEAT : "وضعیت هر صندلی برای این اجرا"
    VENUE_SEAT ||--o{ EVENT_SEAT : "نمونه‌سازی می‌شود"
    EVENT_SEAT ||--o| RESERVATION : "قفل موقت"
    EVENT_SEAT ||--o| TICKET : "بعد از پرداخت موفق"

    VENUE {
        uuid id PK
        varchar name
        varchar city
        text address
        timestamptz created_at
        timestamptz updated_at
        timestamptz deleted_at
    }

    VENUE_SEAT {
        uuid id PK
        uuid venue_id FK
        varchar section "مثلاً سالن اصلی/بالکن"
        varchar row_label
        varchar seat_number
        timestamptz created_at
        timestamptz deleted_at
    }

    EVENT {
        uuid id PK
        uuid venue_id FK
        varchar title
        text description
        timestamptz starts_at
        integer duration_minutes
        varchar status "draft|published|cancelled|completed"
        numeric base_price
        timestamptz created_at
        timestamptz updated_at
        timestamptz deleted_at
    }

    EVENT_SEAT {
        uuid id PK
        uuid event_id FK
        uuid venue_seat_id FK
        varchar status "available|held|sold"
        numeric price "می‌تواند از base_price متفاوت باشد (مثلاً ردیف VIP)"
        timestamptz created_at
        timestamptz updated_at
    }

    RESERVATION {
        uuid id PK
        uuid event_seat_id FK UK "یک EVENT_SEAT فقط یک Reservation فعال دارد"
        uuid user_id FK "-> users.app_user.id، بدون FK فیزیکی Cross-schema (طبق conventions.md)"
        timestamptz expires_at
        timestamptz created_at
    }

    TICKET {
        uuid id PK
        uuid event_seat_id FK UK
        uuid user_id FK "-> users.app_user.id"
        varchar ticket_number UK
        text qr_payload
        varchar status "valid|used|refunded"
        timestamptz issued_at
        timestamptz created_at
        timestamptz updated_at
        timestamptz deleted_at
    }
```

## تصمیم‌های طراحی

### چرا `VENUE_SEAT` از `EVENT_SEAT` جداست

`VENUE_SEAT` چیدمان فیزیکی و ثابت سالن است (یک سالن تئاتر بارها برای اجراهای مختلف استفاده می‌شود). `EVENT_SEAT` وضعیت آن صندلی را **فقط برای یک اجرای مشخص** نشان می‌دهد. این دقیقاً همان الگویی است که سیستم فوتبال Django با `Seat` در برابر `MatchSeat` استفاده می‌کند (کشف‌شده در Phase 0) — یک الگوی اثبات‌شده برای این مسئله، هرچند این‌جا با کد/جدول کاملاً مستقل و تازه پیاده‌سازی شده (بدون هیچ وابستگی به Django).

### چرا `RESERVATION` و `TICKET` جدول جدا هستند، نه یک فیلد وضعیت

`RESERVATION` یک قفل **موقت** با انقضا است (تا زمانی که کاربر پرداخت را تکمیل کند). `TICKET` رکورد **دائمی** بعد از پرداخت موفق است. جدا نگه‌داشتن این دو یعنی:
- منطق انقضای Reservation (که باید مرتب پاک‌سازی شود) هرگز به رکورد Ticket واقعی دست نمی‌زند.
- Audit/گزارش مالی روی Ticket تمیز می‌ماند، بدون آلوده‌شدن به تلاش‌های ناموفق/رهاشده.

### قفل هم‌زمانی: Redis + دیتابیس، نه فقط دیتابیس

جدول `RESERVATION` به‌تنهایی کافی نیست — بین چک‌کردن «این صندلی آزاد است؟» و نوشتن ردیف Reservation، دو درخواست هم‌زمان می‌توانند هر دو موفق فرض کنند (دقیقاً همان Race Condition که در Django یک‌بار برای کیف‌پول/VIP/کد تخفیف رخ داده بود). به‌جای تکیه‌ی صرف به Transaction Isolation دیتابیس، از یک قفل اتمیک در Redis (`SET NX EX`) روی هر `event_seat_id` استفاده می‌شود — همان الگوی کلی‌ای که سیستم فوتبال Django از قبل برای رزرو هم‌زمان صندلی به کار می‌برد (Redis-backed)، این‌بار به‌صورت مستقل در Spring Boot پیاده‌سازی شده. جزئیات پیاده‌سازی در [ADR-0010](../adr/0010-ticketing-redis-locking.md).

### فیلدهای عمداً حذف‌شده در این فاز

- تخفیف/کد تخفیف، کیف‌پول، درگاه پرداخت واقعی — این‌ها موضوع Phase 9 (Payment Architecture) هستند؛ `TICKET` فعلاً فقط بعد از یک مسیر ساده‌شده (بدون پرداخت واقعی) قابل صدور است تا زیرساخت Reservation تست شود.
- `qr_payload` امضا/رمزنگاری واقعی ندارد هنوز — فقط یک مقدار Placeholder ساخته می‌شود؛ تولید QR واقعی و امن موضوع فاز بعدی خواهد بود.
