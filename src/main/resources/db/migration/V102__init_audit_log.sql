-- T-047: Módulo cross-cutting audit — agregados AuditLog (append-only) e Version.
CREATE TABLE audit_log (
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   UUID NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    actor_id       UUID,
    occurred_at    TIMESTAMPTZ NOT NULL,
    data           TEXT,
    version        BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_audit_log_aggregate ON audit_log (aggregate_type, aggregate_id, occurred_at);

CREATE TABLE version (
    id             UUID PRIMARY KEY,
    aggregate_id   UUID NOT NULL,
    version_number INT NOT NULL,
    data           TEXT,
    author_id      UUID NOT NULL,
    changed_at     TIMESTAMPTZ NOT NULL,
    reason         VARCHAR(500),
    UNIQUE (aggregate_id, version_number)
);

CREATE INDEX idx_version_aggregate ON version (aggregate_id, version_number);
