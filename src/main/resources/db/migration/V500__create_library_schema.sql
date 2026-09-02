-- T-036/T-052: Schema da Biblioteca de Exercícios (módulo library)
-- Faixa V5xx reservada para o módulo library.

CREATE TABLE library_exercise (
    id          UUID        PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    equipment   VARCHAR(50)  NOT NULL,
    execution   TEXT         NOT NULL,
    created_by  UUID         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE library_exercise_muscle_group (
    exercise_id UUID        NOT NULL REFERENCES library_exercise (id) ON DELETE CASCADE,
    muscle_group VARCHAR(50) NOT NULL,
    PRIMARY KEY (exercise_id, muscle_group)
);

CREATE TABLE library_exercise_common_error (
    exercise_id  UUID        NOT NULL REFERENCES library_exercise (id) ON DELETE CASCADE,
    common_error VARCHAR(1000) NOT NULL
);

CREATE TABLE library_exercise_progression (
    exercise_id UUID         NOT NULL REFERENCES library_exercise (id) ON DELETE CASCADE,
    progression VARCHAR(1000) NOT NULL
);

CREATE TABLE library_exercise_regression (
    exercise_id UUID         NOT NULL REFERENCES library_exercise (id) ON DELETE CASCADE,
    regression  VARCHAR(1000) NOT NULL
);

CREATE TABLE library_exercise_contraindication (
    exercise_id     UUID         NOT NULL REFERENCES library_exercise (id) ON DELETE CASCADE,
    contraindication VARCHAR(1000) NOT NULL
);

CREATE TABLE library_exercise_reference (
    exercise_id UUID         NOT NULL REFERENCES library_exercise (id) ON DELETE CASCADE,
    authors     VARCHAR(500) NOT NULL,
    title       VARCHAR(1000) NOT NULL,
    source      VARCHAR(500) NOT NULL,
    year        INTEGER
);

CREATE INDEX idx_library_exercise_name ON library_exercise (name);
CREATE INDEX idx_library_exercise_deleted ON library_exercise (deleted);
