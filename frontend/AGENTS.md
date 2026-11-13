# AGENTS.md — Frontend (ProCoach)

> Regras gerais do projeto, invariantes de domínio e configuração das skills ficam em [../AGENTS.md](../AGENTS.md).
> Antes de implementar telas ou hooks, consulte [README.md](README.md) e [TASKS.md](TASKS.md).

## Convenções do Frontend (`frontend/`)

- **Stack:** React 19, TypeScript, Vite, React Router 7, TanStack Query 5, React Hook Form + Zod, Axios.
- **Sem UI kit externo:** não usar MUI/Chakra; reutilizar componentes de `src/components/ui` (`Button`, `ButtonLink`, `Field`, `SelectField`, `Modal`, `DataTable`, `StatusBadge`, `StateMessage`) e os design tokens de `src/styles/tokens.css`.
- **Autorização:** nunca decidir autorização no front; o backend valida RBAC e isolamento multi-tenant. O front apenas oculta UI conforme o papel no JWT e reage a `401`/`403`.
- **Contratos em desenvolvimento:** se um endpoint ainda não existir no backend, documentar a suposição no código (ver `src/lib/auth.ts` como referência) e nunca mascarar erros com fallbacks silenciosos quando o contrato real já existir.
- **Ordem de implementação:** componente de UI reutilizável (`src/components/ui`) → hook/serviço (`src/lib`) → página (`src/pages`) → rota (`src/App.tsx`).

## Fluxo para tasks de `frontend/TASKS.md`

1. Localizar a task pelo ID (`T-xxx` ou `M2-xxx`) em `frontend/TASKS.md` e verificar a coluna **Depende de** (inclusive dependências de backend em `../backend/TASKS.md`). Se alguma dependência estiver `[ ]`, avisar antes de prosseguir.
2. Implementar seguindo as camadas acima e escrever testes em Vitest + Testing Library cobrindo os critérios de aceitação.
3. Rodar `npm test`, `npm run build` e `npm run lint` em `frontend/` e corrigir qualquer falha.
4. Marcar apenas a task concluída como `[x]` em `frontend/TASKS.md`.

## Comandos úteis

```bash
npm run dev       # sobe Vite na porta 5173 (proxy /api -> localhost:8080)
npm test          # Vitest (testes unitários e de integração)
npm run build     # tsc -b + build de produção
npm run lint      # ESLint
npm run test:e2e  # Playwright E2E
```
