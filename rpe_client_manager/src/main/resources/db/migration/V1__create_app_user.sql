CREATE TABLE app_user (
    id            UUID         PRIMARY KEY,
    -- Stored trimmed and lower-cased by the application (Username).
    username      VARCHAR(50)  NOT NULL CONSTRAINT uk_app_user_username UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);
