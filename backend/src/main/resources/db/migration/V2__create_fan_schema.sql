-- طبق docs/database/erd-users-fan.md

CREATE TABLE fan.fan_profile (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE REFERENCES users.app_user (id),
    membership_number   VARCHAR(20),
    birth_date          DATE,
    city                VARCHAR(100),
    avatar_url          TEXT,
    joined_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,
    created_by          UUID,
    updated_by          UUID
);

CREATE UNIQUE INDEX uq_fan_profile_membership_number ON fan.fan_profile (membership_number) WHERE deleted_at IS NULL AND membership_number IS NOT NULL;

-- طبق conventions.md: جدول ساخته می‌شود چون در ERD قطعی شده، اما منطق صدور/QR هنوز پیاده نشده (خارج از محدوده‌ی Phase 6)
CREATE TABLE fan.membership_card (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fan_profile_id    UUID NOT NULL REFERENCES fan.fan_profile (id),
    card_number       VARCHAR(30) NOT NULL,
    qr_payload        TEXT NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'active',
    issued_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,
    created_by        UUID,
    updated_by        UUID,

    CONSTRAINT chk_membership_card_status CHECK (status IN ('active', 'revoked'))
);

CREATE UNIQUE INDEX uq_membership_card_number ON fan.membership_card (card_number) WHERE deleted_at IS NULL;
