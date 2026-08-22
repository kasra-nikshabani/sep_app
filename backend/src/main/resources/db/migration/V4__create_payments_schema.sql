-- طبق ADR-0011: ماژول Payment مستقل از Order (نه FK فیزیکی به ticketing/orders -- طبق
-- conventions.md قانون No Cross-schema JOIN؛ reference_id فقط یک UUID عمومی است).

CREATE TABLE payments.payment (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purpose         VARCHAR(30) NOT NULL,
    reference_id    UUID NOT NULL,
    user_id         UUID NOT NULL,
    provider        VARCHAR(20) NOT NULL,
    track_id        VARCHAR(100),
    redirect_url    TEXT,
    amount          NUMERIC(14, 0) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'pending',
    raw_response    JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    created_by      UUID,
    updated_by      UUID,

    CONSTRAINT chk_payment_purpose CHECK (purpose IN ('ticket_purchase', 'wallet_topup')),
    CONSTRAINT chk_payment_provider CHECK (provider IN ('zibal', 'fake')),
    CONSTRAINT chk_payment_status CHECK (status IN ('pending', 'paid', 'failed'))
);

-- Idempotency طبق ADR-0011: فقط یک Payment در وضعیت pending به‌ازای هر (purpose, reference_id)
CREATE UNIQUE INDEX uq_payment_pending_reference ON payments.payment (purpose, reference_id) WHERE status = 'pending';

CREATE UNIQUE INDEX uq_payment_track_id ON payments.payment (track_id) WHERE track_id IS NOT NULL;
CREATE INDEX idx_payment_user ON payments.payment (user_id);
CREATE INDEX idx_payment_reference ON payments.payment (purpose, reference_id);
