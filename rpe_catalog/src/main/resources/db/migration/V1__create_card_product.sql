CREATE TABLE card_product (
    id          UUID         PRIMARY KEY,
    -- Stored trimmed and upper-cased by the application (Product.normalizeName).
    name        VARCHAR(100) NOT NULL CONSTRAINT uk_card_product_name UNIQUE,
    description VARCHAR(500),
    status      VARCHAR(20)  NOT NULL CHECK (status IN ('ATIVO', 'CANCELADO')),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);
