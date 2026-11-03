-- Avisos ao atleta (in-app): o coach edita treino ou aprova dieta e o atleta é avisado.
CREATE TABLE notification (
    id             UUID PRIMARY KEY,
    athlete_id     UUID         NOT NULL,
    type           VARCHAR(50)  NOT NULL,
    message        VARCHAR(500) NOT NULL,
    reference_type VARCHAR(50),
    reference_id   UUID,
    created_at     TIMESTAMPTZ  NOT NULL,
    read_at        TIMESTAMPTZ
);

CREATE INDEX idx_notification_athlete ON notification (athlete_id, created_at);
