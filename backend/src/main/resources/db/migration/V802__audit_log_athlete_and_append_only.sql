-- T-013/T-015: o audit_log passa a ser consultável por atleta e imutável no banco.
ALTER TABLE audit_log ADD COLUMN athlete_id UUID;

CREATE INDEX idx_audit_log_athlete ON audit_log (athlete_id, occurred_at);

-- Append-only de verdade: UPDATE, DELETE e TRUNCATE são rejeitados pelo banco,
-- não só pela interface do repositório.
CREATE OR REPLACE FUNCTION audit_log_reject_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_log é append-only: % não é permitido', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_log_append_only
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION audit_log_reject_change();

CREATE TRIGGER trg_audit_log_no_truncate
    BEFORE TRUNCATE ON audit_log
    FOR EACH STATEMENT EXECUTE FUNCTION audit_log_reject_change();
