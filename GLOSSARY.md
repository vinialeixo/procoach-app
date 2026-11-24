# ProCoach — Glossário de Domínio

> Vocabulário ubíquo do ProCoach (layout single-context conforme [`docs/agents/domain.md`](docs/agents/domain.md)).
> Ao nomear agregados, endpoints, telas, issues ou testes, use os termos canônicos abaixo e evite os sinônimos listados como não recomendados.

---

## Pessoas e Acesso

### Coach
- **Termo no código:** `Coach` (`com.fitness.procoach.coach`), papel `ROLE_COACH`.
- **Definição:** Profissional de treinamento/nutrição e cliente (tenant) da plataforma. É o único autor de avaliações físicas, dietas, periodizações, planos de treino e prescrições, e detém a decisão final sobre qualquer recomendação.
- **Evitar:** "Treinador", "Personal", "Nutricionista" (como sinônimo genérico de tenant) ou confundir `Coach.id` com `User.id`.

### Atleta
- **Termo no código:** `Athlete` (`com.fitness.procoach.athlete`), papel `ROLE_ATHLETE`.
- **Definição:** Pessoa gerenciada por um `Coach` dentro da sua carteira (isolamento multi-tenant por `coachId`). Possui ciclo de vida (`ACTIVE`, `PAUSED`, `FINISHED`), exclusão lógica (`soft-delete`) e acesso **somente leitura** aos próprios dados liberados (dieta aprovada, plano/sessões publicados e avaliações).
- **Evitar:** "Aluno", "Cliente" (no código, cliente/tenant é o Coach), "Paciente".

---

## Treinamento e Biblioteca

### Periodização
- **Termo no código:** `Periodization` (`com.fitness.procoach.training`).
- **Definição:** Ciclo estruturado de treinamento de um atleta (ex.: macrociclo/mesociclo com datas de início/fim e status), ao qual versões de `Plano de Treino` são vinculadas. Não contém prescrições diretamente.
- **Evitar:** "Ciclo genérico", "Temporada".

### Plano de Treino
- **Termo no código:** `TrainingPlan` (`com.fitness.procoach.training`).
- **Definição:** Agregado versionado e imutável vinculado a um `Coach`, um `Atleta` e uma `Periodização`. Cada alteração gera nova versão encadeada (`previousVersionId`), copiando as sessões da versão anterior em rascunho; ao ativar uma nova versão (`ACTIVE`), versões anteriores são encerradas como histórico (`FINISHED`).
- **Evitar:** "Ficha de treino" (ambíguo entre plano e sessão).

### Sessão
- **Termo no código:** `Training` / sessão de treino (`com.fitness.procoach.training`).
- **Definição:** Unidade executável de treino pertencente a uma versão específica de `Plano de Treino` (`planId`), organizando a rotina (ex.: treino A/B/C, dia/semana) e agrupando `Prescrições`.
- **Evitar:** "Treino" isolado quando for necessário distinguir a sessão do `Plano de Treino`.

### Prescrição
- **Termo no código:** `ExercisePrescription` (`com.fitness.procoach.training`).
- **Definição:** Item de uma `Sessão` que associa um exercício da biblioteca (`library`) a parâmetros de execução: séries, repetições, carga, intensidade (RPE/RIR/%1RM), descanso e observações. Preserva referência à versão do exercício prescrita originalmente e sinaliza quando há versão mais nova no catálogo.
- **Evitar:** "Exercício do treino" quando se referir à instância prescrita (em oposição ao `Exercise` da biblioteca).

---

## Nutrição e Plano Alimentar

### Dieta
- **Termo no código:** `Diet` (`com.fitness.procoach.nutrition`).
- **Definição:** Agregado versionado e imutável que representa uma versão do plano alimentar de um `Atleta`, com objetivo (`DietObjective`), metas/totais de macronutrientes (`Macronutrients`), `Refeições`, `Suplementos`, avisos nutricionais (`warnings[]`) e workflow de status travado (`DRAFT` → `IN_REVIEW` → `APPROVED` → `ARCHIVED`). Cada alteração gera nova versão encadeada (`previousVersionId`) com motivo obrigatório.
- **Evitar:** "Cardápio", "Protocolo alimentar" (como sinônimo do agregado).

### Refeição
- **Termo no código:** `Meal` (`com.fitness.procoach.nutrition`).
- **Definição:** Subdivisão cronológica/funcional de uma `Dieta` (ex.: café da manhã, pré-treino, jantar) com horário/ordem e lista de `Itens` (`MealItem`). Os totais de macronutrientes da dieta são derivados da soma dos itens quando há refeições cadastradas.
- **Evitar:** "Horário" ou "Prato" como nome da entidade.

