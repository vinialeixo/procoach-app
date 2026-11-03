-- T-050: schema do agregado Diet (com.fitness.procoach.nutrition)
-- Faixa V4xx reservada para o módulo nutrition.
-- O agregado é versionado: cada alteração gera uma nova versão (v1 -> v2 -> v3)
-- com histórico encadeado via previous_version_id. Nunca sobrescreve.

CREATE TABLE diets (
    id                   UUID PRIMARY KEY,
    coach_id             UUID        NOT NULL,
    athlete_id           UUID        NOT NULL,
    objective            VARCHAR(32) NOT NULL,
    calories             DOUBLE PRECISION NOT NULL,
    protein              DOUBLE PRECISION NOT NULL,
    carbohydrates        DOUBLE PRECISION NOT NULL,
    fat                  DOUBLE PRECISION NOT NULL,
    status               VARCHAR(32) NOT NULL,
    version              INTEGER     NOT NULL,
    previous_version_id  UUID,
    change_reason        VARCHAR(255),
    created_by           UUID        NOT NULL,
    updated_by           UUID,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    read_only            BOOLEAN     NOT NULL,
    deleted              BOOLEAN     NOT NULL
);

CREATE INDEX idx_diets_athlete_version ON diets (athlete_id, version);
