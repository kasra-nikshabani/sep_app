-- طبق docs/adr/0015-notifications-and-partners.md (Phase 13)

-- دفترکل هر تلاش ارسال (موفق یا ناموفق) -- برای Audit/عیب‌یابی، طبق بند ۷/۸ بریف («Logging» جزء
-- الزامات هر Integration است). status=failed هرگز باعث Rollback جریان اصلی (مثلاً ثبت سفارش) نمی‌شود --
-- ارسال Notification همیشه Best-effort است (ADR-0015).
CREATE TABLE notifications.notification_log (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel        VARCHAR(20) NOT NULL,
    recipient      VARCHAR(255) NOT NULL,
    subject        VARCHAR(255),
    content        TEXT NOT NULL,
    status         VARCHAR(20) NOT NULL,
    provider_name  VARCHAR(40) NOT NULL,
    error_message  TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ,
    created_by     UUID,
    updated_by     UUID,

    CONSTRAINT chk_notification_channel CHECK (channel IN ('sms', 'email', 'push')),
    CONSTRAINT chk_notification_status CHECK (status IN ('sent', 'failed'))
);

CREATE INDEX idx_notification_log_recipient ON notifications.notification_log (recipient, created_at DESC);

-- ثبت Device Token برای Push -- فقط زیرساخت ثبت/لغو، چون هیچ Provider واقعی Push
-- (FCM/APNs) در این فاز متصل نیست (بدون اپ موبایل واقعی هنوز، Phase 15). یک کاربر
-- می‌تواند چند دستگاه/Token فعال داشته باشد.
CREATE TABLE notifications.device_token (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    token       VARCHAR(500) NOT NULL,
    platform    VARCHAR(20) NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    created_by  UUID,
    updated_by  UUID,

    CONSTRAINT chk_device_token_platform CHECK (platform IN ('ios', 'android', 'web'))
);

CREATE UNIQUE INDEX uq_device_token ON notifications.device_token (token) WHERE deleted_at IS NULL;
CREATE INDEX idx_device_token_user ON notifications.device_token (user_id) WHERE deleted_at IS NULL;
