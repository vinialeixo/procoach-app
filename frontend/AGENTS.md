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

1. A plataforma **não substitui o coach**. O RAG é apenas consultivo.
2. A **decisão final sempre pertence ao coach** (nunca automatizar aprovação/geração).
3. **Nunca sobrescrever** Diet, TrainingPlan ou Assessment — sempre criar **nova versão**.
4. Toda operação de escrita **deve** gerar um registro em `AuditLog` (append-only, nunca update/delete).
5. Exclusões são **soft-delete**, nunca DELETE físico.
6. O domínio (`*/domain`) é **framework-agnostic** — nenhuma anotação Spring/JPA ali.
7. Referências entre agregados são **sempre por UUID** (coachId, athleteId), nunca objeto aninhado.
8. O RAG **nunca** tem permissão de escrita no domínio — comunicação apenas via gRPC read-only.

## Convenção de camadas por módulo

Cada bounded context em `com.procoach.core.<modulo>` segue:

```
<modulo>/
├── domain/          # Aggregate root, entidades, VOs, enums, portas (interfaces de repositório). SEM Spring.
├── application/     # Casos de uso / application services. Orquestra domínio + portas.
├── infrastructure/  # Adapters: JPA entities, repositórios Spring Data, mapeamento, eventos.
└── interfaces/      # Controllers REST (DTOs de entrada/saída), exception handlers do módulo.
```

Não acessar classes de `infrastructure`/`domain` de outro módulo diretamente — usar apenas
classes públicas expostas pelo módulo (idealmente uma fachada em `application`) ou eventos de
domínio.

## Backlog e como trabalhar tasks

O trabalho de implementação é guiado por dois arquivos na raiz do repo:

- **`TASKS.md`** — backlog de execução (MVP + MVP2), organizado por sprint, com checkbox de
  status (`[ ]`/`[x]`), dependências (coluna "Depende de") e Task ID (`T-xxx` para MVP, `M2-xxx`
  para MVP2).
- **`USER_STORIES.md`** — as User Stories completas por épico, com critérios de aceitação.

Ao me pedir para "implementar a task T-0XX" (ou similar), siga sempre este processo:

1. Abra `TASKS.md` e localize a task pelo ID. Confira a coluna "Depende de" — se alguma
   dependência ainda estiver `[ ]`, pare e me avise antes de continuar.
2. Identifique o épico e a User Story relacionada na mesma linha, abra `USER_STORIES.md` e leia
   os critérios de aceitação da US correspondente (confirme pela descrição/épico, não só pelo
   número — a numeração das duas planilhas de origem não é idêntica).
3. Releia as regras inegociáveis acima antes de escrever qualquer código.
4. Implemente em camadas (`domain` → `application` → `infrastructure` → `interfaces`), uma de
   cada vez — se a task cobrir só uma camada, não adiante trabalho de outra task.
5. Escreva testes cobrindo os critérios de aceitação da US, priorizando versionamento/auditoria.
6. Rode `./mvnw test` e corrija falhas, incluindo violação de boundary do Spring Modulith.
7. Marque a task como `[x]` em `TASKS.md` (só essa linha).
8. Resuma em poucas linhas o que mudou e se algo da US ficou de fora, e por quê.

Se a task pedida não existir em `TASKS.md`, avise em vez de inventar uma implementação.

## Frontend (SPA)

Projeto separado (`procoach-frontend/`), React + TypeScript + Vite. Stack fixada:
React Router v6, TanStack Query, React Hook Form + Zod, Axios. Sem UI kit de terceiros
(MUI/Chakra) — componentes próprios em `src/components/ui`, seguindo os design tokens
de `src/styles/tokens.css`. Ver `procoach-frontend/README.md` para detalhes.

Regras específicas do front:

- **Nunca decidir autorização no front.** RBAC/isolamento multi-tenant (RN-01) é sempre
  validado no núcleo Java; o front só reage a 401/403 e esconde UI, não é fonte de verdade.
- **Reaproveitar os componentes de `src/components/ui`** (Button, Field, AuthLayout) em vez
  de criar novos padrões visuais a cada tela nova.
- **Contratos de API assumidos a partir do `SPEC.md`** devem ser marcados como suposição no
  código (comentário) até o endpoint real existir no backend — ver `src/lib/auth.ts` como
  exemplo do padrão.
- Ao implementar uma tela nova (tasks "Front: ..." do `TASKS.md`), seguir a mesma ordem de
  camadas: componente de UI reutilizável → hook/serviço em `src/lib` → página em `src/pages`
  → rota em `src/App.tsx`.
- **Manter os mocks MSW (`src/mocks/`) sincronizados com mudanças de contrato.** Ao adicionar
  ou alterar endpoints, DTOs ou regras de domínio em `src/lib/*.ts`, atualize também
  `src/mocks/db.ts`, os handlers em `src/mocks/handlers/*.handlers.ts` e os testes em
  `src/mocks/mocks.test.ts` para manter o modo standalone (`npm run dev:mocks`) fiel ao
  contrato real. Nunca importe `msw` ou `src/mocks/` fora do carregamento condicional de
  `VITE_USE_MOCKS === "true"` para preservar o *tree-shaking* do build de produção.

## Comandos úteis

```bash
npm run dev       # sobe Vite na porta 5173 (proxy /api -> localhost:8080)
npm test          # Vitest (testes unitários e de integração)
npm run build     # tsc -b + build de produção
npm run lint      # ESLint
npm run test:e2e  # Playwright E2E
```
