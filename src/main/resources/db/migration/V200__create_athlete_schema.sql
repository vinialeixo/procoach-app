-- T-017/T-048: schema do agregado Athlete (com.procoach.core.athlete)
CREATE TABLE athletes (
    id          UUID PRIMARY KEY,
    coach_id    UUID NOT NULL,
    name        VARCHAR(150) NOT NULL,
    sex         VARCHAR(10)  NOT NULL,
    birth_date  DATE         NOT NULL,
    height      DECIMAL(5,2) NOT NULL,
    weight      DECIMAL(5,2) NOT NULL,
    goal        VARCHAR(30)  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
