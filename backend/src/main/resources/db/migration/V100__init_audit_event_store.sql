-- T-005: Event store base (Event Sourcing seletivo) — append-only.
CREATE TABLE stored_event (
    id            BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id  UUID NOT NULL,
    event_type    VARCHAR(100) NOT NULL,
    event_data    TEXT,
    occurred_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_stored_event_aggregate ON stored_event (aggregate_type, aggregate_id, id);
