-- T-090: suplementos em seção própria da dieta (countsTowardMacros; Whey como item calórico)
CREATE TABLE diet_supplements (
    id                   UUID PRIMARY KEY,
    diet_id              UUID        NOT NULL REFERENCES diets (id) ON DELETE CASCADE,
    name                 VARCHAR(200) NOT NULL,
    dose                 VARCHAR(100) NOT NULL,
    timing               VARCHAR(200),
    counts_toward_macros BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_diet_supplements_diet ON diet_supplements (diet_id);