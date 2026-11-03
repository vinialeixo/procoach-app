-- Integridade das cadeias de versão (revisão de código, lote 1).
-- Sem estas constraints, duas requisições simultâneas criam duas "v2" a partir
-- da mesma origem e a cadeia bifurca. A violação vira 409 nos handlers.

-- Dieta: uma versão N por atleta (também barra uma segunda v1 para o mesmo atleta).
ALTER TABLE diets
    ADD CONSTRAINT uq_diets_athlete_version UNIQUE (athlete_id, version);

-- Plano de treino: cada versão tem no máximo uma sucessora (NULL = v1, pode repetir).
ALTER TABLE training_plan
    ADD CONSTRAINT uq_training_plan_previous_version UNIQUE (previous_version_id);
