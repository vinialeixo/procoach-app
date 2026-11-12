-- T-085: tabelas de refeições (meals) e itens de refeição (meal_items)
-- Faixa V4xx reservada para o módulo nutrition.
-- Refeições pertencem a uma dieta (versão específica) e contêm itens com snapshot de macros.

CREATE TABLE meals (
    id                   UUID PRIMARY KEY,
    diet_id              UUID        NOT NULL REFERENCES diets(id) ON DELETE CASCADE,
    name                 VARCHAR(100) NOT NULL,
    order_index          INTEGER     NOT NULL,
    time                 VARCHAR(5), -- HH:mm
    notes                TEXT,
    target_calories      DOUBLE PRECISION,
    target_protein       DOUBLE PRECISION,
    target_carbohydrates DOUBLE PRECISION,
    target_fat           DOUBLE PRECISION
);

CREATE INDEX idx_meals_diet_order ON meals (diet_id, order_index);

CREATE TABLE meal_items (
    id                   UUID PRIMARY KEY,
    meal_id              UUID        NOT NULL REFERENCES meals(id) ON DELETE CASCADE,
    food_id              VARCHAR(100), -- referência ao catálogo (TACO/USDA), null para CUSTOM inline
    name                 VARCHAR(200) NOT NULL,
    source               VARCHAR(16) NOT NULL, -- TACO, USDA, CUSTOM
    kind                 VARCHAR(32) NOT NULL, -- FOOD, SUPPLEMENT_CALORIC
    state                VARCHAR(16) NOT NULL, -- RAW, AS_SERVED
    quantity_g           DOUBLE PRECISION NOT NULL,
    household_measure    VARCHAR(100),
    preparation          VARCHAR(200),
    calories             DOUBLE PRECISION NOT NULL,
    protein              DOUBLE PRECISION NOT NULL,
    carbohydrates        DOUBLE PRECISION NOT NULL,
    fat                  DOUBLE PRECISION NOT NULL
);

CREATE INDEX idx_meal_items_meal ON meal_items (meal_id);