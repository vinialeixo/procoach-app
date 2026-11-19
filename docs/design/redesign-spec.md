# Spec de Repaginação Visual — ProCoach ("Dossiê de Federação")

- **Branch:** `feat/visual-redesign` (sobre `docs/impeccable-setup-and-review`)
- **Direção escolhida:** **Dossiê de Federação** (*Track & Field Federation Dossier*, `seed:9d39865fb11afc56`)
- **Esboço de referência:** [.impeccable/mocks/decision/assigned.html](../../.impeccable/mocks/decision/assigned.html)
- **Baseline de qualidade:** `21.5 / 40` (*Acceptable* em `critique`) → Meta: `32+ / 40` (*Good / Excellent*), `0` avisos no `impeccable detect` e conformidade **WCAG AA**.

---

## Problem Statement

Hoje a interface do ProCoach sofre de três problemas estruturais que reduzem a confiança e a velocidade durante consultas reais:
1. **Carga cognitiva alta nos fluxos principais:** `TrainingPage` e `DietMealsPage` empilham formulários abertos e seletores em uma única rolagem longa, enquanto `CoachHomePage` não mostra pendências clínicas.
2. **Quebra de memória operacional ("Memory Bridge"):** ao prescrever macros em `DietEditorPage`, o coach não enxerga `Peso (kg)` e `GET (kcal)` do atleta; na ficha do atleta (`AthleteDetailPage`), a aba `Treinos` não exibe o plano ativo.
3. **Inconsistência de segurança e deriva visual:** ações irreversíveis (`Publicar plano` em `PlanOverviewView` e `Remover refeição` em `DietMealsPage`) acontecem em 1 clique sem confirmação, e a área mobile do atleta (`AthleteContent.css`) foge dos tokens e não prioriza o treino do dia.

---

## Solution

Repaginar toda a aplicação (`Login`, `Coach Workbench` e `Área Mobile do Atleta`) sob o sistema visual **Dossiê de Federação**:
- **Shell com Rail Lateral (`#0F1B2D`) no Desktop + Barra Inferior no Mobile (`<640px`):** libera largura horizontal para workbenches *master-detail* em 2 colunas no desktop e coloca a navegação do atleta na zona do polegar no celular.
- **Faixa de Linhagem de Protocolo (`ProtocolLineageBar`):** ancora o topo das telas de prescrição e detalhe com versão (`v2 -> v3`), `Peso`, `GET` e variação em `JetBrains Mono`.
- **Pulso Clínico na Home do Coach:** substitui atalhos genéricos por pendências reais (dietas em rascunho/revisão, convites expirando, atletas recentes).
- **Guardrails uniformes (`P0`):** confirmação explícita antes de publicar/arquivar planos de treino ou remover refeições.

---

## User Stories

1. As a coach opening ProCoach, I want to see pending diet reviews, expiring athlete invites, and recent athletes on my home screen, so that I know what requires my clinical decision first.
2. As a coach navigating on desktop, I want a fixed left navigation rail that frees horizontal space, so that I can work comfortably in two-column training and diet workbenches.
3. As a coach reviewing an athlete's file, I want to see their active training plan summary inside the `Treinos` tab and readable coach/audit names instead of raw UUIDs, so that I never lose context mid-consultation.
4. As a coach creating or versioning a diet, I want to see the athlete's latest `Peso (kg)`, `TMB`, and `GET (kcal)` alongside live `g/kg` and `4/4/9 kcal` calculations, so that I don't have to memorize numbers from another tab.
5. As a coach building meals in `DietMealsPage`, I want a sticky macro progress bar (`Prescrito vs. Montado`) and collapsed per-meal item forms (`+ Adicionar alimento`), so that I can scan all meals without visual bloat.
6. As a coach removing a meal or publishing a training plan, I want an explicit confirmation modal/step before irreversible actions, so that an accidental click never breaks an athlete's active protocol.
7. As a coach managing periodizations and sessions in `TrainingPage`, I want a compact top context bar, modals for creating periodizations/sessions, and a master-detail split (schedule on the left, active session editor on the right), so that I can edit workouts without scrolling past empty forms.
8. As an athlete logging in on the shared `/login` screen, I want neutral copy that welcomes both coaches and athletes, so that I am confident I am in the right portal.
9. As an athlete checking my workout on mobile at the gym, I want thumb-zone navigation, readable weekday names, and a quick "Treino de hoje" / week-day selector, so that I can find my exercises in one tap.
10. As an athlete viewing my diet and assessments on mobile, I want the same crisp tabular typography (`JetBrains Mono` for loads, reps, and grams) and plain-language legends for `IMC`, `TMB`, and `GET`, so that my clinical data is effortless to read.

---

## Implementation Decisions

### 1. Design Tokens & Tipografia (`tokens.css` + `index.html`)
- **Paleta Dossiê de Federação:**
  - Fundo principal: `#F3F4F0` (*Limestone*)
  - Superfície de trabalho: `#FFFFFF` (*Pure Chalk*)
  - Rail/Shell escuro: `#0F1B2D` (*Deep Pavilion Navy*) com estado ativo `#192A44` + indicador `#B84A27`
  - Acento restrito (linhagem/estados ativos): `#B84A27` (*Track Rust*) e fundo suave `#F7EAE4`
  - Primário de ação: `#0F1B2D` (hover `#192A44`)
  - Semânticas WCAG AA: `#1F6E4A` (*Clinical Emerald* + fundo `#E3F1EA`) e `#B3261E` (*Crimson* + fundo `#FCE8E6`)
  - Bordas `1px`: `#D8DCD3` (claro) e `#2A3B55` (escuro)
