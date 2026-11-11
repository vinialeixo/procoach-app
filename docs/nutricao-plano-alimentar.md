# ProCoach — Plano Alimentar e Nutrição Esportiva

> Documento de produto e contrato técnico do fluxo de dietas.
> Análise PO + Nutrição Esportiva em 2026-11-11. Backlog correspondente:
> `T-085` a `T-090` (backend) em [`backend/TASKS.md`](../backend/TASKS.md) e
> `T-091` a `T-095` (frontend) em [`frontend/TASKS.md`](../frontend/TASKS.md).
>
> Persona principal: **Coach** (tenant). Persona secundária: **Atleta** (somente leitura).

---

## 1. Objetivo

Evoluir o fluxo de dietas do ProCoach de um **orçamento de 4 macros** para um
**plano alimentar executável**: refeições, alimentos, gramaturas, substituições,
periodização e acompanhamento — sem violar as regras inegociáveis do produto
(versionamento, auditoria, soft-delete, coach decide, IA consultiva).

Este documento consolida:

1. o fluxo atual (as-is) e suas lacunas;
2. as regras de negócio do plano alimentar;
3. o contrato JSON v1 do plano (incluindo o gerado por IA);
4. o modelo de domínio alvo;
5. as decisões abertas e o mapa de tasks.

---

## 2. Fluxo atual (as-is)

### 2.1 Telas e arquivos

| Fluxo | Rota | Arquivos |
|---|---|---|
| Ficha do atleta → aba Dietas | `/coach/athletes/:id?tab=dietas` | `AthleteDetailPage.tsx`, `AthleteDietsPanel.tsx` |
| Criar dieta (v1) | `/coach/athletes/:id/diets/new` | `DietEditorPage.tsx` |
| Criar nova versão | `/coach/athletes/:id/diets/:versionId/new-version` | `DietEditorPage.tsx` |
| Atleta vê a dieta aprovada | `/athlete/dieta` | `AthleteDietPage.tsx` |
| Contrato/hooks | — | `src/lib/diets.ts` |
| Backend | `/api/v1/diets` | `DietController`, `DietApplicationService`, `Diet` |

### 2.2 O que existe hoje

- `Diet` guarda **apenas** `objective` + `Macronutrients(calories, protein, carbohydrates, fat)`.
- Versionamento por cadeia (`previousVersionId`), versões anteriores `readOnly`,
  motivo obrigatório na nova versão.
- Fluxo de status travado: `DRAFT → IN_REVIEW → APPROVED → ARCHIVED`.
- Comparação lado a lado de 2+ versões, mas **só dos totais**.
- Balanço energético vs GET existe no backend
  (`GET /diets/energy-balance`, faixa de ±5% = manutenção), porém
  **não é exibido em nenhuma tela** (`useEnergyBalance` é código morto).

### 2.3 Lacunas encontradas

**Nutricionais**

- Não há refeições, alimentos, porções, horários, substituições ou lista de compras.
- Não há base de alimentos (TACO/USDA/custom) nem gramatura por alimento.
- Não há distribuição por refeição (proteína ~0,4 g/kg/refeição, carbo peri-treino).
- Não há relativização por peso (g/kg) nem % das calorias.
- Kcal é uma segunda fonte de verdade: não há checagem 4/4/9.
- `Macronutrients` rejeita zero, impedindo estratégias com carbo/gordura zerados.
- Não há fibras, água, sódio, micronutrientes, restrições/alergias ou suplementos.
- Não há periodização (dia de treino vs. descanso, refeed, diet break, peak week).
- Não há vínculo com check-ins (peso, fotos, fome, digestão, aderência).

**De produto/UX**

- Não é possível **ler** os macros de uma versão: o card atual não mostra P/C/G,
  a tabela mostra só kcal e a comparação exige selecionar 2 versões.
