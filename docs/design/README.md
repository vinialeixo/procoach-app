# Design System & Revisão Visual (Impeccable)

Este documento reúne o setup do **Impeccable** no ProCoach, o diagnóstico da revisão visual atual (`critique`) e as propostas práticas de evolução da experiência para as features em construção.

---

## 1. Setup do Impeccable no Repositório

| Arquivo | Papel | Deve ser commitado? |
| :--- | :--- | :--- |
| [PRODUCT.md](../../PRODUCT.md) | Verdade durável do produto (usuários, posicionamento, contexto operacional, invariantes *coach-led* e versionamento imutável) | Sim |
| [DESIGN.md](../../DESIGN.md) | Especificação visual canônica (*"O Ledger Clínico"*): paleta, tipografia, layout, elevação, formas, componentes e regras | Sim |
| [.impeccable/config.json](../../.impeccable/config.json) | Preferência de fluxo de construção (`buildPath: "code"`) | Sim |
| [.impeccable/live/config.json](../../.impeccable/live/config.json) | Configuração do modo `/impeccable live` para `frontend/index.html` | Sim |
| [.impeccable/design.json](../../.impeccable/design.json) | Sidecar v2 com rampas tonais OKLCH, sombras, transições, breakpoints e 7 snippets HTML/CSS para o painel Live | Sim |
| `.impeccable/critique/` | Snapshots locais de `critique` (já possui `.gitignore` próprio) | Não |

### Comandos úteis
- `/impeccable live` — abrir edição visual interativa no navegador sobre `frontend/`.
- `/impeccable critique` — rodar nova avaliação após implementar melhorias e acompanhar a evolução da pontuação.
- `/impeccable polish` — passe final de qualidade antes de fechar uma entrega.

---

## 2. Diagnóstico Atual (`frontend/src`) — Nota `21.5 / 40` (*Acceptable*)

Avaliação realizada em modo dual-agent (revisão de design + detector estático `impeccable detect --json frontend/src`).

### Veredito de Especificidade ("O Ledger Clínico")
- **Onde funciona bem:** `AuthLayout.tsx` (painel escuro com histórico de versões), `DietEditorPage.tsx` (linhagem `v1 -> v2` + base congelada) e `AthleteDietsPanel.tsx` (diff transposto + balanço energético vs. `GET`) traduzem com clareza a proposta de ledger clínico imutável.
- **Onde deriva para CRUD genérico:** `CoachHomePage.tsx` e `AthleteHomePage.tsx` são grades vazias de atalhos sem pulso clínico; `TrainingPage.tsx` empilha formulários e selects como um painel administrativo de banco; e `AthleteContent.css` foge dos tokens do `DESIGN.md` (`12px`/`999px` de raio, sem `--shadow-card` e sem `IBM Plex Mono`).
- **Scan determinístico:** apenas **1 aviso** (`side-tab` em `frontend/src/components/AthleteNotifications.css:77` para `.athlete-notifications__item--unread`), um falso positivo contextual que pode ser trocado por um indicador discreto de não lido.

### Pontuação por Heurística de Nielsen

