-- T-051: Training module schema (Periodization, TrainingPlan, Training, ExercisePrescription)
-- Regras: soft-delete (deleted/deleted_at), sem UPDATE fisico, referencias por UUID.

CREATE TABLE IF NOT EXISTS training_periodization (
    id              UUID PRIMARY KEY,
    athlete_id      UUID NOT NULL,
    coach_id        UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted         BOOLEAN NOT NULL DEFAULT false,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_periodization_athlete ON training_periodization (athlete_id, deleted);
CREATE INDEX idx_periodization_coach   ON training_periodization (coach_id, deleted);

CREATE TABLE IF NOT EXISTS training_plan (
    id                  UUID PRIMARY KEY,
    athlete_id          UUID NOT NULL,
    coach_id            UUID NOT NULL,
    periodization_id    UUID NOT NULL REFERENCES training_periodization (id),
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    version             INT NOT NULL DEFAULT 1,
    previous_version_id UUID REFERENCES training_plan (id),
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted             BOOLEAN NOT NULL DEFAULT false,
    deleted_at          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_training_plan_athlete       ON training_plan (athlete_id, deleted);
CREATE INDEX idx_training_plan_coach         ON training_plan (coach_id, deleted);
CREATE INDEX idx_training_plan_periodization ON training_plan (periodization_id, deleted);
CREATE INDEX idx_training_plan_prev_version  ON training_plan (previous_version_id);

CREATE TABLE IF NOT EXISTS training_training (
    id               UUID PRIMARY KEY,
    periodization_id UUID NOT NULL REFERENCES training_periodization (id),
    athlete_id       UUID NOT NULL,
    coach_id         UUID NOT NULL,
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    week_number      INT NOT NULL,
    day_of_week      INT NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted          BOOLEAN NOT NULL DEFAULT false,
    deleted_at       TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_training_periodization ON training_training (periodization_id, deleted);
CREATE INDEX idx_training_athlete       ON training_training (athlete_id, deleted);
CREATE INDEX idx_training_coach         ON training_training (coach_id, deleted);

CREATE TABLE IF NOT EXISTS training_exercise_prescription (
    id               UUID PRIMARY KEY,
    training_id      UUID NOT NULL REFERENCES training_training (id),
    exercise_id      UUID,
    exercise_name    VARCHAR(255) NOT NULL,
    custom_exercise  BOOLEAN NOT NULL DEFAULT false,
    order_index      INT NOT NULL,
    sets             INT NOT NULL,
    repetitions      INT NOT NULL,
    load             NUMERIC(10, 2) NOT NULL,
    intensity_type   VARCHAR(20) NOT NULL,
    intensity_value  NUMERIC(10, 2) NOT NULL,
    rest_seconds     INT NOT NULL,
    notes            TEXT
);

CREATE INDEX idx_exercise_prescription_training ON training_exercise_prescription (training_id);