- Não existe tela de detalhe/leitura de uma versão.
- O KPI central (balanço energético) está invisível.
- Auditoria exibe UUID truncado em vez de nome/e-mail.
- Taxonomia de objetivos divergente entre atleta (`RECOMPOSITION`) e dieta
  (`WEIGHT_LOSS`), sem vínculo entre as duas.
- Aprovação sem checklist nutricional.
- Risco de regressão: ao arquivar a versão mais recente, o atleta pode voltar a
  ver uma versão aprovada anterior.
- Estado vazio não orienta registrar avaliação (necessária para o balanço).

---

## 3. Regras de negócio do plano alimentar

### 3.1 Estado dos alimentos (convenção de gramatura)

- Todo item prescrito usa **peso cru** (`state: "RAW"`) quando o alimento passa
  por cocção.
- Alimentos consumidos como estão (frutas, laticínios, pães, embutidos prontos)
  usam `state: "AS_SERVED"`.
- O modo de preparo (`preparation`) é informativo e **não altera** `quantityG`.
- `quantityG` é sempre o peso cru / como comprado. Fator de cocção, se existir,
  é apenas exibição.

### 3.2 Macronutrientes

- Valores `0.0` são aceitos para carboidratos e/ou gorduras em estratégias
  específicas (cetogênica severa, peak week).
- Todo macro zerado gera um item em `warnings[]`.
- O plano nunca é bloqueado automaticamente por warning — a decisão é do coach.
- `calories` deve ser > 0 (ou ao menos um macro positivo).

### 3.3 Estrutura do plano

- `planMode`: `MACRO_BUDGET` (só metas globais), `MEAL_PLAN` (refeições) ou
  `HYBRID` (metas + refeições).
- Suplementos **não** entram nos macros da refeição, exceto fontes calóricas
  explícitas (ex.: Whey Protein), que entram como `MealItem` com
  `kind: "SUPPLEMENT_CALORIC"`.
- Creatina, multivitamínico e similares vão em `supplements[]` com
  `countsTowardMacros: false`.

### 3.4 Nomenclatura e base de alimentos

- Preferência pela nomenclatura TACO: `"Peito de frango, sem pele"`,
  `"Arroz, tipo 1"`, `"Ovo, de galinha, inteiro"`.
- `source`: `TACO` | `CUSTOM` | `USDA`.
- Alimento `CUSTOM` criado pelo coach exige `coachApproved: true`.
- Macros do item são **snapshot** no momento da prescrição: mudança no catálogo
  não altera dietas já criadas.

### 3.5 Decisão e aprovação

- A IA é **consultiva e read-only**; todo plano gerado nasce como rascunho e
  exige `requiresCoachApproval: true`.
- Nenhuma aprovação, publicação ou alteração de status é automática.
- Toda escrita continua gerando `AuditLog`.

---

## 4. Contrato JSON v1