| # | Heurística | Nota (/4) | Principal Gargalo |
|---|-----------|-----------|-------------------|
| 1 | Visibilidade do Status | 2.5 | `TrainingPage` / `PlanOverviewView` sem feedback pós-transição; loading em `<p>` simples; `DietMealsPage` sem barra de progresso de macros |
| 2 | Correspondência com o Mundo Real | 2.5 | `LoginPage` fala em "conta de coach" em rota compartilhada; `AthleteTrainingPage` mostra `"Semana 1 · dia 3"`; UUIDs crus na auditoria |
| 3 | Controle e Liberdade | 2.0 | `AssessmentCreatePage` sem Cancelar; forms inline fixos em `TrainingPage`; `DietMealsPage` remove refeição/item sem confirmação |
| 4 | Consistência e Padrões | 2.0 | `AthleteContent.css` fora dos tokens (`12px`/`999px`); estados de erro alternando entre `StateMessage` e `<p>` |
| 5 | Prevenção de Erros | 2.5 | `PlanOverviewView` publica plano em 1 clique sem modal de confirmação; `DietMealsPage` apaga refeição em 1 clique |
| 6 | Reconhecimento vs. Memorização | 2.0 | `DietEditorPage` esconde `Peso` e `GET` do atleta; `AthleteDetailPage` esconde status ativo de dieta/treino dentro das abas |
| 7 | Flexibilidade e Eficiência | 1.5 | Sem busca rápida de atleta; `TrainingPage` exige 3 selects sequenciais toda vez que abre; sem duplicar refeição |
| 8 | Estética e Minimalismo | 2.5 | `DietMealsPage` repete form de 11 campos aberto em cada refeição; `CoachHomePage` subutilizada |
| 9 | Recuperação de Erros | 2.5 | Mutação cai em mensagem genérica sem motivo (`409`/`422`) e painéis sem botão `Tentar novamente` |
| 10 | Ajuda e Documentação | 1.5 | Sem legenda/explicação inline para `TMB`, `GET`, `IMC` e regra `4/4/9`, especialmente para o atleta |

---

## 3. Propostas Visuais por Feature

### A. Segurança em Ações Irreversíveis (`[P0]` — `/impeccable harden`)
- **Problema:** `DietMealsPage.tsx` remove refeições/itens em 1 clique sem confirmação; `PlanOverviewView.tsx` publica/arquiva planos de treino em 1 clique (ao contrário de `AthleteDietsPanel.tsx`, que já usa modal de confirmação).
- **Proposta:**
  - Replicar o padrão de `Modal` de confirmação em `PlanOverviewView.tsx` explicando a transição irreversível (`DRAFT` → `IN_REVIEW` → `ACTIVE`).
  - Exigir confirmação em 2 passos (ou modal) para `Remover refeição` em `DietMealsPage.tsx`.

### B. Workbench de Treinos (`[P1]` — `/impeccable distill`)
- **Problema:** `TrainingPage.tsx` empilha seletores de contexto, formulário fixo de nova periodização, cronograma e editor de sessão numa única rolagem longa.
- **Proposta:**
  - Converter `01 / Contexto` em uma **barra de contexto compacta** no topo (chips de Atleta · Periodização · Versão do plano).
  - Mover `Criar periodização` e `Criar sessão` para `Modal` acionados por `+ Nova periodização` / `+ Nova sessão`.
  - No desktop, organizar em **master-detail de 2 colunas**: grade semanal de sessões à esquerda e `SessionEditorView` da sessão selecionada à direita.

### C. Fluxo de Prescrição Nutricional (`[P1]` — `/impeccable distill` + `/impeccable polish`)
- **Problema:** `DietEditorPage.tsx` obriga o coach a lembrar `Peso (kg)` e `GET (kcal)` de outra aba; `DietMealsPage.tsx` mantém um formulário de 11 campos aberto simultaneamente dentro de **cada** card de refeição.
- **Proposta:**
  - **Faixa clínica de referência** no topo do `DietEditorPage.tsx` (`Peso atual · GET · TMB`) com cálculo ao vivo de `g/kg` e soma `4/4/9 kcal` durante a digitação.
  - Em `DietMealsPage.tsx`, recolher `AddMealItemForm` atrás de `+ Adicionar alimento` por refeição (expandindo apenas a refeição ativa) e fixar uma **barra de progresso de macros** (`Prescrito vs. Montado`) no topo.

### D. Home do Coach & Ficha do Atleta (`[P1/P2]` — `/impeccable polish` + `/impeccable clarify`)
- **Problema:** `CoachHomePage.tsx` apenas repete links da navegação superior; na ficha do atleta (`AthleteDetailPage.tsx`), a aba `Treinos` é só um link de saída, `birthDate` aparece em ISO cru (`YYYY-MM-DD`) e a auditoria mostra UUIDs crus (`por 8f3a1c2d…`).
- **Proposta:**
  - Adicionar um **Pulso Clínico** compacto na `CoachHomePage.tsx` (dietas em rascunho/revisão, convites próximos de expirar, atletas recentes).
  - Exibir na aba `Treinos` do `AthleteDetailPage.tsx` um resumo read-only do plano ativo (como já ocorre em `Avaliações` e `Dietas`), formatar data de nascimento + idade e substituir UUIDs crus pelo nome do autor ou badge discreta.

