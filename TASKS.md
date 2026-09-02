# ProCoach — Backlog de Tasks (execução)

> Fonte: planilha de tasks de execução (importada em 2026-09-01). Cada task tem ID, épico, sprint, tipo, prioridade, estimativa e dependências (`Depende de`).
> Ver `USER_STORIES.md` para os critérios de aceite completos por épico.
> **Nota de correção**: na planilha original, as tasks do MVP2 usavam prefixos de dependência inconsistentes (`KB-xxx`, `RAG-xxx`, `CHK-xxx`, `MVP2-xxx`) que não existem como Task ID real — só existe `M2-xxx`. Normalizei todas para `M2-xxx` pelo número (ex.: `RAG-012` → `M2-012`), preservando a ordem lógica original. Confirme com o time se essa reconciliação bate com a intenção original antes de seguir cegamente a ordem.

## Convenção de status

Todas as tasks começam como `To Do` na planilha original. Recomendo usar 4 estados neste arquivo: `To Do`, `In Progress`, `Review`, `Done` — e pedir ao Claude Code para atualizar o status aqui mesmo ao concluir uma task (ver seção de prompts no final).


---

# MVP (v1.0)


## S1

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-001** | E0 | Fundação | Setup repositório Java (Clean Arch), estrutura de pastas, linters | Dev | Must | 2 | — |
| [ ] | **T-002** | E0 | Fundação | Configurar CI/CD e ambientes (dev/staging) | DevOps | Must | 2 | T-001 |
| [ ] | **T-003** | E0 | Fundação | Provisionar PostgreSQL + pgvector, Object Storage (S3), Message Broker | DevOps | Must | 2 | T-001 |
| [ ] | **T-004** | E0 | Fundação | Modelar bounded contexts e definir agregados | Dev | Must | 3 | T-001 |
| [x] | **T-005** | E0 | Fundação | Implementar event store base (Event Sourcing seletivo) | Dev | Must | 4 | T-004 |
| [ ] | **T-006** | E0 | Fundação | Setup projeto Front Web (SPA), design system base | Dev | Must | 2 | — |
| [ ] | **T-007** | E0 | Fundação | Definir plano de testes e estratégia de QA (DoD/CA) | QA | Must | 2 | — |
| [ ] | **T-008** | E9 | US-20 | Implementar autenticação + MFA | Dev | Must | 3 | T-004 |
| [ ] | **T-009** | E9 | US-20 | Implementar RBAC (papéis Coach x Atleta) | Dev | Must | 3 | T-008 |
| [ ] | **T-010** | E9 | US-20 | Restringir acesso do atleta apenas aos próprios dados | Dev | Must | 2 | T-009 |
| [ ] | **T-011** | E9 | US-20 | Front: telas de login/registro/MFA | Dev | Must | 2 | T-008 |
| [ ] | **T-012** | E9 | US-20 | Testes de segurança e autorização (RBAC) | QA | Must | 2 | T-010 |
| [ ] | **T-013** | E10 | US-21 | Implementar audit log imutável (append-only) | Dev | Must | 3 | T-005 |
| [ ] | **T-014** | E10 | US-21 | Versionamento de agregados (versão/data/autor/histórico) | Dev | Must | 4 | T-005 |
| [ ] | **T-015** | E10 | US-21 | Registrar eventos: cadastro, alteração, exclusão lógica, login, aprovação | Dev | Must | 3 | T-014 |
| [x] | **T-046** | E0 | Fundação | Criar módulo de domínio security + agregado User/Role (com.procoach.core.security) | Dev | Must | 2 | T-004 |
| [x] | **T-047** | E0 | Fundação | Criar módulo cross-cutting audit + agregado AuditLog/Version (com.procoach.core.audit) | Dev | Must | 2 | T-004 |
| [x] | **T-053** | E0 | Fundação | Configurar Spring Modulith + verificação de boundaries entre módulos | Dev | Must | 2 | T-004 |

## S2

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-016** | E10 | US-21 | Testes de auditoria e versionamento (não-sobrescrita) | QA | Must | 3 | T-015 |
| [x] | **T-017** | E1 | US-01 | Modelo + repositório de Atleta (ID único imutável) | Dev | Must | 2 | T-004 |
| [ ] | **T-018** | E1 | US-01 | Endpoint cadastro de atleta + validação de campos obrigatórios | Dev | Must | 2 | T-017 |
| [ ] | **T-019** | E1 | US-01 | Front: formulário de cadastro de atleta | Dev | Must | 2 | T-018 |
| [ ] | **T-020** | E1 | US-02 | Suporte a múltiplos protocolos por atleta | Dev | Must | 2 | T-017 |
| [ ] | **T-021** | E1 | US-03 | Gestão de status (Ativo/Pausado/Finalizado) + soft-delete | Dev | Must | 2 | T-017 |
| [ ] | **T-022** | E1 | US-03 | Front: listagem/detalhe do atleta + troca de status | Dev | Must | 2 | T-021 |
| [ ] | **T-023** | E1 | US-01/02/03 | Testes E2E gestão de atletas | QA | Must | 2 | T-022 |
| [x] | **T-048** | E1 | US-01 | Criar módulo de domínio athlete + agregado Athlete (com.procoach.core.athlete) | Dev | Must | 3 | T-004 |

