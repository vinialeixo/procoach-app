-- T-058: schema do agregado Coach (com.fitness.procoach.coach).
-- User "1" --> "1" Coach : is (UML seção 4/12.1). O Coach referencia o User
-- autenticável por user_id (subject do JWT) e é o dono da carteira de atletas.
CREATE TABLE IF NOT EXISTS coaches (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_coaches_user_id ON coaches (user_id);
