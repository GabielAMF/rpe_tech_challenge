CREATE TABLE customer (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    -- 11 upper-case letters/digits, formatting removed by the application (Cpf). Never updated.
    cpf         VARCHAR(11)  NOT NULL CONSTRAINT uk_customer_cpf UNIQUE,
    birth_date  DATE         NOT NULL,
    status      VARCHAR(20)  NOT NULL CHECK (status IN ('ATIVO', 'BLOQUEADO', 'CANCELADO')),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);
