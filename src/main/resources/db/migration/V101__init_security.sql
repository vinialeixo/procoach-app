-- T-046: Módulo de domínio security — agregados User e Role (RBAC).
CREATE TABLE app_user (
    id            UUID PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled       BOOLEAN NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    version       BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE role (
    id     UUID PRIMARY KEY,
    name   VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL
);

CREATE TABLE user_role (
    user_id UUID NOT NULL REFERENCES app_user (id),
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE role_permission (
    role_id    UUID NOT NULL REFERENCES role (id),
    permission VARCHAR(50) NOT NULL,
    PRIMARY KEY (role_id, permission)
);
