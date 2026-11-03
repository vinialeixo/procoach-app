-- T-066: exercício da biblioteca versionado. Editar cria uma nova linha (v2, v3…)
-- e marca a anterior como superseded, em vez de sobrescrever: treinos já prescritos
-- continuam apontando para a versão que o coach usou.
ALTER TABLE library_exercise
    ADD COLUMN version             INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN previous_version_id UUID REFERENCES library_exercise (id),
    ADD COLUMN superseded          BOOLEAN NOT NULL DEFAULT FALSE;

-- Cada versão tem no máximo uma sucessora: duas edições simultâneas não bifurcam a cadeia.
ALTER TABLE library_exercise
    ADD CONSTRAINT uq_library_exercise_previous_version UNIQUE (previous_version_id);
