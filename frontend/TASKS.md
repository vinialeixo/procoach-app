# ProCoach Frontend — Tasks

> Tasks da SPA (`frontend/`): telas, design system e tooling do front.
> Backlog completo, convenções de status e contexto: [`TASKS.md` na raiz](../TASKS.md).
> Os IDs (`T-xxx`, `M2-xxx`) são globais — a coluna "Depende de" pode apontar para task de outro arquivo (`backend/TASKS.md` ou o `TASKS.md` da raiz).
> Total neste arquivo: **13** tasks.

---

# MVP (v1.0)


## S1

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-006** | E0 | Fundação | Setup projeto Front Web (SPA), design system base | Dev | Must | 2 | — |
| [x] | **T-011** | E9 | US-20 | Front: telas de login/registro/MFA | Dev | Must | 2 | T-008 |

## S2

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-019** | E1 | US-01 | Front: formulário de cadastro de atleta | Dev | Must | 2 | T-018 |
| [ ] | **T-022** | E1 | US-03 | Front: listagem/detalhe do atleta + troca de status | Dev | Must | 2 | T-021 |

## S3

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-027** | E2 | US-04/05/06 | Front: avaliação física + gráficos de evolução + comparação | Dev | Must | 3 | T-025 |

## S4

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-034** | E3 | US-07/08/09 | Front: editor de dieta + versões + aprovação | Dev | Must | 4 | T-033 |

## S5

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-040** | E4/E8 | US-10/11/19 | Front: biblioteca + montagem de treino/periodização | Dev | Must | 4 | T-037 |

### Resumo por Sprint — MVP

| Sprint | Foco | Tasks | Estimativa Dev (dias) | QA/DevOps (dias) |
|---|---|---|---|---|
| S1 | Fundação + Segurança + Auditoria | 2 | 4 | 0 |
| S2 | Atletas + Auditoria | 2 | 4 | 0 |
| S3 | Avaliação + Nutrição (início) | 1 | 3 | 0 |
| S4 | Nutrição versionada + Biblioteca | 1 | 4 | 0 |
| S5 | Treino + Biblioteca | 1 | 4 | 0 |
| TOTAL |  | 7 | 19 | 0 |


---

# Adendo — tasks fora da planilha original

> Estas tasks não estavam na planilha original. Adicionadas para cobrir `US-038`/RF-11
> (atleta só vê os próprios dados) e o C4 do UML (`Athlete -->|visualiza planos| SPA`),
> que previam essa visão sem nenhuma task de front correspondente. Numeração `T-054+`
> para não colidir com a numeração oficial (`T-001`–`T-053`). Sugestão de posição no
> cronograma: junto do Sprint 4-5, depois que `nutrition`/`training` já existirem.
> Persona de ambas: **Atleta**. Rota: `/athlete/...` (ver `App.tsx` do frontend).

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-054** | E1/E3/E4 | US-38 | Front: área do atleta — visualizar dieta e treino atuais (somente leitura) | Dev | Must (sugerido) | 3 | T-033, T-037 |
| [ ] | **T-055** | E2 | US-38 | Front: área do atleta — histórico de avaliações (somente leitura) | Dev | Should (sugerido) | 2 | T-025 |
| [ ] | **T-056** | E0 | Fundação | Front: configurar ESLint (config ausente desde T-006) | Dev | Must (sugerido) | 0.5 | T-006 |

> `T-056` identificada em 2026-09-02 durante a implementação da `T-011`: `npm run lint`
> quebrado por falta de `.eslintrc`/`eslint.config.*` na raiz do frontend — lacuna da
> `T-006` original, não de quem implementou `T-011`. Resolver antes da Etapa 2 para não
> acumular divergência de estilo entre as próximas 8 telas.

---

# MVP2 (v1.1 — Knowledge Base, RAG, Check-ins)


## S7

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **M2-004** | E7 | US-18 | Front: gestão da Knowledge Base | Dev | Must | 3 | M2-003 |

## S9

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **M2-017** | E6 | US-15/16/17 | Front: painel de sugestões da IA + citações + ações do coach | Dev | Must | 4 | M2-014 |

## S10

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **M2-021** | E5 | US-13 | Front: envio de check-in (atleta) + visualização (coach) | Dev | Should | 3 | M2-020 |

### Resumo por Sprint — MVP2

| Sprint | Foco | Tasks | Dev/Dev-Py (dias) | QA/DevOps (dias) |
|---|---|---|---|---|
| S7 | Knowledge Base + setup RAG | 1 | 3 | 0 |
| S9 | IA assistiva (query + citações) | 1 | 4 | 0 |
| S10 | Check-ins + análise por IA | 1 | 3 | 0 |
| TOTAL |  | 3 | 10 | 0 |
