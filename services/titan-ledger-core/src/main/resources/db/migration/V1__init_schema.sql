-- Habilita extensão para gerar UUIDs randomicos (v4)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. TABELA DE CONTAS (ACCOUNTS)
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id VARCHAR(255) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uc_accounts_client_currency UNIQUE (client_id, currency)
);

-- 2. TABELA DE TRANSAÇÕES (TRANSACTIONS)
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    correlation_id VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. TABELA DO LIVRO-RAZÃO (LEDGER_ENTRIES)
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id UUID NOT NULL,
    account_id UUID NOT NULL,
    operation_type VARCHAR(10) NOT NULL CHECK (operation_type IN ('DEBIT', 'CREDIT')),
    amount DECIMAL(19, 4) NOT NULL CHECK (amount > 0),
    balance_snapshot DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ledger_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT fk_ledger_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

-- Indexes para performance
CREATE INDEX idx_ledger_account ON ledger_entries(account_id);
CREATE INDEX idx_ledger_transaction ON ledger_entries(transaction_id);
CREATE INDEX idx_transactions_correlation ON transactions(correlation_id);