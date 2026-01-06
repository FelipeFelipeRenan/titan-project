-- V5__clean_test_data.sql

-- 1. Remove lançamentos no Ledger (Entries) das contas de teste
-- É necessário remover primeiro por causa da Foreign Key
DELETE FROM ledger_entries
WHERE account_id IN (
    SELECT id FROM accounts WHERE client_id LIKE 'user_%_test'
);

-- 2. Remove eventos da Outbox (Tabela criada no V3)
-- Fazemos o CAST(id AS VARCHAR) pois no V3 o aggregate_id é VARCHAR e o id da conta é UUID
DELETE FROM outbox_events
WHERE aggregate_type = 'ACCOUNT'
  AND aggregate_id IN (
    SELECT CAST(id AS VARCHAR) FROM accounts WHERE client_id LIKE 'user_%_test'
);

-- 3. Remove as Contas de Teste
DELETE FROM accounts
WHERE client_id LIKE 'user_%_test';

-- 4. Limpeza de Transações Órfãs (Opcional, mas recomendado)
-- Remove transações que não têm mais nenhuma entrada no ledger associada
DELETE FROM transactions
WHERE id NOT IN (
    SELECT DISTINCT transaction_id FROM ledger_entries
);