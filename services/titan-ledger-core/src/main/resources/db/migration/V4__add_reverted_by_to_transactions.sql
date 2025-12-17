ALTER TABLE transactions 
ADD COLUMN reverted_by_transaction_id UUID;

-- Opcional: Adicionar uma Foreign Key para garantir integridade
-- (Só descomente se quiser garantir que o ID aponte para uma transação real)
ALTER TABLE transactions 
ADD CONSTRAINT fk_reverted_by 
FOREIGN KEY (reverted_by_transaction_id) REFERENCES transactions(id);