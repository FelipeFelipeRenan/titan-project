CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL, -- ex: "ACCOUNT"
    aggregate_id VARCHAR(255) NOT NULL,   -- ex: ID da conta (Alice)
    type VARCHAR(255) NOT NULL,           -- ex: "TRANSFER_CREATED"
    payload JSONB NOT NULL,               -- O corpo do evento (dados da transf)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE       -- Para sabermos se o Kafka já pegou
);

-- Um índice para ajudar o worker que vai ler os não processados depois
CREATE INDEX idx_outbox_unprocessed ON outbox_events(created_at) WHERE processed = FALSE;