## S3

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-024** | E2 | US-04 | Modelo + endpoint de avaliação física (data obrigatória, sem sobrescrever) | Dev | Must | 2 | T-017 |
| [ ] | **T-025** | E2 | US-05 | Motor de cálculos automáticos (IMC, TMB, GET, evolução) | Dev | Must | 3 | T-024 |
| [ ] | **T-026** | E2 | US-06 | Comparação de avaliações lado a lado | Dev | Should | 2 | T-024 |
| [ ] | **T-027** | E2 | US-04/05/06 | Front: avaliação física + gráficos de evolução + comparação | Dev | Must | 3 | T-025 |
| [ ] | **T-028** | E2 | US-04/05/06 | Testes dos cálculos e comparação de avaliações | QA | Must | 2 | T-027 |
| [ ] | **T-029** | E3 | US-07 | Modelo de dieta + macros + objetivo obrigatório | Dev | Must | 2 | T-017 |
| [ ] | **T-030** | E3 | US-07 | Cálculos de TMB/GET/déficit/superávit | Dev | Must | 2 | T-029 |
| [x] | **T-049** | E2 | US-05 | Criar módulo de domínio assessment + agregado Assessment (com.procoach.core.assessment) | Dev | Must | 3 | T-004 |
| [ ] | **T-050** | E3 | US-08 | Criar módulo de domínio nutrition + agregado Diet (versionada) (com.procoach.core.nutrition) | Dev | Must | 3 | T-004 |

## S4

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-031** | E3 | US-08 | Versionamento de dieta (v1->v2->v3) com histórico encadeado | Dev | Must | 3 | T-014 |
| [ ] | **T-032** | E3 | US-08 | Registrar quem/quando/motivo de cada alteração | Dev | Must | 2 | T-031 |
| [ ] | **T-033** | E3 | US-09 | Fluxo de status (Rascunho/Em Revisão/Aprovada/Arquivada) | Dev | Must | 3 | T-031 |
| [ ] | **T-034** | E3 | US-07/08/09 | Front: editor de dieta + versões + aprovação | Dev | Must | 4 | T-033 |
| [ ] | **T-035** | E3 | US-07/08/09 | Testes de versionamento e fluxo de aprovação da dieta | QA | Must | 3 | T-034 |
| [ ] | **T-036** | E8 | US-19 | Modelo + CRUD Biblioteca de Exercícios (campos científicos completos) | Dev | Must | 3 | T-004 |
| [ ] | **T-052** | E8 | US-19 | Criar módulo de domínio library + agregado Exercise (com.procoach.core.library) | Dev | Must | 2 | T-004 |

## S5

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-037** | E4 | US-10 | Modelo de periodização + treino (séries/reps/carga/intensidade/descanso) | Dev | Must | 3 | T-036 |
| [ ] | **T-038** | E4 | US-11 | Criação de exercícios personalizados pelo coach | Dev | Should | 2 | T-036 |
| [ ] | **T-039** | E4 | US-12 | Sugestão de exercícios semelhantes | Dev | Could | 2 | T-036 |
| [ ] | **T-040** | E4/E8 | US-10/11/19 | Front: biblioteca + montagem de treino/periodização | Dev | Must | 4 | T-037 |
| [ ] | **T-041** | E4/E8 | US-10/11/12/19 | Testes de treino e biblioteca | QA | Must | 3 | T-040 |
| [ ] | **T-051** | E4 | US-12 | Criar módulo de domínio training + agregado TrainingPlan (com.procoach.core.training) | Dev | Must | 3 | T-004 |

## S6

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-042** | - | MVP | Testes E2E integrados de todo o fluxo do MVP | QA | Must | 3 | T-041 |
| [ ] | **T-043** | - | MVP | Revisão LGPD (criptografia, soft-delete, consentimento) | Dev | Must | 2 | T-041 |
| [ ] | **T-044** | - | MVP | Hardening, correção de bugs e estabilização | Dev | Must | 3 | T-043 |
| [ ] | **T-045** | - | MVP | UAT / testes de aceite final | QA | Must | 2 | T-044 |

### Resumo por Sprint — MVP

| Sprint | Foco | Tasks | Estimativa Dev (dias) | Estimativa QA/DevOps (dias) |
|---|---|---|---|---|
| S1 | Fundação + Segurança + Auditoria | 18 | 37 | 8 |
| S2 | Atletas + Auditoria | 9 | 15 | 5 |
| S3 | Avaliação + Nutrição (início) | 9 | 20 | 2 |
| S4 | Nutrição versionada + Biblioteca | 7 | 17 | 3 |
| S5 | Treino + Biblioteca | 6 | 14 | 3 |
| S6 | Integração + Hardening + UAT | 4 | 5 | 5 |
| TOTAL |  | 53 | 108 | 26 |

