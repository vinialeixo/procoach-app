# ProCoach Backend — Tasks

> Tasks do núcleo Java e do serviço Python de RAG (`backend/`), incluindo infra, segurança, testes de regra de negócio e hardening.
> Backlog completo, convenções de status e contexto: [`TASKS.md` na raiz](../TASKS.md).
> Os IDs (`T-xxx`, `M2-xxx`) são globais — a coluna "Depende de" pode apontar para task de outro arquivo (`frontend/TASKS.md` ou o `TASKS.md` da raiz).
> Total neste arquivo: **89** tasks.

---

# MVP (v1.0)


## S1

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-001** | E0 | Fundação | Setup repositório Java (Clean Arch), estrutura de pastas, linters | Dev | Must | 2 | — |
| [ ] | **T-002** | E0 | Fundação | Configurar CI/CD e ambientes (dev/staging) | DevOps | Must | 2 | T-001 |
| [ ] | **T-003** | E0 | Fundação | Provisionar PostgreSQL + pgvector, Object Storage (S3), Message Broker | DevOps | Must | 2 | T-001 |
| [x] | **T-004** | E0 | Fundação | Modelar bounded contexts e definir agregados | Dev | Must | 3 | T-001 |
| [x] | **T-005** | E0 | Fundação | Implementar event store base (Event Sourcing seletivo) | Dev | Must | 4 | T-004 |
| [x] | **T-008** | E9 | US-20 | Implementar autenticação + MFA | Dev | Must | 3 | T-004 |
| [x] | **T-009** | E9 | US-20 | Implementar RBAC (papéis Coach x Atleta) | Dev | Must | 3 | T-008 |
| [x] | **T-010** | E9 | US-20 | Restringir acesso do atleta apenas aos próprios dados | Dev | Must | 2 | T-009 |
| [ ] | **T-012** | E9 | US-20 | Testes de segurança e autorização (RBAC) | QA | Must | 2 | T-010 |
| [x] | **T-013** | E10 | US-21 | Implementar audit log imutável (append-only) | Dev | Must | 3 | T-005 |
| [x] | **T-014** | E10 | US-21 | Histórico de alterações dos agregados mutáveis (exercício da biblioteca e treino): quem, quando e antes/depois no audit log. *(Redefinida em 2026-11-03: dieta e plano já versionam por cadeia própria, então o módulo `version` genérico foi descartado.)* | Dev | Must | 4 | T-005 |
| [x] | **T-015** | E10 | US-21 | Registrar eventos: cadastro, alteração, exclusão lógica, login, aprovação | Dev | Must | 3 | T-014 |
| [x] | **T-046** | E0 | Fundação | Criar módulo de domínio security + agregado User/Role (com.fitness.procoach.security) | Dev | Must | 2 | T-004 |
| [x] | **T-047** | E0 | Fundação | Criar módulo cross-cutting audit + agregado AuditLog/Version (com.fitness.procoach.audit) | Dev | Must | 2 | T-004 |
| [x] | **T-053** | E0 | Fundação | Configurar Spring Modulith + verificação de boundaries entre módulos | Dev | Must | 2 | T-004 |

## S2

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-016** | E10 | US-21 | Testes de auditoria e versionamento (não-sobrescrita) | QA | Must | 3 | T-015 |
| [x] | **T-017** | E1 | US-01 | Modelo + repositório de Atleta (ID único imutável) | Dev | Must | 2 | T-004 |
| [x] | **T-018** | E1 | US-01 | Endpoint cadastro de atleta + validação de campos obrigatórios | Dev | Must | 2 | T-017 |
| [ ] | **T-020** | E1 | US-02 | Suporte a múltiplos protocolos por atleta | Dev | Must | 2 | T-017 |
| [x] | **T-021** | E1 | US-03 | Gestão de status (Ativo/Pausado/Finalizado) + soft-delete | Dev | Must | 2 | T-017 |
| [x] | **T-048** | E1 | US-01 | Criar módulo de domínio athlete + agregado Athlete (com.fitness.procoach.athlete) | Dev | Must | 3 | T-004 |

