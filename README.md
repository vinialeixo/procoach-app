# ProCoach — Frontend

SPA em React + TypeScript + Vite. Cobre a `T-006` (setup + design system base) e
a `T-011` (telas de login/registro/MFA + rotas por papel) do `TASKS.md`.

## Stack

- **Vite + React 18 + TypeScript**
- **React Router v6** — rotas
- **TanStack Query** — estado de servidor / chamadas à API
- **React Hook Form + Zod** — formulários e validação
- **Axios** — cliente HTTP (`src/lib/api-client.ts`), com proxy de `/api` para
  `http://localhost:8080` (o núcleo Java) em dev — ver `vite.config.ts`.

Sem UI kit de terceiros (MUI, Chakra etc.) de propósito: os componentes em
`src/components/ui` são simples e seguem os design tokens em
`src/styles/tokens.css`, para não herdar a estética genérica desses kits.

## Setup

```bash
npm install
cp .env.example .env
npm run dev
```

Abre em `http://localhost:5173`. Para as chamadas de API funcionarem de
verdade, o núcleo Java (`procoach` — ver repositório do backend) precisa
estar rodando em `localhost:8080`.

## Estrutura

```
src/
├── components/
│   ├── ui/            # Button, Field, AuthLayout — componentes base
│   └── ProtectedRoute.tsx
├── lib/
│   ├── api-client.ts  # axios + interceptors (token, 401)
│   └── auth.ts        # hooks de login/MFA (RF-01)
├── pages/
│   ├── LoginPage.tsx  # fluxo de 2 passos: senha -> MFA
│   └── DashboardPage.tsx  # placeholder pós-login
└── styles/
    ├── tokens.css      # cor, tipografia, espaçamento
    └── global.css      # reset + base
```

## Design

Conceito: "ledger clínico" — o produto é sobre evidência, versionamento e
auditoria, então a estética evita o padrão genérico de app de academia
(gradientes, cards arredondados idênticos) e usa o próprio mecanismo do
produto (histórico de versões/auditoria) como elemento visual no painel de
login (`AuthLayout.tsx`).

- Tipografia: **Space Grotesk** (display), **Inter** (corpo/UI), **IBM Plex
  Mono** (dados reais: IDs, versões, timestamps — nunca decorativo).
- Cor: base grafite claro, navy profundo como ação primária, cobre como
  destaque pontual.

## Integração com o núcleo (autenticação — T-008/T-011)

As telas de login/registro/MFA consomem os endpoints reais do módulo
`com.fitness.procoach.security` (Spring Security + JWT + TOTP), documentados
no Swagger do núcleo (`/swagger-ui.html`):

- `POST /api/v1/auth/login` — se `mfaRequired`, o fluxo segue para
  `POST /api/v1/auth/mfa/verify` com o `challengeId`; senão devolve
  `accessToken`/`refreshToken` direto.
- `POST /api/v1/auth/register`, `POST /api/v1/auth/mfa/setup` e
  `POST /api/v1/auth/mfa/setup/confirm` — cadastro de coach + ativação do MFA.

O `src/lib/api-client.ts` anexa `Authorization: Bearer <token>` em toda
requisição e limpa a sessão em 401. O `ProtectedRoute`/`RoleRedirect` usam o
claim `role` do JWT apenas para navegação (COACH → `/coach`, ATHLETE →
`/athlete`) — a autorização real é sempre validada no núcleo (RBAC, RN-01).

## Próximos passos (backlog)

Depois do login, as próximas telas de front no `TASKS.md`, em ordem:

- `T-019` — formulário de cadastro de atleta
- `T-022` — listagem/detalhe do atleta + troca de status
- `T-027` — avaliação física + gráficos de evolução
- `T-034` — editor de dieta + versões + aprovação
- `T-040` — biblioteca + montagem de treino/periodização

Peça ao agente uma tela por vez, reaproveitando os componentes de
`src/components/ui` e os tokens de `src/styles/tokens.css` — não crie um
novo sistema de design a cada tela.
