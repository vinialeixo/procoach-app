-- T-058: soft-delete do agregado Athlete (regra AGENTS.md — exclusões são
-- soft-delete, nunca DELETE físico). Adiciona as colunas de exclusão lógica
-- à tabela athletes criada na V200.
ALTER TABLE athletes
    ADD COLUMN deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
