CREATE TABLE card (
    id                  UUID         PRIMARY KEY,
    -- eventId of the card production message: processing the same message twice can't create two cards.
    source_event_id     UUID         NOT NULL CONSTRAINT uk_card_source_event UNIQUE,
    -- One card per customer (for now).
    customer_id         UUID         NOT NULL CONSTRAINT uk_card_customer UNIQUE,
    holder_name         VARCHAR(150) NOT NULL,
    operator            VARCHAR(20)  NOT NULL CHECK (operator IN ('VISA', 'MASTERCARD', 'ELO')),
    -- AES-256-GCM ciphertext (base64), see CardCipher. Never stored in clear.
    number_encrypted    VARCHAR(255) NOT NULL,
    masked_number       VARCHAR(19)  NOT NULL,
    expiry_encrypted    VARCHAR(255) NOT NULL,
    cvv_encrypted       VARCHAR(255) NOT NULL,
    status              VARCHAR(20)  NOT NULL CHECK (status IN ('ATIVO', 'BLOQUEADO', 'CANCELADO')),
    -- Snapshot of the rpe_catalog product at issue time.
    product_id          UUID         NOT NULL,
    product_name        VARCHAR(100) NOT NULL,
    product_description VARCHAR(500),
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL
);
