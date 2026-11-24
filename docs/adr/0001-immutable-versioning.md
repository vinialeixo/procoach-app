# ADR-0001: Versionamento Imutável de Agregados Críticos

- **Status:** Aceito
- **Data:** 2026-09-01

## Contexto

No domínio do ProCoach (acompanhamento esportivo e nutricional de alta rastreabilidade), alterações em dietas, planos de treino, avaliações físicas e exercícios da biblioteca precisam preservar o histórico exato do que foi prescrito ao atleta em cada momento. Sobrescrever registros existentes destruiria a comparabilidade histórica e invalidaria prescrições antigas.

## Decisão

Agregados críticos de prescrição e avaliação **nunca são sobrescritos**:

1. **`Diet` (`nutrition`) e `TrainingPlan` (`training`):** toda alteração de conteúdo gera uma **nova versão** encadeada (`version + 1`, `previousVersionId`), mantendo versões anteriores como somente leitura (`readOnly = true`) e consultáveis no histórico.
2. **`Assessment` (`assessment`):** cada avaliação física é um registro imutável vinculado à sua data; novas medições criam novas instâncias para comparação lado a lado.
3. **`Exercise` (`library`):** edições no catálogo criam uma nova versão (`version + 1`, `previousVersionId`), marcando a anterior como `superseded` para não alterar retroativamente treinos já prescritos.
4. **Snapshot em `MealItem`:** os macronutrientes de um alimento prescrito são gravados por snapshot na dieta, desacoplando-a de edições futuras no catálogo.

## Consequências

### Positivas
- Rastreabilidade completa de todas as decisões do coach ao longo do tempo.
- Segurança jurídica e clínica: prescrições antigas e avaliações nunca mudam retroativamente.
- Comparação lado a lado nativa entre versões de dieta, plano de treino e avaliações.

### Negativas / Trade-offs
- Maior volume de linhas no banco de dados e necessidade de consultas filtrando a versão corrente ou ativa.
- Fluxos de UI precisam distinguir explicitamente "criar nova versão" (com motivo) de "editar rascunho".

## Referências
- `AGENTS.md` (regra de versionamento imutável)
- `backend/SPEC.md` (RNF-02, RN-02)
- `backend/src/main/java/com/fitness/procoach/nutrition/domain/Diet.java`
