-- T-008/T-010: MFA (TOTP) e vínculo user -> athlete (claim do JWT usado no
-- isolamento de dados da US-038). athlete_id é referência por UUID ao agregado
-- Athlete, sem FK física para não acoplar os bounded contexts.
ALTER TABLE app_user
    ADD COLUMN mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN mfa_secret  VARCHAR(64),
    ADD COLUMN athlete_id  UUID;