```json
{
  "schemaVersion": "1.0",
  "meta": {
    "generatedAt": "2026-11-11T12:00:00Z",
    "source": "AI_ASSISTANT",
    "requiresCoachApproval": true,
    "baseDietVersionId": null
  },
  "athlete": {
    "athleteId": "uuid",
    "name": "string",
    "sex": "MALE | FEMALE",
    "age": 0,
    "heightCm": 0,
    "weightKg": 0,
    "bodyFatPercent": 0
  },
  "strategy": {
    "objective": "CUTTING | HYPERTROPHY | MAINTENANCE | BULKING | WEIGHT_LOSS | RECOMPOSITION",
    "phase": "OFF_SEASON | PRE_CONTEST | PEAK_WEEK | RECOMPOSITION",
    "trainingDay": true,
    "notes": "string"
  },
  "targets": {
    "calories": 0,
    "protein": 0,
    "carbohydrates": 0,
    "fat": 0,
    "fiber": 0,
    "waterMl": 0
  },
  "derived": {
    "tmbKcal": 0,
    "getKcal": 0,
    "balanceKcal": 0,
    "balancePercent": 0,
    "classification": "DEFICIT | MAINTENANCE | SURPLUS",
    "proteinGPerKg": 0,
    "carbohydratesGPerKg": 0,
    "fatGPerKg": 0,
    "proteinPctKcal": 0,
    "carbohydratesPctKcal": 0,
    "fatPctKcal": 0
  },
  "consistency": {
    "calculatedCalories": 0,
    "declaredCalories": 0,
    "deltaKcal": 0,
    "deltaPercent": 0,
    "isConsistent": true
  },
  "planMode": "MEAL_PLAN",
  "meals": [
    {
      "mealId": "meal-1",
      "name": "Café da manhã",
      "order": 1,
      "time": "07:00",
      "target": { "calories": 0, "protein": 0, "carbohydrates": 0, "fat": 0 },
      "items": [
        {
          "foodId": "taco:ovo-de-galinha-inteiro",
          "name": "Ovo, de galinha, inteiro",
          "source": "TACO",
          "kind": "FOOD",
          "state": "RAW",
          "quantityG": 100,
          "householdMeasure": "2 unidades",
          "preparation": null,
          "macros": { "calories": 143, "protein": 13.0, "carbohydrates": 1.6, "fat": 9.5 }
        }
      ],
      "totals": { "calories": 0, "protein": 0, "carbohydrates": 0, "fat": 0 }
    }
  ],
  "supplements": [
    {
      "name": "Creatina monoidratada",
      "dose": "5 g",
      "timing": "qualquer horário",
      "countsTowardMacros": false
    }
  ],
  "warnings": [
    {
      "code": "ZERO_CARBOHYDRATE",
      "severity": "WARNING",
      "message": "Carboidrato zerado por estratégia de peak week; monitorar desempenho e hidratação."
    }
  ],
  "totals": { "calories": 0, "protein": 0, "carbohydrates": 0, "fat": 0 },
  "disclaimer": "Plano consultivo. A decisão final e a aprovação são do coach."
}
```

### 4.1 Códigos de warning

| Código | Severidade | Quando |
|---|---|---|
| `ZERO_CARBOHYDRATE` | WARNING | carboidrato = 0 |
| `ZERO_FAT` | WARNING | gordura = 0 |
| `ZERO_PROTEIN` | CRITICAL | proteína = 0 |
| `MACRO_CALORIE_MISMATCH` | WARNING | kcal declaradas ≠ 4/4/9 além da tolerância |
| `LOW_CALORIES_VS_TMB` | WARNING | kcal abaixo da TMB |
| `AGGRESSIVE_DEFICIT` | WARNING | déficit acima da faixa segura (ex.: > 25% do GET) |
| `PROTEIN_BELOW_RANGE` | WARNING | proteína < 1,6 g/kg |
| `FAT_BELOW_RANGE` | WARNING | gordura < 0,5 g/kg |
| `FIBER_BELOW_RECOMMENDATION` | INFO | fibras < 25 g |
| `RESTRICTION_CONFLICT` | CRITICAL | alimento conflita com restrição/alergia |
| `RAW_WEIGHT_ASSUMED` | INFO | item sem estado explícito, assumido RAW |
| `SUPPLEMENT_EXCLUDED_FROM_MACROS` | INFO | suplemento não calórico fora dos macros |

### 4.2 Faixas de referência (exibição, não bloqueio)

- Proteína: **1,6–2,2 g/kg/dia**; por refeição ~**0,4 g/kg**.
- Gordura: **0,5–1,0 g/kg/dia** (mínimo ~0,5 g/kg).
- Carboidrato: restante das calorias, concentrado no peri-treino.
- Fibras: **25–38 g/dia**; água: **~35 ml/kg**.
- Tolerância de consistência calórica sugerida: **±5% ou ±50 kcal**.

> São referências de apoio. O coach define e justifica a estratégia final.

---

## 5. Modelo de domínio alvo

