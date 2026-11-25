-- T-076: remoção de token de push passa a ser lógica. O registro do dispositivo
-- nunca é apagado fisicamente; o cadastro repetido do mesmo token o reativa.
ALTER TABLE notification_device
    ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_notification_device_active ON notification_device (athlete_id)
    WHERE deleted_at IS NULL;
