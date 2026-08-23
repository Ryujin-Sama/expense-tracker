CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    base_currency   VARCHAR(3)   NOT NULL DEFAULT 'USD',
    role            VARCHAR(50)  NOT NULL DEFAULT 'ROLE_USER',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Explicit named index on top of the unique constraint's implicit index isn't
-- needed - MySQL indexes unique columns automatically. Kept here as a comment
-- so the next migration author doesn't wonder why there isn't one.