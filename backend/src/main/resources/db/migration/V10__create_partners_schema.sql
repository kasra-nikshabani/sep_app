-- طبق docs/adr/0015-notifications-and-partners.md (Phase 13) و rbac-matrix.md:
-- «مدیریت سیستم Partner: فقط admin (🛠)؛ خودِ Partner فقط از طریق API/Webhook مجزا،
-- نه Admin Panel، به داده‌ی خودش دسترسی دارد» -- api_key_hash همان مسیر جداست.

CREATE TABLE partners.partner (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    slug            VARCHAR(200) NOT NULL,
    contact_email   VARCHAR(255),
    contact_phone   VARCHAR(20),
    -- SHA-256 خود کلید (نه bcrypt) عمداً -- کلید یک راز پرآنتروپی تولیدشده است، نه یک
    -- رمز عبور کم‌آنتروپی کاربر؛ هش قطعی و Indexable لازم است تا بشود مستقیم با آن
    -- Partner را از روی کلید ارسالی پیدا کرد. خود کلید فقط یک‌بار، در لحظه‌ی ساخت، نمایش
    -- داده می‌شود و هرگز در دیتابیس ذخیره نمی‌شود.
    api_key_hash    VARCHAR(64) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    created_by      UUID,
    updated_by      UUID
);

CREATE UNIQUE INDEX uq_partner_slug ON partners.partner (slug) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_partner_api_key_hash ON partners.partner (api_key_hash) WHERE deleted_at IS NULL;
