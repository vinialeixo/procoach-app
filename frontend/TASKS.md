# ProCoach Frontend — Tasks

> Tasks da SPA (`frontend/`): telas, design system e tooling do front.
> Backlog completo, convenções de status e contexto: [`TASKS.md` na raiz](../TASKS.md).
> Os IDs (`T-xxx`, `M2-xxx`) são globais — a coluna "Depende de" pode apontar para task de outro arquivo (`backend/TASKS.md` ou o `TASKS.md` da raiz).
> Total neste arquivo: **19** tasks.

---

# MVP (v1.0)


## S1

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [x] | **T-006** | E0 | Fundação | Setup projeto Front Web (SPA), design system base | Dev | Must | 2 | — |
| [x] | **T-011** | E9 | US-20 | Front: telas de login/registro/MFA | Dev | Must | 2 | T-008 |

## S2

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [x] | **T-019** | E1 | US-01 | Front: formulário de cadastro de atleta | Dev | Must | 2 | T-018 |
| [x] | **T-022** | E1 | US-03 | Front: listagem/detalhe do atleta + troca de status | Dev | Must | 2 | T-021 |

## S3

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [x] | **T-027** | E2 | US-04/05/06 | Front: avaliação física + gráficos de evolução + comparação | Dev | Must | 3 | T-025, T-026 (comparação lado a lado — **pronta no backend**: `GET /api/v1/assessments/compare?athleteId=&from=&to=`) |

## S4

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [x] | **T-034** | E3 | US-07/08/09 | Front: editor de dieta + versões + aprovação | Dev | Must | 4 | T-033 |

## S5

| Status | Task ID | Épico | User Story | Task | Tipo | Prioridade | Estimativa (d) | Depende de |
|---|---|---|---|---|---|---|---|---|
| [x] | **T-040** | E4/E8 | US-10/11/19 | Front: biblioteca + montagem de treino/periodização | Dev | Must | 4 | T-037 |

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
| [x] | **T-054** | E1/E3/E4 | US-38 | Front: área do atleta — visualizar dieta e treino atuais (somente leitura) | Dev | Must (sugerido) | 3 | T-033, T-037, T-065, T-067 |
| [x] | **T-055** | E2 | US-38 | Front: área do atleta — histórico de avaliações (somente leitura) | Dev | Should (sugerido) | 2 | T-025, T-065 |
| [x] | **T-056** | E0 | Fundação | Front: configurar ESLint (config ausente desde T-006) | Dev | Must (sugerido) | 0.5 | T-006 |
| [ ] | **T-060** | E9 | US-20 | Front: reintroduzir MFA — etapa 2 no login e tela de ativação com QR Code (recuperar `MfaSetupPage`, `useVerifyMfa`, `useStartMfaSetup`, `useConfirmMfaSetup` e a etapa de código do `LoginPage` do histórico do git; `qrcode` segue no `package.json`) | Dev | Should | 2 | T-059 |
| [x] | **T-072** | E4/E3 | US-38 | Front: avisos do atleta — sino com contagem de não lidos, lista de avisos e marcar como lido (`/api/v1/notifications`; só o atleta acessa) | Dev | Should (sugerido) | 3 | T-070, T-054 |
| [x] | **T-073** | E4 | US-10 | Front: coach edita e remove prescrições do treino (inclusive ativo), com o aviso "há versão mais nova" do exercício e botão para trocar pela versão corrente | Dev | Should (sugerido) | 3 | T-068, T-069, T-040 |
| [x] | **T-074** | E4 | US-10/US-12 | Front: versões do plano de treino — o coach cria a nova versão (que copia as sessões), compara e publica; o atleta vê as sessões da versão corrente e o histórico das anteriores (`planId` nas sessões; `?planId=` na listagem) | Dev | Should (sugerido) | 4 | T-067, T-040 |
| [ ] | **T-075** | E4/E3 | US-38 | Front: preferências de aviso do atleta (e-mail e push) e registro do dispositivo para push (`GET/PUT /api/v1/notifications/preferences`, `POST/DELETE /api/v1/notifications/devices`) | Dev | Should (sugerido) | 2 | T-071, T-072 |
| [x] | **T-063** | E9 | US-20 | Front: convite do atleta — botão "Convidar para o app" na ficha (gera e copia o link) e tela pública /convite/:token para o atleta definir a senha | Dev | Must (sugerido) | 2 | T-062 |

> **MFA/TOTP desligado em 2026-11-03** (decisão do time, backlog `T-059`/`T-060`): o login passa a
> exigir só e-mail e senha. No front foram removidos a etapa de código do login, a tela `/registro/mfa` e os hooks de MFA; o cadastro agora vai direto para a área do coach.
> O código do TOTP continua no repositório atrás da flag `procoach.security.mfa.enabled`
> (padrão `false`; `PROCOACH_MFA_ENABLED`), então reintroduzir não parte do zero. Pendências de
> segurança a resolver **antes** de religar (achados da revisão de código): limite de tentativas por
> desafio e rate limit em `/login` e `/mfa/verify`; proteção contra reuso do mesmo código TOTP
> (guardar o último passo aceito); segredo TOTP cifrado no banco (AES-GCM, chave via ambiente);
> códigos de recuperação.

> `T-054`/`T-055` — o backend já está pronto (backlog `T-065`): o atleta autenticado consulta `GET /api/v1/diets` e `/diets/history`, `GET /api/v1/diets/energy-balance?athleteId=`, `GET /api/training/plans/athlete/{id}[/latest]`, `GET /api/training/trainings/athlete/{id}`, `GET /api/v1/assessments?athleteId=` (com `metrics`). O atleta só recebe conteúdo liberado pelo coach (dieta aprovada, plano e treino ativos); sem dieta aprovada o `GET /diets` responde 404.

