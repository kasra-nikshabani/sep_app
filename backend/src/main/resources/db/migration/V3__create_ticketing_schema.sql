-- طبق docs/database/erd-ticketing.md و ADR-0006 (موتور داخلی بلیط تئاتر)

CREATE TABLE ticketing.venue (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    city        VARCHAR(100),
    address     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    created_by  UUID,
    updated_by  UUID
);

CREATE TABLE ticketing.venue_seat (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id     UUID NOT NULL REFERENCES ticketing.venue (id),
    section      VARCHAR(100) NOT NULL,
    row_label    VARCHAR(20) NOT NULL,
    seat_number  VARCHAR(20) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_venue_seat_position ON ticketing.venue_seat (venue_id, section, row_label, seat_number) WHERE deleted_at IS NULL;

CREATE TABLE ticketing.event (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id          UUID NOT NULL REFERENCES ticketing.venue (id),
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    starts_at         TIMESTAMPTZ NOT NULL,
    duration_minutes  INTEGER NOT NULL DEFAULT 120,
    status            VARCHAR(20) NOT NULL DEFAULT 'draft',
    base_price        NUMERIC(12, 0) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,
    created_by        UUID,
    updated_by        UUID,

    CONSTRAINT chk_event_status CHECK (status IN ('draft', 'published', 'cancelled', 'completed'))
);

CREATE INDEX idx_event_venue_starts_at ON ticketing.event (venue_id, starts_at) WHERE deleted_at IS NULL;

-- یک ردیف به‌ازای هر (Event, VenueSeat) -- طبق ERD، وضعیت صندلی مخصوص همین اجراست
CREATE TABLE ticketing.event_seat (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID NOT NULL REFERENCES ticketing.event (id),
    venue_seat_id   UUID NOT NULL REFERENCES ticketing.venue_seat (id),
    status          VARCHAR(20) NOT NULL DEFAULT 'available',
    price           NUMERIC(12, 0) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_event_seat_status CHECK (status IN ('available', 'held', 'sold'))
);

CREATE UNIQUE INDEX uq_event_seat ON ticketing.event_seat (event_id, venue_seat_id);
CREATE INDEX idx_event_seat_status ON ticketing.event_seat (event_id, status);

CREATE TABLE ticketing.reservation (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_seat_id  UUID NOT NULL UNIQUE REFERENCES ticketing.event_seat (id),
    user_id        UUID NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reservation_expires_at ON ticketing.reservation (expires_at);

CREATE TABLE ticketing.ticket (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_seat_id   UUID NOT NULL UNIQUE REFERENCES ticketing.event_seat (id),
    user_id         UUID NOT NULL,
    ticket_number   VARCHAR(30) NOT NULL,
    qr_payload      TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'valid',
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    created_by      UUID,
    updated_by      UUID,

    CONSTRAINT chk_ticket_status CHECK (status IN ('valid', 'used', 'refunded'))
);

CREATE UNIQUE INDEX uq_ticket_number ON ticketing.ticket (ticket_number) WHERE deleted_at IS NULL;
CREATE INDEX idx_ticket_user ON ticketing.ticket (user_id);
