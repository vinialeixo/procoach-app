-- T-067: cada sessão de treino pertence a uma versão do plano. Criar a v2 do plano copia as
-- sessões da v1 (que ficam intactas); ativar a v2 encerra a v1 e publica as sessões da v2.
ALTER TABLE training_training
    ADD COLUMN plan_id UUID REFERENCES training_plan (id);

CREATE INDEX idx_training_plan_id ON training_training (plan_id, deleted);

-- Sessões que já existiam: ligadas ao plano ativo da periodização (ou, sem ativo, à versão mais recente).
UPDATE training_training t
   SET plan_id = (SELECT p.id
                    FROM training_plan p
                   WHERE p.periodization_id = t.periodization_id
                     AND p.deleted = false
                   ORDER BY CASE p.status WHEN 'ACTIVE' THEN 0 ELSE 1 END, p.version DESC
                   LIMIT 1);