---

# MVP2 (v1.1 — Knowledge Base, RAG, Check-ins)


## S7

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **M2-001** | E7 | US-18 | Modelo de conceito (definição, categoria, referências, nível de evidência) | Dev | Must | 2 | — |
| [ ] | **M2-002** | E7 | US-18 | Versionamento e arquivamento de conceitos (sem exclusão) | Dev | Must | 2 | M2-001 |
| [ ] | **M2-003** | E7 | US-18 | CRUD + API da Knowledge Base | Dev | Must | 2 | M2-002 |
| [ ] | **M2-004** | E7 | US-18 | Front: gestão da Knowledge Base | Dev | Must | 3 | M2-003 |
| [ ] | **M2-005** | E7 | US-18 | Upload de PDFs para Object Storage + evento de ingestão | Dev | Must | 2 | M2-003 |
| [ ] | **M2-006** | E7 | US-18 | Testes da Knowledge Base (versionamento/arquivamento) | QA | Must | 2 | M2-004 |
| [ ] | **M2-007** | E6 | RAG | Setup serviço Python (RAG) + contrato gRPC com núcleo Go | Dev-Py | Must | 3 | M2-005 |

## S8

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **M2-008** | E6 | RAG | Pipeline de ingestão: parsing de PDF (LlamaParse) + chunking | Dev-Py | Must | 4 | M2-007 |
| [ ] | **M2-009** | E6 | RAG | Geração de embeddings + indexação no Vector DB (pgvector) com metadados | Dev-Py | Must | 3 | M2-008 |
| [ ] | **M2-010** | E6 | RAG | Reindexação assíncrona via Message Broker (KB/PDF alterados) | Dev-Py | Must | 3 | M2-009 |
| [ ] | **M2-011** | E6 | RAG | Spike/validação de qualidade de extração com lote piloto de PDFs | Dev-Py | Must | 2 | M2-008 |

## S9

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **M2-012** | E6 | US-15 | Query engine com retrieval + CitationQueryEngine (citações) | Dev-Py | Must | 4 | M2-009 |
| [ ] | **M2-013** | E6 | US-15 | Montagem de prompt com contexto + justificativa e origem da sugestão | Dev-Py | Must | 3 | M2-012 |
| [ ] | **M2-014** | E6 | US-16 | Endpoint de sugestão (aceitar/modificar/recusar) + registro em auditoria | Dev | Must | 3 | M2-012 |
| [ ] | **M2-015** | E6 | US-17 | Tratamento de evidências conflitantes (apresentar alternativas) | Dev-Py | Should | 3 | M2-012 |
| [ ] | **M2-016** | E6 | US-15 | Guardrail técnico: RAG read-only (sem credencial de escrita no domínio) | Dev | Must | 2 | M2-013 |
| [ ] | **M2-017** | E6 | US-15/16/17 | Front: painel de sugestões da IA + citações + ações do coach | Dev | Must | 4 | M2-014 |
| [ ] | **M2-018** | E6 | US-15/16/17 | Testes de IA: citações, guardrail read-only, aceite/recusa | QA | Must | 3 | M2-017 |

## S10

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **M2-019** | E5 | US-13 | Modelo + endpoint de check-in (data, peso, medidas, observações) | Dev | Should | 2 | — |
| [ ] | **M2-020** | E5 | US-13 | Upload de fotos de check-in para Object Storage | Dev | Should | 2 | M2-019 |
| [ ] | **M2-021** | E5 | US-13 | Front: envio de check-in (atleta) + visualização (coach) | Dev | Should | 3 | M2-020 |
| [ ] | **M2-022** | E5 | US-14 | Análise comparativa de check-ins por IA (informativa, sem ajuste automático) | Dev-Py | Should | 3 | M2-012 |
| [ ] | **M2-023** | E5 | US-13/14 | Testes de check-in + análise comparativa (não altera protocolo) | QA | Should | 2 | M2-022 |

## S11

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **M2-024** | - | MVP2 | Testes E2E integrados do fluxo IA + KB + check-ins | QA | Must | 3 | M2-023 |
| [ ] | **M2-025** | - | MVP2 | Hardening, tuning de qualidade do RAG e estabilização | Dev-Py | Must | 3 | M2-024 |
| [ ] | **M2-026** | - | MVP2 | UAT / aceite final do MVP2 | QA | Must | 2 | M2-025 |

### Resumo por Sprint — MVP2

| Sprint | Foco | Tasks | Dev/Dev-Py (dias) | QA/DevOps (dias) |
|---|---|---|---|---|
| S7 | Knowledge Base + setup RAG | 7 | 14 | 2 |
| S8 | Pipeline de ingestão de PDFs | 4 | 12 | 0 |
| S9 | IA assistiva (query + citações) | 7 | 19 | 3 |
| S10 | Check-ins + análise por IA | 5 | 10 | 2 |
| S11 | Integração + Hardening + UAT | 3 | 3 | 5 |
| TOTAL |  | 26 | 58 | 12 |