## S3

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [x] | **T-024** | E2 | US-04 | Modelo + endpoint de avaliação física (data obrigatória, sem sobrescrever) | Dev | Must | 2 | T-017 |
| [x] | **T-025** | E2 | US-05 | Motor de cálculos automáticos (IMC, TMB, GET, evolução) | Dev | Must | 3 | T-024 |
| [x] | **T-026** | E2 | US-06 | Comparação de avaliações lado a lado (`GET /api/v1/assessments/compare?athleteId=&from=&to=`: variação `to - from`, absoluta e %, de peso, gordura, massa muscular, cintura, quadril, IMC, TMB e GET) | Dev | Should | 2 | T-024 |
| [ ] | **T-028** | E2 | US-04/05/06 | Testes dos cálculos e comparação de avaliações | QA | Must | 2 | T-027 |
| [x] | **T-029** | E3 | US-07 | Modelo de dieta + macros + objetivo obrigatório | Dev | Must | 2 | T-017 |
| [x] | **T-030** | E3 | US-07 | Cálculos de TMB/GET/déficit/superávit | Dev | Must | 2 | T-029 |
| [x] | **T-050** | E3 | US-08 | Criar módulo de domínio nutrition + agregado Diet (versionada) (com.procoach.core.nutrition) | Dev | Must | 3 | T-004 |
| [x] | **T-049** | E2 | US-05 | Criar módulo de domínio assessment + agregado Assessment (com.procoach.core.assessment) | Dev | Must | 3 | T-004 |

## S4

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [x] | **T-031** | E3 | US-08 | Versionamento de dieta (v1->v2->v3) com histórico encadeado | Dev | Must | 3 | T-014 |
| [x] | **T-032** | E3 | US-08 | Registrar quem/quando/motivo de cada alteração | Dev | Must | 2 | T-031 |
| [x] | **T-033** | E3 | US-09 | Fluxo de status (Rascunho/Em Revisão/Aprovada/Arquivada) | Dev | Must | 3 | T-031 |
| [ ] | **T-035** | E3 | US-07/08/09 | Testes de versionamento e fluxo de aprovação da dieta | QA | Must | 3 | T-034 |
| [x] | **T-036** | E8 | US-19 | Modelo + CRUD Biblioteca de Exercícios (campos científicos completos) | Dev | Must | 3 | T-004 |
| [x] | **T-052** | E8 | US-19 | Criar módulo de domínio library + agregado Exercise (com.procoach.core.library) | Dev | Must | 2 | T-004 |

## S5

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [x] | **T-037** | E4 | US-10 | Modelo de periodização + treino (séries/reps/carga/intensidade/descanso) | Dev | Must | 3 | T-036 |
| [ ] | **T-038** | E4 | US-11 | Criação de exercícios personalizados pelo coach | Dev | Should | 2 | T-036 |
| [ ] | **T-039** | E4 | US-12 | Sugestão de exercícios semelhantes | Dev | Could | 2 | T-036 |
| [ ] | **T-041** | E4/E8 | US-10/11/12/19 | Testes de treino e biblioteca | QA | Must | 3 | T-040 |
| [x] | **T-051** | E4 | US-12 | Criar módulo de domínio training + agregado TrainingPlan (com.procoach.core.training) | Dev | Must | 3 | T-004 |

## S6

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **T-043** | - | MVP | Revisão LGPD (criptografia, soft-delete, consentimento) | Dev | Must | 2 | T-041 |
| [ ] | **T-044** | - | MVP | Hardening, correção de bugs e estabilização | Dev | Must | 3 | T-043 |

### Resumo por Sprint — MVP

