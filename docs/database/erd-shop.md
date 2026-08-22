# ERD — `shop` (فروشگاه اینترنتی)

مرجع تصمیم: [ADR-0012](../adr/0012-shop-module.md). قراردادهای پایه: [conventions.md](conventions.md).

## دیاگرام

```mermaid
erDiagram
    CATEGORY ||--o{ CATEGORY : "زیردسته"
    CATEGORY ||--o{ PRODUCT : "دسته‌بندی"
    PRODUCT ||--o{ PRODUCT_VARIANT : "حداقل یک Variant"
    CART ||--o{ CART_ITEM : "آیتم‌ها"
    PRODUCT_VARIANT ||--o{ CART_ITEM : "انتخاب‌شده در"
    SHOP_ORDER ||--o{ ORDER_ITEM : "اقلام سفارش (Snapshot)"
    PRODUCT_VARIANT ||--o{ ORDER_ITEM : "خریداری‌شده"
    SHIPPING_METHOD ||--o{ SHOP_ORDER : "روش ارسال انتخابی"
    COUPON ||--o| SHOP_ORDER : "اعمال‌شده روی"
    COUPON ||--o{ COUPON_REDEMPTION : "مصرف‌شده در"
    SHOP_ORDER ||--o| COUPON_REDEMPTION : "یک بار مصرف کد"
    SHOP_ORDER ||--o{ RETURN_REQUEST : "درخواست مرجوعی"

    CATEGORY {
        uuid id PK
        varchar name
        varchar slug UK
        uuid parent_id FK "خودارجاع -- زیردسته"
    }

    PRODUCT {
        uuid id PK
        uuid category_id FK
        varchar name
        varchar slug UK
        numeric base_price
        boolean is_active
    }

    PRODUCT_VARIANT {
        uuid id PK
        uuid product_id FK
        varchar sku UK
        jsonb attributes "مثلاً {size: L, color: black}"
        numeric price_override "NULL یعنی از base_price محصول استفاده شود"
        integer stock_quantity "کاهش/افزایش فقط با UPDATE اتمی -- ADR-0012"
        integer weight_grams "پیش‌فرض 500 -- برای محاسبه‌ی هزینه‌ی ارسال"
    }

    CART {
        uuid id PK
        uuid user_id UK "-> users.app_user.id، بدون FK فیزیکی Cross-schema"
    }

    CART_ITEM {
        uuid id PK
        uuid cart_id FK
        uuid product_variant_id FK
        integer quantity
    }

    SHIPPING_METHOD {
        uuid id PK
        varchar name
        numeric base_rate
        numeric per_kg_rate "هزینه = base_rate + per_kg_rate * وزن(kg)"
    }

    COUPON {
        uuid id PK
        varchar code UK
        varchar discount_type "percentage|fixed_amount"
        numeric discount_value
        integer max_uses_total
        integer max_uses_per_user
    }

    SHOP_ORDER {
        uuid id PK
        uuid user_id "-> users.app_user.id"
        varchar status "pending_payment|paid|shipped|delivered|cancelled|expired|return_requested|returned"
        numeric subtotal_amount
        numeric discount_amount
        numeric shipping_amount
        numeric total_amount
        uuid coupon_id FK "nullable"
        uuid shipping_method_id FK
        timestamptz expires_at
        boolean requires_manual_review "پرداخت دیرهنگام بعد از انقضا -- ADR-0012"
    }

    ORDER_ITEM {
        uuid id PK
        uuid shop_order_id FK
        uuid product_variant_id FK
        varchar product_name "Snapshot لحظه‌ی خرید"
        numeric unit_price "Snapshot لحظه‌ی خرید"
        integer quantity
        numeric subtotal
    }

    COUPON_REDEMPTION {
        uuid id PK
        uuid coupon_id FK
        uuid shop_order_id FK UK "هر سفارش حداکثر یک بار"
        uuid user_id
    }

    RETURN_REQUEST {
        uuid id PK
        uuid shop_order_id FK
        uuid user_id
        varchar status "requested|approved|rejected|completed"
        varchar order_status_before_return "برای بازگرداندن دقیق وضعیت سفارش در صورت رد"
    }
```

## تصمیم‌های طراحی

### چرا جدول سفارش `shop_order` نام دارد، نه `order`

`order` یک کلمه‌ی رزروشده‌ی SQL است -- استفاده‌ی خام از آن در هر Migration/Query دستی نیاز به Quote دائمی دارد. نام کلاس جاوا همچنان `Order` است (واژگان دامنه)، فقط جدول با `@Table(name = "shop_order")` نگاشت شده.

### چرا هر Product حداقل یک Variant دارد

طبق ADR-0012، این یعنی Cart/Order/Inventory همیشه فقط با `ProductVariant` کار می‌کنند -- حتی محصولات بدون گزینه‌ی واقعی (رنگ/سایز) یک Variant «استاندارد» می‌گیرند، به‌جای دو مسیر کد جدا برای محصول ساده/دارای Variant.

### چرا موجودی با UPDATE اتمی کم می‌شود، نه قفل Redis (برخلاف صندلی تئاتر)

صندلی یک منبع **منحصربه‌فرد** است (یا آزاد یا رزرو). موجودی فروشگاه یک **Pool تعداد** است -- `UPDATE ... WHERE stock_quantity >= :qty` در همان تراکنش دیتابیسی، بدون Redis، همان تضمین صحت را می‌دهد. جزئیات و نتیجه‌ی تست هم‌زمانی در [ADR-0012](../adr/0012-shop-module.md) آمده.

### چرا `unit_price`/`product_name` در `ORDER_ITEM` تکرار می‌شوند

Snapshot لحظه‌ی خرید -- گزارش مالی سفارش‌های قدیمی نباید با تغییر بعدی قیمت/نام محصول عوض شود (همان انضباطی که `payments.payment.amount` دارد).

### فیلدهای عمداً حذف‌شده در این فاز

- هیچ اتصال واقعی به شرکت باربری/پستی -- `SHIPPING_METHOD` فقط نرخ محاسبه‌ی داخلی دارد (تصمیم صریح کارفرما، ADR-0012).
- هیچ ستون/جدول تسویه‌ی مالی خودکار برای `RETURN_REQUEST` -- فقط گردش‌کار (تصمیم صریح کارفرما، ADR-0012).
- دفترچه‌آدرس قابل‌استفاده‌ی مجدد -- آدرس ارسال مستقیم روی `SHOP_ORDER` ذخیره می‌شود، نه یک جدول Address جدا؛ فقط برای همان سفارش لازم است.