### E. Experiência Mobile do Atleta (`[P2]` — `/impeccable adapt`)
- **Problema:** `AthleteContent.css` foge dos tokens (`12px`/`999px` de raio, sem `--shadow-card` e sem `IBM Plex Mono` para cargas/séries/gramas), `AthleteTrainingPage.tsx` exibe `"Semana 1 · dia 3"` numa lista longa sem atalho para o treino de hoje e `AthleteAssessmentHistoryPage.tsx` não explica `TMB`/`GET`.
- **Proposta:**
  - Alinhar `AthleteContent.css` ao `DESIGN.md` (`--radius-sm` / `--radius-md`, `--shadow-card`, `--font-mono` em métricas e cargas).
  - Usar nomes legíveis de dias (`DAY_LABELS`), adicionar seletor rápido **Semana / Dia ("Treino de hoje")** no topo de `AthleteTrainingPage.tsx` e incluir legendas curtas para `IMC`, `TMB` e `GET`.

### F. Ajustes Pontuais (`[P2]` — `/impeccable clarify`)
- Atualizar subtítulo de `LoginPage.tsx` de *"Acesse sua conta de coach para continuar"* para *"Acesse sua conta ProCoach para continuar"* (a rota atende coach e atleta).
- Avaliar tornar `width: 100%` opcional (`fullWidth`) em `Button.css` para eliminar ~6 overrides de `width: auto` espalhados pelas páginas.

---

## 4. Red Flags por Persona

- **Alex (Power User):** Sem busca rápida na `CoachHomePage`, 3 selects sequenciais ao abrir `TrainingPage` e ausência de autocomplete no cadastro de itens de refeição.
- **Sam (Acessibilidade):** Botões (`Convidar`) dentro de `<tr>` clicável (`tabIndex={0}`) em `AthleteListPage`; estados de loading sem `aria-live="polite"` em `AthleteDetailPage`.
- **Dr. Marina (Coach independente no desktop):** Precisa trocar de aba para consultar `Peso`/`GET` ao editar dieta; aba `Treinos` da ficha do atleta não mostra o plano ativo; risco de publicar plano em 1 clique em `PlanOverviewView`.
- **Lucas (Atleta no celular):** Navegação mobile no topo (fora da zona do polegar); lista inteira de sessões com rótulo `"Semana 1 · dia 3"` sem salto para o treino do dia; siglas `TMB`/`GET` sem explicação.

---

## 5. Questões Importantes para Decisão

1. **Prioridade de ataque:** Entre *segurança de fluxo (`P0`)*, *carga cognitiva em Treinos/Refeições (`P1`)* e *alinhamento mobile do Atleta (`P2`)*, qual frente entra primeiro?
2. **Ficha do Atleta (`AthleteDetailPage`):** A aba `Treinos` deve passar a exibir o resumo do plano ativo inline (como `Avaliações` e `Dietas`) em vez de ser apenas um link para `/coach/training`?
3. **Pulso Clínico na Home (`CoachHomePage`):** Faz sentido transformar a entrada do coach em um painel de pendências (dietas para aprovar, convites expirando, eventos recentes do `AuditLog`)?
4. **Atalho do Atleta na Academia (`AthleteTrainingPage`):** Qual mecanismo mínimo faz mais sentido para o treino do dia (seletor de semana/dia com destaque para hoje, acordeão por sessão)?
5. **Escopo em aberto no `PRODUCT.md`:** Definir se haverá meta formal de acessibilidade (ex.: WCAG AA) e se o produto permanece exclusivamente em `pt-BR`.
