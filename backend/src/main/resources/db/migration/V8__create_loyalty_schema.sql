-- طبق docs/database/erd-loyalty.md و ADR-0014 (Loyalty، Phase 12)
-- طبق بند ۱۶ بریف: سطوح عضویت و منطق امتیازدهی Configurable هستند، نه Hardcode --
-- level و earning_rule جدول‌های عادی‌اند که ادمین مدیریت می‌کند، نه Enum ثابت در کد.

CREATE TABLE loyalty.level (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    min_points  INTEGER NOT NULL,
    benefits    TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    created_by  UUID,
    updated_by  UUID,

    CONSTRAINT chk_level_min_points_non_negative CHECK (min_points >= 0)
);

CREATE UNIQUE INDEX uq_level_min_points ON loyalty.level (min_points) WHERE deleted_at IS NULL;

-- حداکثر یک قانون فعال به‌ازای هر منبع رویداد -- جلوگیری از ابهام در محاسبه‌ی امتیاز.
CREATE TABLE loyalty.earning_rule (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_type        VARCHAR(40) NOT NULL,
    points_per_amount  NUMERIC(14, 2) NOT NULL,
    is_active          BOOLEAN NOT NULL DEFAULT true,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at         TIMESTAMPTZ,
    created_by         UUID,
    updated_by         UUID,

    CONSTRAINT chk_earning_rule_source_type CHECK
        (source_type IN ('shop_order', 'ticket_purchase', 'football_ticket_purchase')),
    CONSTRAINT chk_earning_rule_points_per_amount_positive CHECK (points_per_amount > 0)
);

CREATE UNIQUE INDEX uq_earning_rule_active_source ON loyalty.earning_rule (source_type)
    WHERE is_active = true AND deleted_at IS NULL;

-- user_id فقط UUID عمومی است (-> users.app_user.id)، بدون FK فیزیکی Cross-schema (conventions.md).
-- lifetime_points هرگز با خرج‌کردن امتیاز کم نمی‌شود -- سطح عضویت بر همین مبنا محاسبه می‌شود
-- (نه روی points_balance) تا خرج‌کردن امتیاز باعث افت غیرمنصفانه‌ی سطح نشود (ADR-0014).
CREATE TABLE loyalty.account (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL UNIQUE,
    points_balance   INTEGER NOT NULL DEFAULT 0,
    lifetime_points  INTEGER NOT NULL DEFAULT 0,
    level_id         UUID REFERENCES loyalty.level (id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ,
    created_by       UUID,
    updated_by       UUID,

    CONSTRAINT chk_account_points_balance_non_negative CHECK (points_balance >= 0),
    CONSTRAINT chk_account_lifetime_points_non_negative CHECK (lifetime_points >= 0)
);

-- Append-only (دفترکل امتیاز) -- source_type/source_reference_id برای Idempotency رویدادهای
-- بیرونی (خرید Shop/تئاتر/فوتبال) استفاده می‌شود؛ برای redeem/adjust همیشه NULL می‌ماند
-- (آن دو رویدادی نیستند که ممکن است دوباره تحویل داده شوند).
CREATE TABLE loyalty.transaction (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id           UUID NOT NULL REFERENCES loyalty.account (id),
    type                 VARCHAR(20) NOT NULL,
    points               INTEGER NOT NULL,
    source_type          VARCHAR(40),
    source_reference_id  UUID,
    description          TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           UUID,

    CONSTRAINT chk_transaction_type CHECK (type IN ('earn', 'redeem', 'adjust'))
);

CREATE UNIQUE INDEX uq_transaction_source ON loyalty.transaction (source_type, source_reference_id)
    WHERE source_reference_id IS NOT NULL;
CREATE INDEX idx_transaction_account ON loyalty.transaction (account_id, created_at DESC);

CREATE TABLE loyalty.reward (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    points_cost     INTEGER NOT NULL,
    stock_quantity  INTEGER,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    created_by      UUID,
    updated_by      UUID,

    CONSTRAINT chk_reward_points_cost_positive CHECK (points_cost > 0),
    CONSTRAINT chk_reward_stock_non_negative CHECK (stock_quantity IS NULL OR stock_quantity >= 0)
);

-- بدون common.BaseEntity -- یک رکورد گردش‌کار مثل shop.return_request، نه داده‌ی کاتالوگ.
CREATE TABLE loyalty.reward_redemption (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id        UUID NOT NULL REFERENCES loyalty.account (id),
    reward_id         UUID NOT NULL REFERENCES loyalty.reward (id),
    points_spent      INTEGER NOT NULL,
    redemption_code   VARCHAR(40) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'requested',
    fulfilled_by      UUID,
    fulfilled_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_redemption_status CHECK (status IN ('requested', 'fulfilled', 'cancelled'))
);

CREATE UNIQUE INDEX uq_redemption_code ON loyalty.reward_redemption (redemption_code);
CREATE INDEX idx_redemption_account ON loyalty.reward_redemption (account_id);

-- Cursor برای Polling رویدادهای بیرونی (مثلاً بلیط فوتبال از Django) -- یک ردیف به‌ازای هر منبع،
-- تا بعد از Restart Backend هم Polling دقیقاً از همان‌جا ادامه پیدا کند (ADR-0014).
CREATE TABLE loyalty.external_poll_cursor (
    source          VARCHAR(60) PRIMARY KEY,
    last_cursor_at  TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
