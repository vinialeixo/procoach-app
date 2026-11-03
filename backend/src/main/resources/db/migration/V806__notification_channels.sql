-- T-071: canais externos do aviso ao atleta (e-mail e push), preferências e rastro de entrega.
CREATE TABLE notification_preference (
    athlete_id     UUID PRIMARY KEY,
    email_enabled  BOOLEAN     NOT NULL DEFAULT TRUE,
    push_enabled   BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE TABLE notification_device (
    id            UUID PRIMARY KEY,
    athlete_id    UUID         NOT NULL,
    token         VARCHAR(512) NOT NULL UNIQUE,
    platform      VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    last_seen_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_notification_device_athlete ON notification_device (athlete_id);

CREATE TABLE notification_delivery (
    id               UUID PRIMARY KEY,
    notification_id  UUID         NOT NULL REFERENCES notification (id),
    channel          VARCHAR(20)  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    attempts         INTEGER      NOT NULL DEFAULT 0,
    last_error       VARCHAR(500),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_notification_delivery UNIQUE (notification_id, channel)
);

CREATE INDEX idx_notification_delivery_retry ON notification_delivery (status, attempts);
