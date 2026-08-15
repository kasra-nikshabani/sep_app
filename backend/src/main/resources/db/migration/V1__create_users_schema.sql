-- طبق docs/database/erd-users-fan.md و docs/database/conventions.md
-- Schema این ماژول توسط spring.flyway.schemas (application.yml) از قبل ساخته می‌شود.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users.app_user (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_subject  UUID NOT NULL,
    national_code     VARCHAR(10) NOT NULL,
    phone_number      VARCHAR(15) NOT NULL,
    display_name      VARCHAR(150),
    status            VARCHAR(20) NOT NULL DEFAULT 'active',
    source            VARCHAR(20) NOT NULL DEFAULT 'mobile_app',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,
    created_by        UUID,
    updated_by        UUID,

    CONSTRAINT chk_app_user_status CHECK (status IN ('active', 'suspended', 'banned')),
    CONSTRAINT chk_app_user_source CHECK (source IN ('mobile_app', 'admin_created', 'django_jit'))
);

-- Unique فقط روی رکوردهای فعال (Soft-delete-aware) — یک کد ملی حذف‌شده نباید مانع ثبت‌نام مجدد شود
CREATE UNIQUE INDEX uq_app_user_keycloak_subject ON users.app_user (keycloak_subject) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_app_user_national_code ON users.app_user (national_code) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_app_user_phone_number ON users.app_user (phone_number) WHERE deleted_at IS NULL;
