CREATE TABLE idempotency_keys (
    key VARCHAR(255) NOT NULL PRIMARY KEY,
    response_status INT NOT NULL,
    response_body JSONB, -- Se usar Postgres antigo mude para TEXT
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);