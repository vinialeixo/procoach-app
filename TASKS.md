# ProCoach — Backlog de Tasks (execução)

> Fonte: planilha de tasks de execução (importada em 2026-09-01). Cada task tem ID, épico, sprint, tipo, prioridade, estimativa e dependências (`Depende de`).
> Ver `USER_STORIES.md` para os critérios de aceite completos por épico.
> **Nota de correção**: na planilha original, as tasks do MVP2 usavam prefixos de dependência inconsistentes (`KB-xxx`, `RAG-xxx`, `CHK-xxx`, `MVP2-xxx`) que não existem como Task ID real — só existe `M2-xxx`. Normalizei todas para `M2-xxx` pelo número (ex.: `RAG-012` → `M2-012`), preservando a ordem lógica original. Confirme com o time se essa reconciliação bate com a intenção original antes de seguir cegamente a ordem.
> **Lacuna encontrada (2026-09-02)**: a planilha original não tinha nenhuma task de front para o atleta visualizar sua própria dieta/treino (só leitura) — apesar de `US-038`/RF-11 exigirem isso e o C4 do UML mostrar `Athlete -->|visualiza planos| SPA`. Adicionei `T-054` e `T-055` abaixo (fora da numeração/sprints originais) para cobrir isso. Confirme com o time se faz sentido antes de tratar como backlog oficial.

## Convenção de status

Todas as tasks começam como `To Do` na planilha original. Recomendo usar 4 estados neste arquivo: `To Do`, `In Progress`, `Review`, `Done` — e pedir ao Claude Code para atualizar o status aqui mesmo ao concluir uma task (ver seção de prompts no final).

## Onde estão as tasks

O backlog é dividido por ambiente. Os IDs são globais e não foram renumerados.

| Arquivo | Escopo | Tasks |
|---|---|---|
| [`backend/TASKS.md`](backend/TASKS.md) | Núcleo Java, RAG (Python), infra, segurança, testes de regra de negócio | 65 |
| [`frontend/TASKS.md`](frontend/TASKS.md) | SPA: telas, design system, tooling | 13 |
| este arquivo | Transversais: estratégia de QA, E2E integrado, UAT | 6 |

Critério: a task entra em `frontend/` se o entregável é código da SPA (tasks "Front: ..." e setup do front). Entra aqui se só faz sentido com front e back juntos (E2E, UAT, plano de QA). O resto fica em `backend/`, inclusive testes de QA de regra de negócio e hardening.

---

# Tasks transversais

# MVP (v1.0)


## S1

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-007** | E0 | Fundação | Definir plano de testes e estratégia de QA (DoD/CA) | QA | Must | 2 | — |

## S2

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-023** | E1 | US-01/02/03 | Testes E2E gestão de atletas | QA | Must | 2 | T-022 |

## S6

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-042** | - | MVP | Testes E2E integrados de todo o fluxo do MVP | QA | Must | 3 | T-041 |
| [ ] | **T-045** | - | MVP | UAT / testes de aceite final | QA | Must | 2 | T-044 |

### Resumo por Sprint — MVP

| Sprint | Foco | Tasks | Estimativa Dev (dias) | QA/DevOps (dias) |
|---|---|---|---|---|
| S1 | Fundação + Segurança + Auditoria | 1 | 0 | 2 |
| S2 | Atletas + Auditoria | 1 | 0 | 2 |
| S6 | Integração + Hardening + UAT | 2 | 0 | 5 |
| TOTAL |  | 4 | 0 | 9 |


---

# MVP2 (v1.1 — Knowledge Base, RAG, Check-ins)


## S11

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **M2-024** | - | MVP2 | Testes E2E integrados do fluxo IA + KB + check-ins | QA | Must | 3 | M2-023 |
| [ ] | **M2-026** | - | MVP2 | UAT / aceite final do MVP2 | QA | Must | 2 | M2-025 |

### Resumo por Sprint — MVP2

| Sprint | Foco | Tasks | Dev/Dev-Py (dias) | QA/DevOps (dias) |
|---|---|---|---|---|
| S11 | Integração + Hardening + UAT | 2 | 0 | 5 |
| TOTAL |  | 2 | 0 | 5 |