- **Tipografia:** `Archivo` (600/700, títulos e métricas), `Public Sans` (400/500/600, UI e formulários), `JetBrains Mono` (400/500/600, exclusivamente para dados reais: versões, `kcal`, `g/kg`, cargas, datas).
- **Geometria:** manter escala estrita `4px` (`--radius-sm`) para controles/badges e `6px`/`8px` (`--radius-md`) para cards/painéis, sem `999px` pill cards.

### 2. Seams & Arquitetura de Componentes (`src/components/ui` + `src/layouts`)
- **`AppShell`:** migra de barra superior para **rail lateral escuro (`248px`)** no desktop e **barra inferior fixa** (`44px+` touch targets) no mobile (`<= 640px`), preservando as mesmas rotas de `COACH_NAV` e `ATHLETE_NAV`.
- **Novo componente `ProtocolLineageBar` (`src/components/ui`):** faixa reutilizável no topo de `AthleteDetailPage`, `DietEditorPage`, `DietMealsPage` e `TrainingPage` exibindo versão (`v2 -> v3`), métricas-âncora (`Peso`, `GET`) e CTA primário.
- **Novo componente `ConfirmModal` / padrão único de confirmação (`src/components/ui`):** reutilizado em `AthleteDietsPanel`, `PlanOverviewView` (`Publicar` / `Finalizar`) e `DietMealsPage` (`Remover refeição`).
- **`Button` (`Button.css`):** tornar `width: 100%` opt-in (`fullWidth?: boolean`), eliminando overrides repetidos nas páginas.
- **`AthleteNotifications.css`:** substituir `border-left: 3px` em `.athlete-notifications__item--unread` por ponto indicador + fundo sutil para zerar o aviso `side-tab` do detector.

### 3. Reestruturação por Superfície (Fases)
- **Fase 1 — Fundação & Segurança (`P0`):** atualizar `tokens.css`, `index.html`, `AppShell` (rail desktop + bottom nav mobile), `Button` e adicionar confirmações em `PlanOverviewView` e `DietMealsPage`.
- **Fase 2 — Workbench do Coach (`P1`):**
  - `CoachHomePage`: adicionar **Pulso Clínico** (dietas em revisão, convites expirando, atletas recentes) consumindo os hooks já existentes.
  - `DietEditorPage`: faixa clínica com `Peso`, `TMB`, `GET` e cálculo ao vivo de `g/kg` e `4/4/9`.
  - `DietMealsPage`: barra fixa de progresso de macros + formulário `AddMealItemForm` recolhido por refeição (`+ Adicionar alimento`).
  - `TrainingPage`: barra de contexto fixa + criação de periodização/sessão em `Modal` + layout *master-detail* 2 colunas no desktop.
- **Fase 3 — Ficha do Atleta, Login & Mobile Atleta (`P2`):**
  - `AthleteDetailPage`: resumo inline do plano ativo na aba `Treinos`, formatação `dd/mm/yyyy` + idade, nomes legíveis no `AuditLog`.
  - `AthleteContent.css` + `AthleteTrainingPage` + `AthleteAssessmentHistoryPage`: alinhar tokens/mono, usar `DAY_LABELS`, seletor rápido "Treino de hoje" e legendas de `IMC`/`TMB`/`GET`.
  - `LoginPage`: subtítulo inclusivo para coach e atleta.

---

## Testing Decisions

- **Testes unitários e de integração (`Vitest + Testing Library`):**
  - Testar comportamento externo nos novos fluxos: bloqueio de publicação/remoção sem confirmação no modal, cálculo de `g/kg` no `DietEditorPage`, expansão sob demanda de `+ Adicionar alimento` em `DietMealsPage` e seletor de treino do dia em `AthleteTrainingPage`.
  - Manter todos os mocks MSW (`src/mocks/`) sincronizados e sem alterar contratos REST do backend.
- **Acessibilidade & Detector:**
  - Garantir contraste mínimo WCAG AA (`>= 4.5:1` texto normal, `>= 3:1` texto grande/bordas de foco), navegação por teclado completa e alvos de toque `>= 44px`.
  - Rodar `impeccable detect --json frontend/src` esperando `0` avisos.
- **Build & Critique:**
  - Validar `cd frontend && npm test && npm run build && npm run lint`.
  - Re-executar `/impeccable critique` após a conclusão das fases para confirmar evolução do score.

---

## Out of Scope

- Alterações no backend Java / Spring Modulith, contratos de API REST ou regras de domínio.
- Novos recursos de IA/RAG ou aprovação automática de protocolos.
- UI kits externos (MUI, Chakra, Tailwind CSS).

---

## Further Notes

- Contrato de direção registrado em `.impeccable/surfaces/` via `impeccable surface-brief write`.
- Cada fase pode ser quebrada em tickets/PRs independentes mantendo o app sempre verde no CI.
