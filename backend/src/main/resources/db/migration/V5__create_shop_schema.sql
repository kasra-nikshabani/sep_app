-- طبق docs/database/erd-shop.md و ADR-0012 (فروشگاه اینترنتی -- بازسازی کامل، Phase 10)
-- نکته: جدول سفارش عمداً shop_order نام‌گذاری شده، نه order -- چون order یک کلمه‌ی
-- رزروشده‌ی SQL است و استفاده‌ی خام از آن در هر Migration/Query دستی نیاز به Quote دائمی دارد.

CREATE TABLE shop.category (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(150) NOT NULL,
    parent_id   UUID REFERENCES shop.category (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    created_by  UUID,
    updated_by  UUID
);

CREATE UNIQUE INDEX uq_category_slug ON shop.category (slug) WHERE deleted_at IS NULL;

CREATE TABLE shop.product (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id  UUID NOT NULL REFERENCES shop.category (id),
    name         VARCHAR(200) NOT NULL,
    slug         VARCHAR(200) NOT NULL,
    description  TEXT,
    image_url    VARCHAR(500),
    base_price   NUMERIC(12, 0) NOT NULL,
    is_active    BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ,
    created_by   UUID,
    updated_by   UUID
);

CREATE UNIQUE INDEX uq_product_slug ON shop.product (slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_product_category ON shop.product (category_id) WHERE deleted_at IS NULL;

-- هر Product حداقل یک Variant دارد (حتی محصولات بدون گزینه‌ی واقعی -- یک Variant
-- "استاندارد" پیش‌فرض) -- طبق تصمیم ADR-0012 برای این‌که کد Cart/Order همیشه فقط
-- با ProductVariant کار کند، نه دو مسیر جدا برای Product ساده/دارای Variant.
CREATE TABLE shop.product_variant (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID NOT NULL REFERENCES shop.product (id),
    sku             VARCHAR(60) NOT NULL,
    attributes      JSONB,
    price_override  NUMERIC(12, 0),
    stock_quantity  INTEGER NOT NULL DEFAULT 0,
    weight_grams    INTEGER NOT NULL DEFAULT 500,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    created_by      UUID,
    updated_by      UUID,

    CONSTRAINT chk_variant_stock_non_negative CHECK (stock_quantity >= 0)
);

CREATE UNIQUE INDEX uq_variant_sku ON shop.product_variant (sku) WHERE deleted_at IS NULL;
CREATE INDEX idx_variant_product ON shop.product_variant (product_id) WHERE deleted_at IS NULL;

-- سبد خرید ماندگار و مختص کاربر -- طبق ADR-0012 عمداً بدون ستون‌های Audit/Soft-delete
-- پایه (مثل ticketing.reservation) چون داده‌ی گذرا و دائم در حال تغییر است.
CREATE TABLE shop.cart (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE shop.cart_item (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id             UUID NOT NULL REFERENCES shop.cart (id),
    product_variant_id  UUID NOT NULL REFERENCES shop.product_variant (id),
    quantity            INTEGER NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_cart_item_quantity_positive CHECK (quantity > 0)
);

CREATE UNIQUE INDEX uq_cart_item ON shop.cart_item (cart_id, product_variant_id);

-- هزینه‌ی ارسال = base_rate + per_kg_rate * وزن(kg) -- یک فرمول برای هم نرخ ثابت
-- (per_kg_rate=0) و هم نرخ بر اساس وزن، به‌جای دو مسیر کد جدا (طبق بند ۱۶ بریف:
-- Configurable نه Hardcode). بدون اتصال واقعی به هیچ شرکت باربری/پستی -- تصمیم
-- صریح کارفرما در همین فاز (ADR-0012).
CREATE TABLE shop.shipping_method (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(100) NOT NULL,
    base_rate    NUMERIC(12, 0) NOT NULL,
    per_kg_rate  NUMERIC(12, 0) NOT NULL DEFAULT 0,
    is_active    BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ,
    created_by   UUID,
    updated_by   UUID
);

CREATE TABLE shop.coupon (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(40) NOT NULL,
    discount_type       VARCHAR(20) NOT NULL,
    discount_value      NUMERIC(12, 0) NOT NULL,
    min_order_amount    NUMERIC(12, 0),
    max_uses_total      INTEGER,
    max_uses_per_user   INTEGER,
    valid_from          TIMESTAMPTZ,
    valid_to            TIMESTAMPTZ,
    is_active           BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,
    created_by          UUID,
    updated_by          UUID,

    CONSTRAINT chk_coupon_discount_type CHECK (discount_type IN ('percentage', 'fixed_amount'))
);

CREATE UNIQUE INDEX uq_coupon_code ON shop.coupon (code) WHERE deleted_at IS NULL;

-- shop_order، نه order -- به دلیل بالا. آدرس ارسال مسطح (Flatten) روی خود سفارش
-- ذخیره می‌شود، نه یک جدول Address جدا -- طبق این فاز آدرس فقط برای همین سفارش
-- لازم است، نه یک دفترچه‌آدرس قابل‌استفاده‌ی مجدد (آن موضوع فاز بعدی است اگر لازم شد).
CREATE TABLE shop.shop_order (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                   UUID NOT NULL,
    status                    VARCHAR(20) NOT NULL DEFAULT 'pending_payment',
    subtotal_amount           NUMERIC(12, 0) NOT NULL,
    discount_amount           NUMERIC(12, 0) NOT NULL DEFAULT 0,
    shipping_amount           NUMERIC(12, 0) NOT NULL DEFAULT 0,
    total_amount              NUMERIC(12, 0) NOT NULL,
    coupon_id                 UUID REFERENCES shop.coupon (id),
    shipping_method_id        UUID NOT NULL REFERENCES shop.shipping_method (id),
    shipping_recipient_name   VARCHAR(150) NOT NULL,
    shipping_phone            VARCHAR(20) NOT NULL,
    shipping_province         VARCHAR(100) NOT NULL,
    shipping_city             VARCHAR(100) NOT NULL,
    shipping_address_line     TEXT NOT NULL,
    shipping_postal_code      VARCHAR(20),
    expires_at                TIMESTAMPTZ,
    requires_manual_review    BOOLEAN NOT NULL DEFAULT false,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at                TIMESTAMPTZ,
    created_by                UUID,
    updated_by                UUID,

    CONSTRAINT chk_shop_order_status CHECK (status IN
        ('pending_payment', 'paid', 'shipped', 'delivered', 'cancelled', 'expired', 'return_requested', 'returned'))
);

CREATE INDEX idx_shop_order_user ON shop.shop_order (user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_shop_order_pending_expiry ON shop.shop_order (expires_at) WHERE status = 'pending_payment';

-- قیمت/نام لحظه‌ی خرید این‌جا Snapshot می‌شود -- گزارش مالی سفارش‌های قدیمی نباید
-- با تغییر بعدی قیمت/نام محصول عوض شود (دقیقاً همان انضباطی که Payment.amount دارد).
CREATE TABLE shop.order_item (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_order_id       UUID NOT NULL REFERENCES shop.shop_order (id),
    product_variant_id  UUID NOT NULL REFERENCES shop.product_variant (id),
    product_name        VARCHAR(200) NOT NULL,
    variant_sku         VARCHAR(60) NOT NULL,
    unit_price          NUMERIC(12, 0) NOT NULL,
    quantity            INTEGER NOT NULL,
    subtotal            NUMERIC(12, 0) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_order_item_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_order_item_order ON shop.order_item (shop_order_id);

-- هر سفارش حداکثر یک بار می‌تواند از یک کد تخفیف استفاده کند (UNIQUE روی shop_order_id)؛
-- طبق ADR-0012، این رکورد در لحظه‌ی ساخت سفارش ثبت می‌شود و با انقضا/لغو سفارش
-- برگردانده (Release) نمی‌شود -- تصمیم عمداً محافظه‌کارانه برای جلوگیری از دور زدن
-- سقف تعداد استفاده با ساخت/رهاسازی مکرر سفارش.
CREATE TABLE shop.coupon_redemption (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_id      UUID NOT NULL REFERENCES shop.coupon (id),
    shop_order_id  UUID NOT NULL UNIQUE REFERENCES shop.shop_order (id),
    user_id        UUID NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_coupon_redemption_coupon_user ON shop.coupon_redemption (coupon_id, user_id);

-- بدون هیچ تسویه‌ی مالی خودکار (نه Refund API واقعی برای Zibal طبق ADR-0011، نه
-- ماژول Wallet که هنوز ساخته نشده) -- فقط گردش‌کار درخواست/تأیید/رد/تکمیل، طبق
-- تصمیم صریح کارفرما در همین فاز. order_status_before_return برای بازگرداندن
-- دقیق وضعیت سفارش در صورت رد درخواست لازم است.
CREATE TABLE shop.return_request (
    id                           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_order_id                UUID NOT NULL REFERENCES shop.shop_order (id),
    user_id                      UUID NOT NULL,
    reason                       TEXT NOT NULL,
    status                       VARCHAR(20) NOT NULL DEFAULT 'requested',
    order_status_before_return   VARCHAR(20) NOT NULL,
    admin_note                   TEXT,
    resolved_by                  UUID,
    resolved_at                  TIMESTAMPTZ,
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_return_request_status CHECK (status IN ('requested', 'approved', 'rejected', 'completed'))
);

CREATE INDEX idx_return_request_order ON shop.return_request (shop_order_id);
