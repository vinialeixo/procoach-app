-- Convite do atleta (primeira credencial): o coach gera um link de uso único e o
-- atleta define a própria senha. Só o hash SHA-256 do token é guardado.
CREATE TABLE athlete_invite (
    id          UUID PRIMARY KEY,
    athlete_id  UUID         NOT NULL,
    coach_id    UUID         NOT NULL,
    email       VARCHAR(255) NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_athlete_invite_athlete ON athlete_invite (athlete_id);

-- Um atleta tem no máximo uma credencial (vínculo app_user -> athlete).
CREATE UNIQUE INDEX uq_app_user_athlete ON app_user (athlete_id) WHERE athlete_id IS NOT NULL;
