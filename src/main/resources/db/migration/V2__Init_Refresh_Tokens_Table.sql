CREATE TABLE refresh_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP    NOT NULL,
    is_revoked  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Storing token_hash as a SHA-256 digest of the raw opaque refresh token,
-- never the raw value - if this table ever leaks, the tokens in it aren't
-- directly usable. The raw token is generated and handed to the client once
-- in RefreshTokenService (Day 5); only its hash ever touches the database.
--
-- SHA-256, not BCrypt, despite the FDS listing "BCrypt/SHA-256" as
-- interchangeable: BCrypt salts each hash randomly, so the same input
-- produces a different output every call, which makes an equality lookup
-- (WHERE token_hash = ?) impossible without iterating every stored row.
-- SHA-256 is deterministic, which is what the unique index actually needs.

-- Composite, not single-column: both repository queries filter on
-- user_id AND is_revoked together, so a user_id-only index would still force
-- MySQL to scan and filter is_revoked in memory once this table has any real
-- volume.
CREATE INDEX idx_refresh_tokens_user_id_revoked ON refresh_tokens (user_id, is_revoked);

-- No cleanup job exists yet, but "DELETE FROM refresh_tokens WHERE
-- expiry_date < NOW()" is inevitable once this table has traffic - indexing
-- now avoids a production ALTER TABLE later.
CREATE INDEX idx_refresh_tokens_expiry_date ON refresh_tokens (expiry_date);