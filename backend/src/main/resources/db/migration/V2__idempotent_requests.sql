-- Newtonite Operations: Idempotency Schema Migration (V2)
-- Guarantees safe retries, payload fingerprint validation, and concurrency protection

CREATE TABLE IF NOT EXISTS idempotent_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    work_item_id BIGINT NOT NULL,
    response_status INT NOT NULL DEFAULT 201,
    response_body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_idempotency_key_user UNIQUE (user_id, idempotency_key),
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_idempotent_requests_lookup ON idempotent_requests (user_id, idempotency_key);
CREATE INDEX idx_idempotent_requests_expires ON idempotent_requests (expires_at);
