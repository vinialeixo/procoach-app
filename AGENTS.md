# ProCoach — Guia para Agentes

## Regras do projeto

- **Decisão do coach:** a plataforma é coach-led; qualquer recurso de IA/RAG é apenas consultivo e read-only.
- **Versionamento imutável:** nunca sobrescrever `Diet`, `TrainingPlan` ou `Assessment` — sempre criar nova versão encadeada.
- **Auditoria e exclusão:** operações de escrita relevantes devem registrar `AuditLog` (append-only); exclusões são sempre **soft-delete**.
- **Fronteiras Spring Modulith (`com.fitness.procoach.<modulo>`):** pacotes `*/domain` são livres de Spring/JPA; comunicação entre módulos ocorre apenas por fachadas públicas (`application`), portas ou eventos pós-commit; referências entre agregados são sempre por `UUID`.
- **Autorização:** RBAC (`ROLE_COACH` / `ROLE_ATHLETE`) e isolamento multi-tenant são sempre validados no backend; o frontend apenas reflete UI e trata `401`/`403`.
- **Frontend sem UI kit externo:** reutilizar `frontend/src/components/ui` e tokens de `frontend/src/styles/tokens.css`, seguindo a ordem `src/components/ui` → `src/lib/*` → `src/pages/*` → `src/App.tsx`.

## Onde ler antes de agir

- **Backend:** leia `backend/SPEC.md` e `backend/TASKS.md` antes de implementar tasks de API/domínio.
- **Frontend:** leia `frontend/AGENTS.md`, `frontend/README.md` e `frontend/TASKS.md` antes de criar telas ou hooks.
- **Nutrição:** leia `docs/nutricao-plano-alimentar.md` ao trabalhar em dieta, refeições, macros ou balanço energético.
- **QA / E2E transversal:** consulte `TASKS.md` na raiz.

## Verificação antes de concluir

- **Backend:** `cd backend && ./mvnw test` (inclui `ModulithArchitectureTest`).
- **Frontend:** `cd frontend && npm test && npm run build && npm run lint`.

## Agent skills

### Issue tracker

Issues and specs live as GitHub issues in `vinialeixo/procoach-app` (managed via `gh`). See `docs/agents/issue-tracker.md`.

### Triage labels

Standard 5-label triage vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout (`GLOSSARY.md` and `docs/adr/` at the repo root). See `docs/agents/domain.md`.