### Item / TACO
- **Termo no código:** `MealItem` e catálogo de alimentos `TACO` / `CUSTOM` / `USDA` (`com.fitness.procoach.nutrition`).
- **Definição:** Alimento prescrito em uma `Refeição` com gramatura em gramas (`quantityG`), convenção de estado (`RAW` para peso cru quando há cocção; `AS_SERVED` para consumo direto), medida caseira opcional e **snapshot** dos macronutrientes no momento da prescrição (alterações futuras no catálogo não modificam dietas já criadas). A tabela TACO (Tabela Brasileira de Composição de Alimentos) é a base padrão de referência.
- **Evitar:** "Ingrediente" ou usar peso cozido como padrão de `quantityG`.

### Suplemento
- **Termo no código:** `Supplement` (`com.fitness.procoach.nutrition`) e `MealItem` com `kind = SUPPLEMENT_CALORIC`.
- **Definição:** Substância complementar prescrita na `Dieta`. Suplementos não calóricos ou sem impacto relevante de macros (ex.: creatina, multivitamínico) ficam na seção própria `supplements[]` com `countsTowardMacros: false`; fontes calóricas explícitas (ex.: Whey Protein) entram como `MealItem` (`SUPPLEMENT_CALORIC`) e contabilizam nos macros da refeição.
- **Evitar:** Misturar creatina/vitaminas no cálculo de calorias da refeição.

---

## Avaliação Física e Métricas Energéticas

### Avaliação Física
- **Termo no código:** `Assessment` (`com.fitness.procoach.assessment`).
- **Definição:** Registro antropométrico imutável (data obrigatória, peso, altura, dobras/circunferências, composição corporal) criado pelo `Coach` para um `Atleta`. Nunca é sobrescrita; permite comparação lado a lado entre datas e serve de base para cálculos metabólicos.
- **Evitar:** "Anamnese" ou "Check-in" (check-in é acompanhamento periódico previsto para o MVP2).

### IMC
- **Termo no código:** Índice de Massa Corporal (`bmi` em `AssessmentMetrics`).
- **Definição:** Razão entre peso (kg) e altura ao quadrado (m²), calculada automaticamente na `Avaliação Física`.
- **Evitar:** Calcular ou persistir manualmente no frontend como fonte de verdade.

### TMB
- **Termo no código:** Taxa Metabólica Basal (`bmr` em `AssessmentMetrics`).
- **Definição:** Estimativa do gasto energético em repouso calculada automaticamente a partir dos dados da `Avaliação Física`.
- **Evitar:** Confundir com `GET` (gasto total diário).

### GET
- **Termo no código:** Gasto Energético Total (`tdee` em `AssessmentMetrics`).
- **Definição:** Estimativa do gasto calórico diário total do atleta (TMB multiplicada pelo fator de atividade), usada como referência no cálculo do `Balanço Energético`.
- **Evitar:** "Calorias da dieta" ou "TMB".

### Balanço Energético
- **Termo no código:** `EnergyBalance` (`GET /api/v1/diets/energy-balance`).
- **Definição:** Diferença entre as calorias prescritas na versão corrente da `Dieta` e o `GET` da avaliação física mais recente do atleta, classificada em déficit, manutenção (faixa de ±5%) ou superávit.
- **Evitar:** Calcular no frontend sem consultar a base da avaliação.

---

## Governança, Auditoria e Ciclo de Vida

### AuditLog
- **Termo no código:** `AuditLog` (`com.fitness.procoach.audit`, tabela `audit_log`).
- **Definição:** Registro **append-only** e imutável (protegido no PostgreSQL por triggers contra `UPDATE`, `DELETE` e `TRUNCATE`) que grava quem (`author`), quando (`timestamp`), qual ação, qual entidade e o antes/depois das operações de escrita relevantes na mesma transação.
- **Evitar:** "Log de aplicação", "Tabela editável de histórico".

### Soft-delete
- **Termo no código:** Exclusão lógica (`deleted = true` / `deletedAt`).
- **Definição:** Estratégia obrigatória de remoção no ProCoach em que registros nunca sofrem `DELETE` físico no banco, preservando integridade referencial, versões anteriores e trilha no `AuditLog`. Registros removidos logicamente não são retornados em consultas padrão.
- **Evitar:** `DELETE` físico (hard-delete) em entidades de domínio.