```
Diet (versionada, inalterada nas regras atuais)
├── objective
├── targets: Macronutrients        # orçamento
├── meals: List<Meal>
│   └── Meal (id, name, order, time, notes, target)
│       └── MealItem (id, foodRef, name, source, kind, state, quantityG,
│                     householdMeasure, macrosSnapshot)
├── supplements: List<Supplement>
└── totais calculados a partir dos itens
```

Regras:

- `Diet` continua sendo o agregado raiz e o único ponto de versionamento.
- Totais nunca são digitados quando há refeições: são derivados dos itens.
- Nova versão copia refeições, itens e suplementos; a anterior vira `readOnly`.
- `Food` é catálogo (fora do agregado), referenciado por `foodId` + snapshot.
- Suplemento calórico é `MealItem`; os demais ficam em `supplements`.

### 5.1 Impactos no backend atual

1. `Macronutrients` hoje exige todos os valores `> 0` → mudar para `>= 0`.
2. Não existe refeição/alimento no domínio → criar `Meal`, `MealItem`, `Food`.
3. Não existe suplemento → criar `Supplement` e a distinção de macros.
4. `DietResponse` precisa expor refeições, derivados, warnings e autor legível.
5. Visão do atleta precisa de regra explícita para versão arquivada (ver T-089).

---

## 6. Regra de visibilidade do atleta (proposta para T-089)

Ao consultar a dieta corrente:

- Se a **versão mais alta da cadeia** está `APPROVED` → é a corrente.
- Se a mais alta está `DRAFT`/`IN_REVIEW` → o atleta continua vendo a maior
  `APPROVED` abaixo dela (rascunho não vaza).
- Se a mais alta está `ARCHIVED` → **não** cair para uma versão anterior:
  responder 404 (sem plano ativo). Isso corrige a regressão atual em que o
  atleta volta a ver a v1 depois de a v2 ser arquivada.

---

## 7. Decisões abertas

1. Base de alimentos: TACO, USDA, catálogo do coach ou combinação?
2. Padrão de gramatura: sempre cru (recomendado) ou permitir escolher por item?
3. Expor metas por refeição no MVP ou só totais do dia?
4. Suplementos entram no MVP ou ficam para depois da T-085?
5. Arquivar a última versão deve despublicar a dieta do atleta (404)?
6. Quem pode aprovar: o próprio coach ou um revisor separado?
7. Fibras/água/micronutrientes entram no MVP ou em fase 2?

---

## 8. Backlog e dependências

| Task | Lado | Título | Depende de |
|---|---|---|---|
| T-085 | BE | Modelo de refeições e itens de alimento na dieta versionada | T-029, T-031, T-050 |
| T-086 | BE | Catálogo TACO com peso cru e medida caseira | T-085 |
| T-087 | BE | Macros zeráveis + warnings + consistência + derivados | T-029, T-030 |
| T-088 | BE | Contrato JSON v1 do plano gerado por IA | T-085, T-087 |
| T-089 | BE | Balanço na versão corrente + regra de arquivamento do atleta | T-030, T-065 |
| T-090 | BE | Suplementos + auditoria legível do autor | T-013, T-085 |
| T-091 | FE | Resumo da versão atual (macros, g/kg, %kcal, balanço) | T-089 |
| T-092 | FE | Detalhe somente leitura + comparação com derivados | T-085, T-090 |
| T-093 | FE | Editor por refeições/itens com catálogo TACO | T-085, T-086 |
| T-094 | FE | Avisos nutricionais e faixas de referência no editor | T-087 |
| T-095 | FE | Visão do atleta por refeição + lista de compras | T-093, T-054 |

---

## 9. Definition of Done (comum)

- [ ] Regras inegociáveis preservadas (nunca sobrescrever, auditoria, soft-delete,
      coach decide, IA read-only).
- [ ] Testes automatizados cobrindo os critérios de aceitação.
- [ ] `./mvnw test` verde no backend e `npm run build`/`npm run lint` limpos no front.
- [ ] `TASKS.md` do lado correspondente atualizado.
- [ ] Contrato documentado neste arquivo e em `SPEC.md`, quando existir.
