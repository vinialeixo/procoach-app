-- T-086: catálogo de alimentos (TACO) com macros por 100 g, porção comestível,
-- medida caseira e estado padrão (RAW/AS_SERVED).
-- Faixa V4xx reservada para o módulo nutrition.
--
-- Alimentos globais (TACO/USDA) não têm coach_id; alimentos CUSTOM pertencem ao
-- coach que os criou (coach_id NOT NULL) e exigem coach_approved = true.

CREATE TABLE foods (
    id                      UUID PRIMARY KEY,
    taco_id                 VARCHAR(100) UNIQUE, -- referência estável à TACO; null para CUSTOM
    name                    VARCHAR(200) NOT NULL,
    category                VARCHAR(100),
    calories                DOUBLE PRECISION NOT NULL,
    protein                 DOUBLE PRECISION NOT NULL,
    carbohydrates           DOUBLE PRECISION NOT NULL,
    fat                     DOUBLE PRECISION NOT NULL,
    edible_portion_percent  DOUBLE PRECISION NOT NULL DEFAULT 100,
    household_measure       VARCHAR(100),
    household_measure_grams DOUBLE PRECISION,
    source                  VARCHAR(16) NOT NULL, -- TACO, USDA, CUSTOM
    state_default           VARCHAR(16) NOT NULL, -- RAW, AS_SERVED
    coach_id                UUID,                  -- dono do alimento CUSTOM
    coach_approved          BOOLEAN NOT NULL DEFAULT false,
    deleted                 BOOLEAN NOT NULL DEFAULT false,
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_foods_source_coach CHECK (
        (source = 'CUSTOM' AND coach_id IS NOT NULL AND coach_approved = true)
        OR (source <> 'CUSTOM' AND coach_id IS NULL)
    )
);

CREATE INDEX idx_foods_coach ON foods (coach_id);
CREATE INDEX idx_foods_category ON foods (category);
CREATE INDEX idx_foods_name_lower ON foods (lower(name));

-- Seed inicial e idempotente dos alimentos in natura prioritários da TACO.
-- ON CONFLICT (taco_id) DO NOTHING garante que reexecuções não duplicam linhas.
INSERT INTO foods (id, taco_id, name, category, calories, protein, carbohydrates, fat,
                   edible_portion_percent, household_measure, household_measure_grams,
                   source, state_default, coach_id, coach_approved, deleted, created_at, updated_at)
VALUES
    ('f0000000-0000-4000-8000-000000008001', 'taco:frango-peito-sem-pele-cru',
     'Peito de frango, sem pele, cru', 'Carnes e derivados',
     119.0, 21.5, 0.0, 3.0, 100.0, '1 filé médio', 100.0, 'TACO', 'RAW', NULL, false, false, NOW(), NOW()),
    ('f0000000-0000-4000-8000-000000008002', 'taco:arroz-tipo-1-cru',
     'Arroz, tipo 1, cru', 'Cereais e derivados',
     358.0, 7.2, 78.8, 0.3, 100.0, '1 colher de sopa', 15.0, 'TACO', 'RAW', NULL, false, false, NOW(), NOW()),
    ('f0000000-0000-4000-8000-000000008003', 'taco:ovo-de-galinha-inteiro-cru',
     'Ovo, de galinha, inteiro, cru', 'Ovos',
     143.0, 13.0, 1.6, 9.5, 100.0, '1 unidade', 50.0, 'TACO', 'RAW', NULL, false, false, NOW(), NOW()),
    ('f0000000-0000-4000-8000-000000008004', 'taco:aveia-flocos-crua',
     'Aveia, flocos, crua', 'Cereais e derivados',
     394.0, 13.9, 66.6, 8.5, 100.0, '1 colher de sopa', 15.0, 'TACO', 'RAW', NULL, false, false, NOW(), NOW()),
    ('f0000000-0000-4000-8000-000000008005', 'taco:batata-doce-crua',
     'Batata-doce, crua', 'Verduras, hortaliças e derivados',
     77.0, 1.3, 18.4, 0.1, 100.0, '1 unidade média', 150.0, 'TACO', 'RAW', NULL, false, false, NOW(), NOW())
ON CONFLICT (taco_id) DO NOTHING;