> **Alinhamento front × back (proposta do backend, aguardando confirmação do time de front)**
> - `T-027`: a comparação lado a lado depende da `T-026` (backend, **concluída**: `GET /api/v1/assessments/compare?athleteId=&from=&to=` devolve `from`, `to`, `daysBetween` e, por medida, `from/to/change/changePercent`; 400 se as duas forem a mesma, 404 se não forem do atleta). `T-061` (fator de atividade) **não** altera o aceite: hoje o GET usa 1,2 e o front só exibe o valor devolvido; planejar à parte.
> - `T-040`: escopo mínimo = só o que o backend já oferece (biblioteca de exercícios, planos, sessões com `planId`, periodização, prescrição). `T-038` (exercício personalizado) e `T-039` (semelhantes) ficam **fora**. Biblioteca como consulta dentro do editor de treino. Módulo `src/lib/training.ts` concentra URLs, DTOs e versionamento. `T-073`/`T-074` vêm depois.
> - Treino: toda alteração de plano cria nova versão (`previousVersionId`); versões antigas seguem consultáveis; sessão pertence a uma versão (`planId`, `?planId=`); prescrição pode ser editada/removida até com o treino ACTIVE (o atleta é avisado); transição inválida responde 409.
> - Estados do atleta (`T-054`/`T-055`): conteúdo disponível; sem conteúdo (listas vazias); dieta não aprovada (`GET /diets` → 404, tratar como vazio); 401 sessão expirada; 403 acesso negado. 404 nunca é autorização.
> - `T-072`: usar só a API in-app (`/api/v1/notifications`); preferências e dispositivos são a `T-075`. E-mail e push (`T-071`) já existem no backend; a entrega é transparente para o front.
> - MFA: SPEC atualizado; o front segue com e-mail e senha.
> - MVP2 (`M2-004`, `M2-017`, `M2-021`): sem backend (M2-003, M2-014, M2-019/020 abertas); não iniciar telas antes dos contratos. Prioridade sugerida: `T-040` → `T-054`/`T-055` → `T-072` → `T-073`/`T-074` → `T-075` → MVP2.

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

---

## Changelog de status — revisão Dev/QA (2026-11-10)

- **T-027 → `[x]`**: painel de avaliações entregue em `AthleteAssessmentsPanel.tsx`
  (lista cronológica, gráfico de evolução com Recharts e comparação lado a lado de 2+
  avaliações com destaque das diferenças) + formulário em `AssessmentCreatePage.tsx` e
  contrato em `src/lib/assessments.ts`. `T-025`/`T-026` concluídas no backend. *Observação
  do QA:* a comparação é montada no front a partir da listagem, e não pela chamada a
  `GET /assessments/compare`; os critérios de US-009 são atendidos, mas vale avaliar a troca
  para o endpoint em uma melhoria futura (evita divergência de cálculo).
- **T-034 → `[x]`**: editor de dieta (`DietEditorPage.tsx`), painel de versões
  (`AthleteDietsPanel.tsx`), criação de v1/nova versão com motivo obrigatório e fluxo de
  status/aprovação (`DIET_STATUS_FLOW` + `useChangeDietStatus`) entregues; `T-033` concluída.
- **T-073 → `[x]`**: `TrainingPage` agora edita (modal com série, reps, carga,
  intensidade, descanso, nome e observações) e remove prescrições, inclusive em sessão
  **ACTIVE**; em `FINISHED` as ações ficam desabilitadas, como no backend (409). O aviso
  "há versão mais nova" aparece por prescrição e o botão "Trocar pela versão atual" busca
  o exercício corrente (`currentExerciseId`) e reenvia a prescrição. Hooks em
  `src/lib/training.ts` cobrem `PUT/DELETE /api/training/trainings/{id}/exercises/{prescriptionId}`.
  Testes novos em `src/lib/training.test.ts` (payload de edição/troca de versão).
- **T-074 → `[x]`**: `TrainingPage` ganhou "Criar nova versão" (modal com nome/descrição,
  aviso de que as sessões são copiadas e a versão nasce em rascunho), "Comparar com vN"
  (modal lado a lado com sessões e exercícios adicionados/removidos/alterados) e o fluxo de
  publicação (`DRAFT → IN_REVIEW → ACTIVE`, com textos específicos de versão). A versão de
  origem permanece intacta, conforme a regra de nunca sobrescrever. `AthleteTrainingPage`
  passou a exibir a versão corrente e o histórico das anteriores (somente leitura, sessões
  carregadas por `planId`). Helpers em `src/lib/training.ts`: `sortPlanVersions`,
  `currentPlanVersion`, `previousPlanVersions` e `comparePlanVersions`, com testes em
  `src/lib/training.test.ts`.
- **Correção de contagem**: o cabeçalho dizia 13 tasks; o arquivo tem 19 (7 MVP + 9 do
  adendo + 3 MVP2), alinhado ao `TASKS.md` da raiz.
- **Mantidas em `[ ]`**: `T-060` (depende de `T-059`, backend ainda aberto e MFA desligado
  por decisão do time) e `T-075` (não há telas/hooks de preferências e dispositivos de push).
  `M2-004`, `M2-017` e `M2-021` seguem bloqueadas pelas dependências de backend abertas
  (`M2-003`, `M2-014`, `M2-020`).
