-- Transactional outbox: events saved in the same transaction as the change that raised them, then published to SQS
-- by OutboxRelay. Delivery is at least once; consumers de-duplicate on event_id.
CREATE TABLE outbox_event (
    id                UUID         PRIMARY KEY,
    -- The eventId inside the payload: stays the same on every retry, so consumers can de-duplicate.
    event_id          UUID         NOT NULL CONSTRAINT uk_outbox_event_event_id UNIQUE,
    event_type        VARCHAR(50)  NOT NULL,
    -- The customer the event is about.
    aggregate_id      UUID         NOT NULL,
    -- AES-256-GCM ciphertext (base64) of the JSON message: it carries personal data. Cleared once SENT.
    payload_encrypted TEXT,
    status            VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts          INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at   TIMESTAMPTZ  NOT NULL,
    -- Exception class and message of the last failed send; never the payload.
    last_error        VARCHAR(500),
    sent_at           TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_outbox_event_payload CHECK (status = 'SENT' OR payload_encrypted IS NOT NULL)
);

-- The relay's query: due PENDING events, oldest first.
CREATE INDEX idx_outbox_event_pending ON outbox_event (next_attempt_at) WHERE status = 'PENDING';
CREATE INDEX idx_outbox_event_aggregate ON outbox_event (aggregate_id);