| Sprint | Foco | Tasks | Estimativa Dev (dias) | QA/DevOps (dias) |
|---|---|---|---|---|
| S1 | Fundação + Segurança + Auditoria | 15 | 33 | 6 |
| S2 | Atletas + Auditoria | 6 | 11 | 3 |
| S3 | Avaliação + Nutrição (início) | 8 | 17 | 2 |
| S4 | Nutrição versionada + Biblioteca | 6 | 13 | 3 |
| S5 | Treino + Biblioteca | 5 | 10 | 3 |
| S6 | Integração + Hardening + UAT | 2 | 5 | 0 |
| TOTAL |  | 42 | 89 | 17 |


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
| [x] | **T-057** | E0 | Fundação | Backend: auditoria de conformidade dos agregados vs UML (ProCoach_System_Design_UML.md seção 4/12.1) — sem escrever fix ainda, só relatório | Dev | Must (sugerido) | 1 | T-046, T-048, T-049, T-050, T-051, T-052 |
| [x] | **T-058** | E0/E1 | Fundação | Backend: criar agregado Coach (com.procoach.core.coach) — id, userId, name, status, addAthlete()/removeAthlete(); migrar coachId em Athlete/Assessment/Diet/TrainingPlan de User.id para Coach.id | Dev | Must (confirmado) | 3 | T-057
| [ ] | **T-059** | E9 | US-20 | Backend: reintroduzir MFA/TOTP endurecido — religar `procoach.security.mfa.enabled`; limite de tentativas + rate limit; anti-replay do código TOTP; segredo TOTP cifrado; códigos de recuperação | Dev | Should | 4 | T-008 |
| [ ] | **T-061** | E2 | US-05 | Backend: nível de atividade do atleta (fator do GET) — hoje a T-025 usa fator sedentário 1,2 fixo; guardar o fator por atleta/avaliação e usá-lo no cálculo do GET (e reaproveitar na T-030) | Dev | Should (sugerido) | 2 | T-025 |
| [x] | **T-062** | E9 | US-20 | Backend: convite do atleta (primeira credencial) — coach gera link de uso único (7 dias, só o hash do token é guardado); atleta consulta e aceita sem autenticação, define a senha e abre a sessão; um acesso por atleta | Dev | Must (sugerido) | 3 | T-008, T-017 |
| [ ] | **T-064** | E9 | US-20 | Backend: envio do convite do atleta por e-mail (hoje o coach copia o link e repassa) e reenvio | Dev | Should (sugerido) | 2 | T-062 |
| [x] | **T-066** | E8 | US-19 | Backend: exercício da biblioteca versionado — editar cria nova versão (v2, v3…) em vez de sobrescrever, para que treinos já prescritos não mudem retroativamente; listagens mostram só a versão corrente | Dev | Should (sugerido) | 3 | T-014, T-036 |
| [x] | **T-067** | E4 | US-10/US-12 | Backend: sessões de treino ligadas à versão do plano — cada sessão pertence a uma versão do plano (`planId`, obrigatório na criação); criar a v2 copia as sessões da v1 (em rascunho, prescrições com ids novos) e deixa a v1 intacta; ativar a v2 publica as sessões novas, encerra a v1 e as sessões dela como histórico (FINISHED) e avisa o atleta | Dev | Should (sugerido) | 4 | T-037, T-066 |
| [x] | **T-068** | E4 | US-10 | Backend: editar prescrição de exercício já criada (séries, repetições, carga, intensidade, descanso, nome, observações) com antes/depois no audit log (`TRAINING_EXERCISE_UPDATED`); inclui remover prescrição (`TRAINING_EXERCISE_REMOVED`) e trocar pela versão corrente do exercício da biblioteca. *(Ampliada em 2026-11-03: vale também para treino ativo, com aviso ao atleta — ver T-070.)* | Dev | Should (sugerido) | 2 | T-014, T-037 |
| [x] | **T-070** | E4/E3 | US-38 | Backend: avisos in-app ao atleta — o coach edita (adiciona, ajusta, remove ou reordena) um treino ativo ou aprova uma dieta e o atleta recebe um aviso na mesma transação; o atleta lista, marca como lido e marca todos (`/api/v1/notifications`); treino em rascunho ou revisão não avisa | Dev | Must (sugerido) | 3 | T-068 |
| [x] | **T-071** | E4/E3 | US-38 | Backend: canais externos do aviso ao atleta — e-mail (SMTP, desligado por padrão) e push (porta `PushGateway`), com preferência por canal, registro de dispositivos, rastro de entrega e repetição das falhas (até 3 tentativas); entrega só depois do commit e fora da requisição do coach | Dev | Should (sugerido) | 3 | T-070 |
| [ ] | **T-076** | E4/E3 | US-38 | Backend: provedor de push de verdade (FCM/APNs/Web Push) como implementação de `PushGateway`, com credenciais por ambiente e remoção de tokens inválidos; hoje só existe o provedor `log` | Dev | Should (sugerido) | 2 | T-071 |
| [x] | **T-069** | E4/E8 | US-19 | Backend: aviso de versão mais nova do exercício nas prescrições — cada prescrição de exercício da biblioteca traz `exerciseVersion`, `newerVersionAvailable` e `currentExerciseId` (segue a cadeia até a versão corrente; sem aviso se a corrente foi removida); a prescrição continua apontando para a versão que o coach usou | Dev | Should (sugerido) | 1 | T-066 |
| [x] | **T-065** | E1/E3/E4 | US-38 | Backend da área do atleta (T-054/T-055): leitura somente leitura filtrada — atleta só vê a dieta APROVADA (versão aprovada mais recente), planos/treinos/periodizações ACTIVE ou FINISHED e o balanço da dieta aprovada; registros removidos não aparecem para ninguém; avaliações com `metrics` já eram acessíveis ao atleta | Dev | Must (sugerido) | 2 | T-062 |
| [ ] | **T-077** | E9 | US-20 | Backend: visibilidade de acesso do atleta — `GET /api/v1/athletes` e `/{id}` retornam `hasCredential` (User existe) e `pendingInvite` (convite ativo com expiração); listagem de convites do atleta e revogação de convite (audit `ATHLETE_INVITE_REVOKED`); base para os badges "Tem acesso / Convite pendente / Sem acesso" do front (T-082). *(Análise PO 2026-11-11 — fluxo /coach/athletes, GitHub #48.)* | Dev | Must (sugerido) | 2 | T-062 |
| [ ] | **T-078** | E1/E9 | US-01 | Backend: e-mail opcional no cadastro do atleta para dedupe — `POST /api/v1/athletes` aceita `email?`; se o e-mail já pertencer a um User, responder 409 acionável (`EMAIL_ALREADY_REGISTERED`) em vez de deixar o erro aparecer só no convite; e-mail continua opcional (compatibilidade). *(Análise PO 2026-11-11 — GitHub #49.)* | Dev | Should (sugerido) | 2 | T-018, T-062 |
| [ ] | **T-079** | E9 | US-20 | Backend: 409 do convite acionável — distinguir "atleta já tem credencial" (`ATHLETE_HAS_CREDENTIAL`) de "e-mail já registrado de outro usuário" (`EMAIL_IN_USE`) com códigos e mensagens próprios, para o front mapear orientação clara. *(Análise PO 2026-11-11 — GitHub #50.)* | Dev | Should (sugerido) | 1 | T-062 |
| [ ] | **T-080** | E1/E9 | US-01 | Backend: vínculo coach↔atleta explícito — decidir e documentar o modelo (ex.: `CoachAthleteLink` com status PENDING/ACTIVE/FINISHED) ou formalizar o `coach_id` como vínculo único; emitir `AthleteAddedToRosterEvent` no `register` (hoje o evento existe e nunca é emitido); avaliar multi-coach (mesma pessoa com coaches diferentes / transferência). *(Análise PO 2026-11-11 — GitHub #51.)* | Dev | Could (sugerido) | 3 | T-058 |
| [ ] | **T-081** | - | MVP | Backend: documentar o fluxo de convite/credencial no SPEC.md e USER_STORIES.md — convite (token de uso único, TTL 7 dias, hash), aceite (senha + auto-login), regras de duplicidade e revogação não têm RF/US hoje. *(Análise PO 2026-11-11 — GitHub #52.)* | Doc | Should (sugerido) | 1 | — |
| [ ] | **T-085** | E3 | US-07/08 | Backend: modelo de refeições e itens de alimento na dieta versionada — `Meal`/`MealItem` no agregado `Diet`, totais derivados dos itens, snapshot de macros, migração e testes. *(Análise PO/Nutrição 2026-11-11 — GitHub #56, `docs/nutricao-plano-alimentar.md`.)* | Dev | Must (sugerido) | 5 | T-029, T-031, T-050 |
| [ ] | **T-086** | E3 | US-07 | Backend: catálogo de alimentos TACO — macros por 100 g, porção comestível, medida caseira, estado `RAW`/`AS_SERVED`, busca paginada e alimento `CUSTOM` por coach. *(Análise PO/Nutrição 2026-11-11 — GitHub #57, `docs/nutricao-plano-alimentar.md`.)* | Dev | Must (sugerido) | 4 | T-085 |
| [ ] | **T-087** | E3 | US-07/14 | Backend: macros zeráveis (`>= 0`) + `warnings[]` estruturados + consistência 4/4/9 + derivados (g/kg, %kcal, kcal/kg) + unificar taxonomia de objetivos com `RECOMPOSITION`. *(Análise PO/Nutrição 2026-11-11 — GitHub #58, `docs/nutricao-plano-alimentar.md`.)* | Dev | Must (sugerido) | 3 | T-029, T-030 |
| [ ] | **T-088** | E3/E6 | US-15/16 | Backend: contrato JSON v1 do plano alimentar gerado por IA — validação/dry-run, persistência apenas como rascunho consultivo (`requiresCoachApproval`), auditoria `AI_PLAN_PROPOSED`, nunca aprova sozinho. *(Análise PO/Nutrição 2026-11-11 — GitHub #59, `docs/nutricao-plano-alimentar.md`.)* | Dev | Should (sugerido) | 3 | T-085, T-087 |
| [ ] | **T-089** | E3 | US-07/38 | Backend: balanço energético da versão corrente + corrigir regressão do atleta ao arquivar (mais alta `ARCHIVED` responde 404 em vez de voltar para uma `APPROVED` anterior); 409 acionável sem avaliação. *(Análise PO/Nutrição 2026-11-11 — GitHub #60, `docs/nutricao-plano-alimentar.md`.)* | Dev | Must (sugerido) | 2 | T-030, T-065 |
| [ ] | **T-090** | E3 | US-07/16 | Backend: suplementos em seção própria (`countsTowardMacros`; Whey como item calórico) + auditoria legível do autor (nome/e-mail em vez de UUID) no histórico da dieta. *(Análise PO/Nutrição 2026-11-11 - GitHub #61, `docs/nutricao-plano-alimentar.md`.)* | Dev | Should (sugerido) | 2 | T-013, T-085 |
| [ ] | **T-096** | E1/E9 | US-01/US-20 | Backend: decidir e implementar a exposição do e-mail do atleta — hoje o `email?` do cadastro (T-078) só serve para dedupe e não é persistido, então `AthleteResponse` não tem `email` e o front não pré-preenche o convite. Persistir (com migração + testes) ou remover a dependência e documentar. *(QA 2026-11-12 — integração PR #121/#118, GitHub #122.)* | Dev | Should (sugerido) | 1 | T-077, T-078 |

> `T-057`/`T-058` identificadas em 2026-09-02: nenhuma task original cria o agregado
> `Coach` (`com.procoach.core.coach`), apesar do UML exigir `User "1" --> "1" Coach` e
> o `Coach` (não o `User`) ser o dono de `addAthlete()`/`removeAthlete()` e autor de
> Diet/TrainingPlan/Assessment. **Confirmado com o PO em 2026-09-02**: o agregado deve
> existir — não é ambiguidade de escopo, é lacuna de planejamento. `T-057` mapeia o
> estado atual (relatório); `T-058` cria o agregado e migra as referências. Ainda sem
> dado de produção — é troca de modelo, não migração de dado real.
>
> **Achado de segurança (relatório T-057, 2026-09-02)**: hoje `coachId` em
> Athlete/Assessment/Diet/TrainingPlan vem cru do corpo da requisição, sem validação
> contra o usuário autenticado — qualquer coach logado pode gravar dados em nome de
> qualquer UUID (falha de autorização tipo IDOR, não só divergência de modelo).
> `T-058` corrige isso junto (`coachId` passa a ser resolvido server-side a partir do
> JWT, não mais aceito do cliente) — tratar como prioridade alta mesmo em dev.

> **MFA/TOTP desligado em 2026-11-03** (decisão do time, backlog `T-059`/`T-060`): o login passa a
> exigir só e-mail e senha. No backend os endpoints `/api/v1/auth/mfa/*` respondem 404 e o `/login` ignora o MFA da conta.
> O código do TOTP continua no repositório atrás da flag `procoach.security.mfa.enabled`
> (padrão `false`; `PROCOACH_MFA_ENABLED`), então reintroduzir não parte do zero. Pendências de
> segurança a resolver **antes** de religar (achados da revisão de código): limite de tentativas por
> desafio e rate limit em `/login` e `/mfa/verify`; proteção contra reuso do mesmo código TOTP
> (guardar o último passo aceito); segredo TOTP cifrado no banco (AES-GCM, chave via ambiente);
> códigos de recuperação.

> **Auditoria ligada em 2026-11-03** (T-013/T-015): cadastro e exclusão lógica do atleta, mudança de status
> (com motivo), avaliação, dieta (criação, nova versão, mudança de status/aprovação), periodização, plano e
> treino (criação, versão, status, exclusão), convite do atleta, primeiro acesso e login passam pelo
> `AuditService`, na mesma transação da mudança. O coach dono consulta o histórico do atleta em
> `GET /api/v1/athletes/{id}/history` (mais recente primeiro). O banco rejeita UPDATE, DELETE e TRUNCATE em
> `audit_log` (trigger da migração `V802`, específica do Postgres — não exercitada pelos testes em H2).
> `T-014` (versionamento genérico de agregados) segue em aberto: dieta e plano já versionam por cadeia
> própria (`previousVersionId`), então o módulo `version` genérico ainda não tem uso — decidir se a task é
> dada como coberta ou redefinida. `T-043` (LGPD) pode partir deste histórico.
> **T-014 redefinida (2026-11-03)**: adicionar e reordenar exercícios do treino (`TRAINING_EXERCISE_ADDED`,
> `TRAINING_EXERCISES_REORDERED`, com a ordem antes/depois) e criar, editar e remover exercício da biblioteca
> (`EXERCISE_CREATED/UPDATED/DELETED`, com os campos alterados e antes/depois de nome, equipamento e grupos
> musculares) entram no audit log. Observações livres do treino ficam de fora do histórico. A edição do
> exercício deixou de sobrescrever com a `T-066`: editar cria uma nova versão (id novo, `version`+1,
> `previousVersionId`), a anterior fica `superseded` e legível por id, e as buscas mostram só a versão
> corrente; o evento passa a ser `EXERCISE_VERSIONED`. Migração `V803` (Postgres, não exercitada em H2).
> `T-069` avisa, em cada prescrição do treino, quando o exercício da biblioteca já tem versão mais nova
> (`newerVersionAvailable` + `currentExerciseId`). Trocar a prescrição para a versão corrente depende da
> `T-068` (editar prescrição).
>
> **Modelo de treino hoje**: a periodização (ex.: ciclo de 3 meses) tem id, datas e status, mas não contém
> exercícios; as sessões de treino (semana/dia) guardam as prescrições (séries, repetições, carga…); o plano
> versiona só nome e descrição e não aponta para as sessões. Por isso o histórico é por sessão e por atleta e
> não há instantâneo do que valia em cada semana — `T-067` liga as sessões à versão do plano e `T-068` permite
> editar prescrições com antes/depois.
>
> **Regra de edição do treino mudou (2026-11-03)**: o treino **ativo** agora aceita adicionar, ajustar, remover e
> reordenar exercícios (antes só rascunho e revisão); treino finalizado ou removido continua recusando (409). Como o
> atleta já enxerga o treino ativo, cada alteração gera o aviso `TRAINING_CHANGED` para ele e fica no histórico com
> antes/depois. Aprovar uma dieta gera `DIET_APPROVED`. Os avisos são in-app (`T-070`) e também saem por e-mail e push (`T-071`).
>
> **Sessões ligadas à versão do plano (T-067, 2026-11-03)**: `POST /api/training/trainings` agora exige `planId` (plano do
> mesmo atleta e da mesma periodização; versão finalizada recusa com 409) e `GET /trainings/athlete/{id}` aceita
> `?planId=`. Nova versão do plano copia as sessões; ativar um plano publica as suas sessões (rascunho e revisão viram
> ACTIVE), encerra as versões anteriores ainda ativas (plano e sessões FINISHED, intactos) e avisa o atleta
> (`TRAINING_PLAN_PUBLISHED`). A edição no lugar (`T-068`) segue valendo para ajustes pequenos; a nova versão serve para
> revisões maiores. Migração `V805` (Postgres) liga as sessões já existentes ao plano ativo ou mais recente da
> periodização — não exercitada em H2.
>
> **Canais externos do aviso (T-071, 2026-11-03)**: depois do commit da ação do coach, cada aviso é entregue em um
> executor próprio pelos canais disponíveis, respeitando a preferência do atleta (`GET/PUT /api/v1/notifications/preferences`,
> padrão: tudo ligado; o aviso no app sempre existe). E-mail por SMTP usa `spring.mail.*` e fica desligado até
> `procoach.notification.email.enabled=true` (`PROCOACH_EMAIL_ENABLED`); o destino é o e-mail de acesso do atleta. Push usa
> `POST/DELETE /api/v1/notifications/devices` e só funciona com um provedor (`procoach.notification.push.provider`:
> `none` por padrão, `log` só registra no log; FCM/APNs entram em `T-076`). Cada tentativa fica em `notification_delivery`
> (SENT, FAILED, SKIPPED sem destino) e as falhas são repetidas a cada 5 min até 3 tentativas. Falha de canal nunca
> derruba a ação do coach. Migração `V806` (Postgres), não exercitada em H2.

> **Épico E9 / US-20 concluído em 2026-09-04** (T-008 a T-011): autenticação JWT
> (AccessToken + RefreshToken), MFA/TOTP (setup/verify/confirm), RBAC
> (`ROLE_COACH`/`ROLE_ATHLETE` via `@PreAuthorize`) e isolamento de dados por perfil
> (`@athleteGuard.canAccess` — atleta só acessa o próprio `athleteId`; coach acessa a
> carteira) — tudo encapsulado em `com.fitness.procoach.security`. Front (`procoach-front`)
> integrado aos endpoints reais: login em 2 etapas, ativação de MFA com QR Code,
> `Authorization: Bearer` no `api-client` e redirecionamento por papel
> (COACH → `/coach`, ATHLETE → `/athlete`). Gate `.\mvnw.cmd clean test` verde
> (178 testes, 0 falhas) e `npm run build`/`npm run lint` limpos no front.

---

# MVP2 (v1.1 — Knowledge Base, RAG, Check-ins)


## S7

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **M2-001** | E7 | US-18 | Modelo de conceito (definição, categoria, referências, nível de evidência) | Dev | Must | 2 | — |
| [ ] | **M2-002** | E7 | US-18 | Versionamento e arquivamento de conceitos (sem exclusão) | Dev | Must | 2 | M2-001 |
| [ ] | **M2-003** | E7 | US-18 | CRUD + API da Knowledge Base | Dev | Must | 2 | M2-002 |
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
| [ ] | **M2-018** | E6 | US-15/16/17 | Testes de IA: citações, guardrail read-only, aceite/recusa | QA | Must | 3 | M2-017 |

## S10

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **M2-019** | E5 | US-13 | Modelo + endpoint de check-in (data, peso, medidas, observações) | Dev | Should | 2 | — |
| [ ] | **M2-020** | E5 | US-13 | Upload de fotos de check-in para Object Storage | Dev | Should | 2 | M2-019 |
| [ ] | **M2-022** | E5 | US-14 | Análise comparativa de check-ins por IA (informativa, sem ajuste automático) | Dev-Py | Should | 3 | M2-012 |
| [ ] | **M2-023** | E5 | US-13/14 | Testes de check-in + análise comparativa (não altera protocolo) | QA | Should | 2 | M2-022 |

## S11

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [ ] | **M2-025** | - | MVP2 | Hardening, tuning de qualidade do RAG e estabilização | Dev-Py | Must | 3 | M2-024 |

### Resumo por Sprint — MVP2

| Sprint | Foco | Tasks | Dev/Dev-Py (dias) | QA/DevOps (dias) |
|---|---|---|---|---|
| S7 | Knowledge Base + setup RAG | 6 | 11 | 2 |
| S8 | Pipeline de ingestão de PDFs | 4 | 12 | 0 |
| S9 | IA assistiva (query + citações) | 6 | 15 | 3 |
| S10 | Check-ins + análise por IA | 4 | 7 | 2 |
| S11 | Integração + Hardening + UAT | 1 | 3 | 0 |
| TOTAL |  | 21 | 48 | 7 |
