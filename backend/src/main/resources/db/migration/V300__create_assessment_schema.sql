-- T-049: schema do agregado Assessment (com.fitness.procoach.assessment)
-- Faixa V3xx reservada para o módulo assessment.
-- O agregado é imutável: cada avaliação é um novo registro, nunca sobrescrito.

CREATE TABLE assessments (
    id                   UUID PRIMARY KEY,
    athlete_id           UUID        NOT NULL,
    coach_id             UUID        NOT NULL,
    assessment_date      DATE        NOT NULL,
    weight_kg            DECIMAL(5,2) NOT NULL,
    height_cm            DECIMAL(5,2) NOT NULL,
    body_fat_percentage  DECIMAL(5,2),
    muscle_mass_kg       DECIMAL(5,2),
    waist_cm             DECIMAL(5,2),
    hip_cm               DECIMAL(5,2),
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    version              BIGINT      NOT NULL
);

CREATE INDEX idx_assessments_athlete_date ON assessments (athlete_id, assessment_date);
