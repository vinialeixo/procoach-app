# Painel Admin — Coaches e Atletas (plano)

> **Status:** proposta revisada com as decisões do PO em 2026-11-26. Nada implementado ainda.
> **GitHub:** épico **#214**; tasks **#215–#224**.
> **Persona nova:** Administrador (`ROLE_ADMIN`).
> **Rastreabilidade:** US-039 (admin gerencia RBAC), US-042 (auditoria), RN-01 (isolamento multi-tenant), RF-11 (atleta só vê os próprios dados).

---

## 0. Decisões confirmadas pelo PO

| # | Decisão | Efeito no plano |
|---|---|---|
| 1 | **Convite-only** para coach | Remover o cadastro público (`/registro` e `POST /api/v1/auth/register`). Coach só nasce por convite do admin. |
| 2 | Visão de atletas = **recomendada** | Admin vê identidade, status, objetivo e estado de acesso; **não** vê dieta/treino/avaliação. |
| 3 | Cancelamento com atletas = **recomendado** | Bloquear com 409 enquanto houver atletas ativos. |
| 4 | Admin default + gestão de admins | Criar `admin` / `procoach` no primeiro boot e ter página para adicionar admins e alterar senhas. |
| 5 | Auditoria de leitura = **não** | Apenas escritas administrativas geram `AuditLog`. |

---

## 1. Objetivo

Dar ao administrador da plataforma uma área separada da área do coach para:

1. Entrar com um papel próprio (`ADMIN`), sem virar um "super coach".
2. Ver a lista de coaches (nome, e-mail, status, volume de atletas, estado do convite de acesso).
3. Adicionar um coach por convite, sem o admin definir a senha dele.
4. Abrir um coach e ver os atletas da carteira (identidade + status, não conteúdo clínico).
5. Suspender, reativar ou arquivar coaches, com motivo e auditoria.
6. Gerenciar contas de administrador: listar, adicionar e alterar/resetar senha.
7. Manter o isolamento multi-tenant: o admin usa uma API administrativa própria e **não** acessa endpoints de coach/atleta fingindo ser outra pessoa.

### Fora de escopo deste MVP

- Impersonation ("entrar como coach").
- Admin editar dieta, treino ou avaliação.
- Transferência de atleta entre coaches.
- CRUD completo de papéis/permissões (a US-039 pede, mas é um épico à parte; aqui só o papel `ADMIN` e suas permissões).
- Exportação do log de auditoria (US-042).
- Faturamento/assinatura por coach.
- Desativar/remover administradores (só adicionar e trocar senha neste MVP).

---

## 2. Diagnóstico do que já existe

| Área | Situação atual | Impacto no plano |
|---|---|---|
| RBAC | Tabelas `role`, `role_permission`, `user_role`; papéis criados em runtime (`COACH`, `ATHLETE`). `Permission` já tem `MANAGE_ROLES` e `MANAGE_USERS`. | Falta `ADMIN`, `MANAGE_COACHES` e o bootstrap. |
| JWT | Claim único `role` (`COACH`/`ATHLETE`) convertido em `ROLE_*`. | Admin entra no mesmo login e no `primaryRole`; contas de admin são separadas (sem acúmulo de papéis no MVP). |
| Login | `AuthService.login` busca **somente por e-mail**. | Precisa aceitar usuário **ou** e-mail para o default `admin`. |
| Coach | Agregado `Coach` com `userId`, `status` (`ACTIVE`/`SUSPENDED`/`CANCELLED`) e transições `suspend`/`reactivate`/`cancel` no domínio. | Aproveitar o ciclo de vida; falta fachada de listagem/admin e efeito no login. |
| Atleta | `Athlete` com `coachId`, soft-delete e `findActiveByCoachId`; `AthleteAccessInfoProvider` já informa credencial/convite. | Expor leitura por `coachId` para o admin, sem depender do usuário autenticado. |
| Convite | `AthleteInvite` (token hasheado, uso único, 7 dias) + e-mail + tela `/convite/:token`. | Reutilizar o padrão para coach em uma tabela própria. |
| Cadastro | `POST /api/v1/auth/register` é público e cria coach com senha + auto-login. | **Será removido** (convite-only). |
| Frontend | Rotas por papel em `App.tsx`, `RoleRedirect`, `ProtectedRoute`, `homeForRole`; `Role = "COACH" \| "ATHLETE"`. | Adicionar `ADMIN`, `/admin`, tela de troca de senha e remover `/registro`. |
| Mocks | Um único coach (`mockDb.coach`) e atletas sem `coachId`. | Modo standalone precisa de vários coaches, `coachId` no atleta e credencial admin. |

---

## 3. Decisões de produto

### D1 — Admin é um papel separado, não um coach com superpoderes

O admin **não** possui agregado `Coach` nem `athleteId`. Ele enxerga a operação por `/api/v1/admin/**`.

Consequências:

- `ROLE_ADMIN` não passa nos `@PreAuthorize("hasRole('COACH')")` existentes.
- O admin não entra em `/coach/...` nem `/athlete/...`; tem shell e rotas próprios em `/admin`.
- Se no futuro uma pessoa precisar ser coach **e** admin, será preciso evoluir o claim de `role` para `roles[]`. Não fazer agora.

### D2 — Coach é criado por convite, nunca por senha definida pelo admin

1. Admin informa **nome** e **e-mail**.
2. Backend cria o `User` (desabilitado, senha aleatória inutilizável), o agregado `Coach` e um `CoachInvite` de uso único (7 dias).
3. O coach recebe um link (`/convite-coach/:token`), define a própria senha e já entra em `/coach`.
4. Enquanto o convite não é aceito, o coach aparece como **"Convite pendente"** e não loga.

### D3 — Cadastro público de coach removido (convite-only)

- Remover a rota `/registro` e a página `RegisterPage`.
- Remover (ou desativar com 410/403) o endpoint `POST /api/v1/auth/register`; o provisionamento de coach passa a ser interno ao fluxo do admin.
- O único caminho de entrada de coach é o convite emitido pelo admin.
- Manter o cadastro de atleta como está (o coach convida o atleta).

### D4 — Visão de atletas do coach é operacional, não clínica

No detalhe do coach o admin vê: nome, status, objetivo, estado de acesso e data de cadastro. **Não** vê dieta, treino, avaliação, medidas ou histórico clínico.

### D5 — Arquivar é `CANCELLED`, nunca DELETE físico

- `CANCELLED` é estado terminal; coaches cancelados ficam fora da lista padrão (`includeArchived=false`), mas continuam consultáveis e auditáveis.
- Cancelamento é bloqueado enquanto houver atletas ativos (409 com a contagem).
- Não existe endpoint de DELETE físico.

### D6 — Admin default e gestão de administradores

- No primeiro boot, se **não existir nenhum usuário com papel `ADMIN`**, criar:
  - usuário: `admin`
  - e-mail interno: `admin@procoach.local`
  - senha: `procoach`
  - papel: `ADMIN`
  - flag `mustChangePassword = true`
- A senha default é configurável por ambiente (`PROCOACH_BOOTSTRAP_ADMIN_PASSWORD`), com `procoach` apenas como padrão local. O startup deve logar um aviso claro para trocar a senha.
- O login aceita **usuário ou e-mail**, para que o admin entre literalmente com `admin` / `procoach`.
- O primeiro login força a troca de senha antes de liberar o restante do painel.
- A página `/admin/admins` permite:
  - listar administradores;
  - adicionar um novo admin (usuário, e-mail e senha inicial, com troca obrigatória no primeiro login);
  - alterar a própria senha (exige a senha atual);
  - resetar a senha de outro admin (gera auditoria; a sessão antiga expira no máximo em 15 min e o refresh é invalidado).
- O e-mail interno `admin@procoach.local` não recebe mensagens; serve só para satisfazer a unicidade de `app_user.email`. O login principal é o username.

> **Nota de segurança (a confirmar na implementação):** `procoach` é uma senha fraca e conhecida. O plano mantém o default solicitado, mas com troca obrigatória no primeiro acesso e bloqueio de reutilização da senha padrão. Em produção, o time deve definir `PROCOACH_BOOTSTRAP_ADMIN_PASSWORD` antes do primeiro boot.

### D7 — Autorização sempre no backend

O front esconde menus e redireciona por papel, mas toda rota `/api/v1/admin/**` exige `ROLE_ADMIN` no backend (método + `SecurityConfig`). Ações de escrita geram `AuditLog` append-only com o `sub` do JWT como autor. Leituras não são auditadas.

---

## 4. Arquitetura do backend

### 4.1 Novo módulo `admin`

```
com.fitness.procoach.admin/
├── application/     # AdminCoachService (orquestra coach + athlete + security + audit)
│                    # AdminSummary, AdminCoachSummary, AdminAthleteSummary
├── infrastructure/  # AdminConfiguration (beans), se necessário
└── interfaces/      # AdminCoachController, DTOs, AdminExceptionHandler
```

- O módulo `admin` **depende** apenas das APIs públicas (`@NamedInterface("api")`) de `coach.application`, `athlete.application`, `security.application` e `audit.application`.
- Nenhum módulo existente depende de `admin` (sem ciclo).
- O admin não acessa `domain`/`infrastructure` de outro módulo, nem as tabelas de outro módulo diretamente.
- Não é necessário um agregado de domínio novo aqui: o admin é uma superfície de orquestração/consulta. O convite do coach e a gestão de admins ficam em `security` (ciclo de vida de credencial).

### 4.2 Mudanças no módulo `security`

| Mudança | Detalhe |
|---|---|
| Papel `ADMIN` | `AuthService.ADMIN_ROLE = "ADMIN"`; `primaryRole` com precedência `ADMIN > COACH > ATHLETE`. |
| Permissões | Novo valor `Permission.MANAGE_COACHES`. Role `ADMIN` com `MANAGE_USERS`, `MANAGE_ROLES`, `MANAGE_COACHES`. |
| Bootstrap | `AdminBootstrapRunner` idempotente criando `admin` / `procoach` apenas se não houver admin (seção 4.4). |
| Login por identificador | `login(identifier, password)` resolve primeiro por e-mail e depois por username. O campo do DTO pode continuar se chamando `email`, mas passa a aceitar os dois. |
| Bloqueio de login do coach | `login()` e `refresh()` recusam coach `SUSPENDED`/`CANCELLED` (401 genérico, sem vazar o motivo). |
| Provisionamento de coach | Novo método na fachada (`AdminAccountService`): criar `User` desabilitado com papel `COACH` e senha aleatória; devolver `userId`. |
| Senha | `User` ganha `setPasswordHash(...)`, `passwordChangedAt` e `mustChangePassword`. |
| Convite | `CoachInvite` + repositório + `CoachInviteService` (mesmo padrão do `AthleteInvite`): token só em hash, TTL 7 dias, revogação, uso único. |
| Gestão de admins | `AdminAccountService`: criar admin, alterar a própria senha, resetar senha de outro admin. |
| E-mail | Reutilizar o padrão `AthleteInviteMailer` com um `CoachInviteMailer`; se SMTP não estiver configurado, devolver o link na resposta para o admin copiar. |
| Senha obrigatória | Claim `mustChangePassword` no JWT + filtro que bloqueia `/api/v1/admin/**` (exceto `PUT /admin/me/password`) enquanto for `true`. |

### 4.3 Mudanças nas fachadas existentes

`coach/application/CoachApplicationService`:

```java
List<CoachSummary> list(CoachStatus status, boolean includeArchived);
CoachSummary requireSummaryById(UUID coachId);
Coach requireById(UUID coachId);           // para ações administrativas
void suspend(UUID coachId);
void reactivate(UUID coachId);
void cancel(UUID coachId);
```

`athlete/application/AthleteApplicationService` (ou uma fachada de consulta dedicada):

```java
List<Athlete> listByCoachId(UUID coachId, AthleteStatus status); // inclui não removidos
Map<UUID, Long> countActiveByCoachIds(Collection<UUID> coachIds);
```

`security/application/UserService` (ou `UserDirectory`):

```java
Optional<String> emailOf(UUID userId);
Optional<Boolean> enabledOf(UUID userId);
Map<UUID, String> emailsByUserIds(Collection<UUID> userIds); // evita N+1
```

> `CoachSummary`/`AdminCoachSummary` são records públicos da camada `application`; não expõem JPA nem o agregado inteiro.

### 4.4 Bootstrap do primeiro admin

Configuração:

```properties
procoach.admin.bootstrap.username=admin
procoach.admin.bootstrap.email=admin@procoach.local
procoach.admin.bootstrap.password=procoach
procoach.admin.bootstrap.force-password-change=true
```

- `ApplicationRunner` idempotente: só age se **não existir** nenhum usuário com papel `ADMIN`.
- Cria a role `ADMIN` se necessário, o usuário habilitado e `mustChangePassword=true`.
- Loga um aviso (nunca a senha) e recomenda trocar imediatamente.
- **Não** sobrescreve admin existente e **não** recria a senha default após o primeiro boot.
- Em produção, sobrescrever `PROCOACH_BOOTSTRAP_ADMIN_PASSWORD` antes do primeiro boot.

### 4.5 Segurança

- `SecurityConfig`: `.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")` (defesa em profundidade) além dos `@PreAuthorize` nos controllers.
- `AdminCoachController` e `AdminAccountController` com `@PreAuthorize("hasRole('ADMIN')")`.
- Guards existentes (`@athleteGuard`, `CoachAthleteAccessGuard`) continuam negando `ADMIN` por padrão — o admin usa apenas `/api/v1/admin/**`.
- Nenhum endpoint administrativo aceita `actorId`/`coachId` do request como identidade: o autor é sempre o `sub` do JWT.
- Suspensão/cancelamento: os endpoints de coach já resolvem o coach via `requireCoachIdByUserId` e falham se não estiver `ACTIVE`. Com o bloqueio no login/refresh, o efeito é imediato para novas requisições e novos tokens.
- Troca/reset de senha: `refresh` compara o `iat` do token com `passwordChangedAt` e recusa tokens emitidos antes da troca. Access tokens expiram em 15 min.
- Filtro de senha obrigatória: enquanto `mustChangePassword=true`, só `PUT /api/v1/admin/me/password` e endpoints de autenticação são permitidos; o restante responde `403 PASSWORD_CHANGE_REQUIRED`.

### 4.6 Gestão de administradores

Casos de uso em `security/application/AdminAccountService`:

```java
List<AdminSummary> listAdmins();
AdminSummary createAdmin(String username, String email, String rawPassword);
AuthTokens changeOwnPassword(UUID adminUserId, String currentPassword, String newPassword);
void resetPassword(UUID actingAdminId, UUID targetAdminId, String newPassword);
```

Regras:

- Adicionar admin: username e e-mail únicos (409), senha inicial com mínimo 8; `mustChangePassword=true`.
- Alterar a própria senha: exige a senha atual; não permite repetir a senha default `procoach`.
- Resetar senha de outro admin: permitido a qualquer `ADMIN` (não há super-admin no MVP), com auditoria e sem revelar a senha antiga.
- O endpoint de reset não pode ser usado para a própria conta (usar `me/password`, que exige a senha atual).
- Não há remoção/desativação de admin neste MVP; se necessário depois, adicionar proteção para não remover o último admin.

---

## 5. Contratos de API

Base: `/api/v1/admin` — todas exigem `Authorization: Bearer` com `ROLE_ADMIN`.

### 5.1 Coaches

| Método | Rota | Uso |
|---|---|---|
| `GET` | `/coaches?status=&search=&includeArchived=false` | Lista coaches com contagem de atletas e estado de acesso. |
| `POST` | `/coaches` | Cria coach + convite. Corpo `{ "name", "email" }`. |
| `GET` | `/coaches/{coachId}` | Detalhe + contagens por status de atleta. |
| `GET` | `/coaches/{coachId}/athletes?status=` | Atletas da carteira (visão operacional). |
| `POST` | `/coaches/{coachId}/status` | `{ "status": "SUSPENDED" \| "ACTIVE" \| "CANCELLED", "reason" }`. |
| `POST` | `/coaches/{coachId}/invites/resend` | Revoga o convite pendente e emite outro. |
| `DELETE` | `/coaches/{coachId}/invites` | Revoga o convite pendente (opcional no MVP). |
| `GET` | `/summary` | KPIs do painel: coaches por status, convites pendentes, atletas ativos. |

### 5.2 Administradores

| Método | Rota | Uso |
|---|---|---|
| `GET` | `/admins` | Lista administradores (id, username, email, enabled, mustChangePassword, createdAt). |
| `POST` | `/admins` | Cria admin. Corpo `{ "username", "email", "password" }`. |
| `PUT` | `/me/password` | Altera a própria senha. Corpo `{ "currentPassword", "newPassword" }`; devolve novos tokens. |
| `PUT` | `/admins/{userId}/password` | Reseta a senha de outro admin. Corpo `{ "newPassword" }`; 204. |

### 5.3 Exemplo — listagem de coaches

```json
{
  "items": [
    {
      "coachId": "8b1c...",
      "name": "Vinícius Almeida",
      "email": "vinicius@exemplo.com",
      "status": "ACTIVE",
      "accessStatus": "ACTIVE",
      "athleteCount": 12,
      "activeAthleteCount": 9,
      "createdAt": "2026-01-10T12:00:00Z"
    },
    {
      "coachId": "9c2d...",
      "name": "Marina Costa",
      "email": "marina@exemplo.com",
      "status": "ACTIVE",
      "accessStatus": "INVITE_PENDING",
      "athleteCount": 0,
      "activeAthleteCount": 0,
      "createdAt": "2026-11-25T09:30:00Z"
    }
  ],
  "total": 2
}
```

`accessStatus`: `ACTIVE` (usuário habilitado), `INVITE_PENDING` (convite utilizável), `DISABLED` (sem convite e sem credencial válida). O `status` do coach é o ciclo de vida (`ACTIVE`/`SUSPENDED`/`CANCELLED`).

### 5.4 Exemplo — criação de coach com convite

```json
// POST /api/v1/admin/coaches
{ "name": "Marina Costa", "email": "marina@exemplo.com" }

// 201 Created
{
  "coachId": "9c2d...",
  "userId": "a1b2...",
  "name": "Marina Costa",
  "email": "marina@exemplo.com",
  "status": "ACTIVE",
  "accessStatus": "INVITE_PENDING",
  "invite": {
    "token": "tok_...",
    "expiresAt": "2026-12-03T09:30:00Z",
    "emailSent": true
  }
}
```

O `token` aparece **somente nesta resposta** (e no reenvio), para o fallback de copiar o link. Depois disso, só o hash existe no banco.

### 5.5 Exemplo — atletas do coach

```json
[
  {
    "athleteId": "2222...",
    "name": "Lucas Silva",
    "status": "ACTIVE",
    "goal": "HYPERTROPHY",
    "accessStatus": "HAS_ACCESS",
    "createdAt": "2026-02-01T10:00:00Z"
  }
]
```

### 5.6 Erros

| Situação | HTTP | Código/mensagem |
|---|---|---|
| Sem token / token inválido | 401 | padrão do resource server |
| Token sem `ROLE_ADMIN` | 403 | padrão |
| Senha default ainda não trocada | 403 | `PASSWORD_CHANGE_REQUIRED` |
| Coach inexistente/arquivado não visível | 404 | `COACH_NOT_FOUND` |
| E-mail/username já cadastrado | 409 | `EMAIL_ALREADY_REGISTERED` / `USERNAME_ALREADY_REGISTERED` |
| Convite já aceito/não pendente | 409 | `INVITE_NOT_PENDING` |
| Transição de status inválida | 409 | `INVALID_COACH_STATUS_TRANSITION` |
| Cancelar com atletas ativos | 409 | `COACH_HAS_ACTIVE_ATHLETES` + contagem |
| Convite expirado/revogado/inexistente | 404 | `INVALID_INVITE` (genérico) |
| Senha atual incorreta | 400 | `INVALID_CURRENT_PASSWORD` |
| Nova senha igual à default | 400 | `DEFAULT_PASSWORD_NOT_ALLOWED` |

---

## 6. Banco de dados

### Migration `V810__admin_coach_invite_and_accounts.sql`

```sql
-- Convite do coach (primeira credencial): token só em hash, uso único.
CREATE TABLE coach_invite (
    id             UUID PRIMARY KEY,
    coach_id       UUID         NOT NULL,
    user_id        UUID         NOT NULL,
    email          VARCHAR(255) NOT NULL,
    token_hash     VARCHAR(64)  NOT NULL UNIQUE,
    expires_at     TIMESTAMPTZ  NOT NULL,
    used_at        TIMESTAMPTZ,
    revoked_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_coach_invite_coach ON coach_invite (coach_id);
CREATE INDEX idx_coach_invite_user  ON coach_invite (user_id);
CREATE INDEX idx_coaches_status     ON coaches (status);

-- Troca obrigatória e invalidação de refresh após alteração de senha.
ALTER TABLE app_user
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN password_changed_at  TIMESTAMPTZ;

CREATE UNIQUE INDEX uq_app_user_username ON app_user (username);
```

Notas:

- `coach_id`/`user_id` são referências por UUID (sem FK física entre bounded contexts, como já ocorre em `athlete_invite`).
- A unicidade de e-mail continua em `app_user.email`; a de username já existe na tabela original (índice único).
- Não adicionar coluna de exclusão física em `coaches`: `CANCELLED` é o estado de arquivamento.
- O default admin é criado pelo bootstrap, não por `INSERT` em migration.

---

## 7. Regras de negócio e auditoria

| ID | Regra |
|---|---|
| BR-ADM-01 | Somente `ROLE_ADMIN` acessa `/api/v1/admin/**`; o front não é fonte de autorização. |
| BR-ADM-02 | Admin não possui `Coach` nem `athleteId`; não acessa endpoints de coach/atleta. |
| BR-ADM-03 | Coach criado pelo admin nasce com acesso pendente (usuário desabilitado) até aceitar o convite. |
| BR-ADM-04 | Convite de coach é de uso único, expira em 7 dias e é revogado ao reenviar. |
| BR-ADM-05 | Coach `SUSPENDED` ou `CANCELLED` não loga e não renova token; endpoints de coach respondem 403. |
| BR-ADM-06 | `CANCELLED` é terminal e não há DELETE físico; cancelamento com atletas ativos é bloqueado (409). |
| BR-ADM-07 | Toda escrita administrativa gera `AuditLog` na mesma transação. |
| BR-ADM-08 | O admin não vê conteúdo clínico do atleta (dieta, treino, avaliação) neste MVP. |
| BR-ADM-09 | Cadastro público de coach não existe; o único onboarding é o convite do admin. |
| BR-ADM-10 | O primeiro admin (`admin`/`procoach`) só é criado se não houver nenhum admin e deve trocar a senha no primeiro login. |
| BR-ADM-11 | Um admin pode criar outros admins e alterar/resetar senhas; a própria troca exige a senha atual. |
| BR-ADM-12 | A senha default `procoach` não pode ser reutilizada após a troca obrigatória. |
| BR-ADM-13 | Leituras administrativas não geram auditoria; escritas sempre geram. |

### Eventos de auditoria

| Evento | Agregado | Quando |
|---|---|---|
| `ADMIN_BOOTSTRAPPED` | `User` | Admin default criado no primeiro boot. |
| `ADMIN_CREATED` | `User` | Admin cria outro admin. |
| `ADMIN_PASSWORD_CHANGED` | `User` | Admin altera a própria senha. |
| `ADMIN_PASSWORD_RESET` | `User` | Admin reseta a senha de outro admin. |
| `COACH_CREATED` | `Coach` | Admin cria coach + usuário. |
| `COACH_INVITED` | `CoachInvite` | Convite emitido. |
| `COACH_INVITE_RESENT` | `CoachInvite` | Convite reemitido. |
| `COACH_INVITE_REVOKED` | `CoachInvite` | Admin revoga convite. |
| `COACH_INVITE_ACCEPTED` | `CoachInvite` | Coach define a senha. |
| `COACH_SUSPENDED` | `Coach` | Admin suspende (com `reason`). |
| `COACH_REACTIVATED` | `Coach` | Admin reativa. |
| `COACH_CANCELLED` | `Coach` | Admin arquiva (com `reason`). |

---

## 8. Frontend

### 8.1 Infra de autenticação e navegação

Arquivos a alterar:

- `src/lib/jwt.ts` — `role?: "COACH" | "ATHLETE" | "ADMIN"`; claim `mustChangePassword`.
- `src/lib/auth.ts` — `Role` inclui `"ADMIN"`; `homeForRole("ADMIN") => "/admin"`; login envia o identificador (`admin` ou e-mail) e trata `passwordChangeRequired`.
- `src/components/RoleRedirect.tsx` — admin vai para `/admin`.
- `src/components/ProtectedRoute.tsx` — fallback por papel inclui admin.
- `src/layouts/AppShell.tsx` — `end` também para `/admin`.
- `src/App.tsx` — novo bloco `<ProtectedRoute allow={["ADMIN"]}>` e remoção da rota `/registro`.
- `src/components/ProtectedRoute.tsx` / interceptor — se a API responder `403 PASSWORD_CHANGE_REQUIRED`, redirecionar para `/admin/alterar-senha`.

```tsx
<Route element={<ProtectedRoute allow={["ADMIN"]} />}>
  <Route path="/admin" element={<AdminHomePage />} />
  <Route path="/admin/coaches" element={<AdminCoachListPage />} />
  <Route path="/admin/coaches/:coachId" element={<AdminCoachDetailPage />} />
  <Route path="/admin/admins" element={<AdminAccountsPage />} />
  <Route path="/admin/alterar-senha" element={<ChangePasswordPage />} />
</Route>
<Route path="/convite-coach/:token" element={<AcceptCoachInvitePage />} />
```

### 8.2 Camada de dados

Novo `src/lib/admin.ts`:

- Tipos: `AdminCoach`, `AdminCoachDetail`, `AdminAthlete`, `AdminSummary`, `CoachStatus`, `CoachAccessStatus`, `AdminAccount`.
- Hooks: `useAdminCoaches(filters)`, `useAdminCoach(coachId)`, `useAdminCoachAthletes(coachId, status)`, `useCreateCoach()`, `useResendCoachInvite()`, `useChangeCoachStatus()`.
- Hooks de admins: `useAdminAccounts()`, `useCreateAdmin()`, `useChangeOwnPassword()`, `useResetAdminPassword()`.
- `src/lib/coach-invites.ts`: `useCoachInvitePreview(token)` e `useAcceptCoachInvite()`.
- Query keys hierárquicas (`["admin", "coaches"]`, `["admin", "coaches", id]`, `["admin", "admins"]`, ...) para invalidação correta.

### 8.3 Telas

| Tela | Conteúdo |
|---|---|
| `AdminHomePage` (`/admin`) | KPIs: coaches totais/ativos/suspensos, convites pendentes, atletas ativos, admins. Atalhos para Coaches e Administradores. Se `mustChangePassword`, CTA destacado para trocar a senha. |
| `AdminCoachListPage` (`/admin/coaches`) | `DataTable`: Coach (nome + e-mail), Status, Acesso, Atletas, Criado em, Ações. Busca por nome/e-mail e filtro por status. Botão "Novo coach". |
| `AdminCoachCreateModal` (na lista) | Nome + e-mail; explica que será enviado um convite. Após criar: exibe link, botão "Copiar link" e aviso se o e-mail não pôde ser enviado. |
| `AdminCoachDetailPage` (`/admin/coaches/:coachId`) | Cabeçalho com nome/e-mail/status; contagens de atletas (ativos/pausados/finalizados); tabela de atletas (nome, status, objetivo, acesso, desde); ações suspender/reativar/arquivar com motivo obrigatório em `Modal`. Nenhum link para dieta/treino/avaliação. |
| `AdminAccountsPage` (`/admin/admins`) | Tabela de admins (usuário, e-mail, criado em, estado da senha); botão "Novo admin"; ação "Resetar senha" (não aparece para a própria conta). |
| `CreateAdminModal` | Usuário, e-mail, senha inicial + confirmação; aviso de troca obrigatória no primeiro login. |
| `ChangePasswordPage` (`/admin/alterar-senha`) | Senha atual + nova + confirmação; usada no primeiro login e na troca voluntária. |
| `AcceptCoachInvitePage` (`/convite-coach/:token`) | Público, `AuthLayout`; mostra nome/e-mail do coach e validade; senha + confirmação; ao aceitar, sessão aberta e redirect para `/coach`. |

Reaproveitar obrigatoriamente: `DataTable`, `StatusBadge`, `Button`, `ButtonLink`, `Field`, `SelectField`, `Modal`, `StateMessage`, `AuthLayout` e tokens de `src/styles/tokens.css`.

### 8.4 Remoção do cadastro público

- Remover `RegisterPage` e a rota `/registro`.
- Remover o link "Ainda não tem conta? Criar conta" do `LoginPage`.
- Ajustar o texto do login para "Acesse sua conta ProCoach (coach, atleta ou administrador)".
- Atualizar `README.md` do frontend para explicar que coach só entra por convite.

### 8.5 Mocks MSW

- `MockCoach` deixa de ser um objeto único: `mockDb.coaches: MockCoach[]` + `mockDb.currentCoachId` (sessão do coach).
- `MockApiAthlete` ganha `coachId`; handlers de atleta filtram pela carteira do coach logado.
- Novo `MOCK_ADMIN_CREDENTIALS` (`admin@procoach.local` ou username `admin`) e botão "Admin" no `MockBanner`.
- Novo `src/mocks/handlers/admin.handlers.ts` cobrindo listagem, detalhe, atletas por coach, criação com convite, reenvio, mudança de status, admins e senhas.
- Seed com pelo menos 3 coaches (1 ativo com atletas, 1 ativo com convite pendente, 1 suspenso) e 2 admins.
- Atualizar `src/mocks/mocks.test.ts` e `resetMockDb`.

---

## 9. Testes

### Backend

- `ModulithArchitectureTest` continua verde (admin só depende de `*.application`).
- `SecurityConfig`: `/api/v1/admin/**` responde 403 para `COACH`/`ATHLETE` e 200 para `ADMIN`; `/registro`/`POST /auth/register` não existe mais.
- `AdminCoachService`: criação transacional, 409 de e-mail duplicado, bloqueio de cancelamento com atletas ativos, transições inválidas, auditoria correta.
- `CoachInviteService`: token não recuperável, uso único, expiração, revogação, aceite habilita o login.
- `AdminAccountService`: bootstrap idempotente, username/e-mail duplicados, troca de senha exige senha atual, reset invalida refresh, senha default rejeitada após troca.
- `AuthService`: login por username (`admin`) e por e-mail; coach suspenso/cancelado não loga nem renova; admin recebe `role=ADMIN` e `mustChangePassword=true` no primeiro acesso.
- `AdminCoachController`/`AdminAccountController`: contratos, 401/403/404/409 e `PASSWORD_CHANGE_REQUIRED`.

### Frontend

- Vitest: `admin.ts` (URLs/payloads), `AdminCoachListPage`, `AdminCoachDetailPage`, `AdminAccountsPage`, `ChangePasswordPage`, `AcceptCoachInvitePage`, `ProtectedRoute` com `ADMIN`, redirecionamento por `PASSWORD_CHANGE_REQUIRED`.
- `mocks.test.ts` para os novos handlers.
- E2E Playwright (opcional no MVP): admin default faz login, é forçado a trocar a senha, cria coach, copia link, aceita em contexto anônimo, coach cai em `/coach`; admin vê o atleta no detalhe; cria segundo admin.

---

## 10. Tasks sugeridas (IDs globais)

> A numeração usada chega a T-101. Propor T-102..T-111 no `TASKS.md` (backend/frontend conforme o caso) e uma US nova no `USER_STORIES.md`.

| Task | Camada | Descrição | Est. | Depende de |
|---|---|---|---|---|
| **T-102** | Backend (security) | Papel `ADMIN`, `MANAGE_COACHES`, bootstrap `admin`/`procoach`, login por username/e-mail, `mustChangePassword`, precedência no `primaryRole`, bloqueio de coach não ativo no login/refresh e remoção do cadastro público. | 3d | — |
| **T-103** | Backend (security) | Convite de coach: domínio, repositório, migration `coach_invite`, serviço, e-mail, endpoints públicos de preview/aceite. | 2d | T-102 |
| **T-104** | Backend (admin) | Módulo `admin`: fachadas de leitura em coach/athlete/security + `GET /admin/coaches`, `/coaches/{id}`, `/coaches/{id}/athletes`, `/summary`. | 2d | T-102 |
| **T-105** | Backend (admin) | Criação de coach por convite, reenvio/revogação, suspender/reativar/arquivar, auditoria e regras de bloqueio. | 2d | T-103, T-104 |
| **T-106** | Backend (security) | Gestão de admins: `GET/POST /admin/admins`, `PUT /admin/me/password`, `PUT /admin/admins/{id}/password`, invalidação de refresh, auditoria. | 2d | T-102 |
| **T-107** | Frontend | Role `ADMIN`, `homeForRole`, `ProtectedRoute`, `RoleRedirect`, shell/rotas `/admin`, remoção de `/registro`, fluxo de troca obrigatória de senha. | 2d | T-102 |
| **T-108** | Frontend | Lista de coaches + filtros + modal de criação/convite + copiar link. | 2d | T-104, T-107 |
| **T-109** | Frontend | Detalhe do coach + tabela de atletas + ações de status com motivo. | 2d | T-105, T-108 |
| **T-110** | Frontend | Página de administradores (listar/criar/resetar) + tela de alteração da própria senha + aceite do convite de coach (`/convite-coach/:token`). | 3d | T-106, T-107, T-103 |
| **T-111** | QA/Mocks | Sincronizar mocks MSW (vários coaches, `coachId` no atleta, credencial admin), testes Vitest/Playwright e checklist de aceite. | 2d | T-108, T-109, T-110 |

**Total estimado:** ~22 dias de desenvolvimento + QA.

### Ordem de entrega sugerida

1. T-102 (base de acesso, default admin, convite-only).
2. T-103 + T-106 (onboarding de coach e gestão de admins no backend).
3. T-104 + T-105 (API administrativa de coaches).
4. T-107 em paralelo com T-104 (infra de rotas do front).
5. T-108, T-109, T-110 (telas).
6. T-111 (mocks, testes, aceite).

---

## 11. Critérios de aceite (rascunho de US)

**US-043 — Como administrador, quero gerenciar coaches e consultar os atletas de cada um, para operar a plataforma com segurança.**

1. O sistema cria `admin`/`procoach` no primeiro boot (se ainda não houver admin) e exige troca de senha no primeiro login.
2. O admin autentica com usuário ou e-mail e é direcionado a `/admin`; coach e atleta não acessam essas rotas nem a API administrativa.
3. A lista mostra nome, e-mail, status, quantidade de atletas e estado de acesso de cada coach, com busca e filtro por status.
4. O admin adiciona um coach informando nome e e-mail; o coach recebe um convite de uso único (7 dias) e define a própria senha.
5. E-mail já cadastrado retorna 409 com mensagem acionável; o convite pode ser reenviado (revogando o anterior).
6. O detalhe do coach lista os atletas da carteira com status, objetivo e estado de acesso, sem exibir conteúdo clínico.
7. O admin suspende, reativa e arquiva coaches com motivo obrigatório; coach suspenso/arquivado não loga e não renova token.
8. Arquivamento é soft-delete (`CANCELLED`); não existe DELETE físico; cancelar com atletas ativos retorna 409.
9. O admin cria outros admins, altera a própria senha (exigindo a atual) e reseta a senha de outro admin, tudo auditado.
10. O cadastro público de coach não existe mais; o único caminho é o convite do admin.
11. Toda escrita administrativa gera `AuditLog` (ator = admin autenticado); leituras não geram.
12. O modo mock (`npm run dev:mocks`) reproduz os cenários com múltiplos coaches, atletas por coach, admin default e troca de senha.

---

## 12. Decisões confirmadas e detalhes de implementação

| Tema | Decisão |
|---|---|
| Cadastro público de coach | Removido; convite-only. |
| Visão de atletas | Identidade/status/objetivo/aprovação, sem conteúdo clínico. |
| Cancelar com atletas ativos | Bloquear com 409. |
| Admin default | `admin` / `procoach`, criado só se não houver admin, com troca obrigatória no primeiro login. |
| Gestão de admins | Página para listar/adicionar/resetar senha; troca da própria senha com senha atual. |
| Auditoria de leitura | Não. |

Detalhes ainda em aberto (podem ser resolvidos na implementação):

1. **Política de senha:** mínimo 8 caracteres e bloqueio de `procoach`; avaliar exigir letra + número.
2. **Reset de senha entre admins:** qualquer admin pode resetar outro (não há super-admin). Se o time preferir, criar um `SUPER_ADMIN` depois.
3. **E-mail do admin default:** `admin@procoach.local` (sem envio real) vs permitir configurar um e-mail real no bootstrap.
4. **Desativação de admin:** fora do MVP; se entrar, proteger o último admin.

---

## 13. Riscos e mitigação

| Risco | Mitigação |
|---|---|
| Senha default `procoach` conhecida | Criada só se não houver admin; troca obrigatória; bloqueio de reutilização; aviso no startup; override por ambiente em produção. |
| Admin preso em conta compartilhada/genérica | Cada admin tem username/e-mail próprio; ações auditadas por `sub`; reset de senha auditado. |
| Sessão antiga continuar após troca/reset de senha | `refresh` recusa tokens com `iat` anterior a `passwordChangedAt`; access token expira em 15 min. |
| Vazamento de PII/dados de saúde para o admin | DTO administrativo mínimo; sem endpoints clínicos; leituras não auditadas por decisão do PO. |
| Coach suspenso continuar com token válido por 15 min | Endpoints de coach validam `Coach` ACTIVE a cada requisição; login/refresh bloqueados. |
| N+1 ao montar a lista de coaches | Fachadas com métodos em lote (`emailsByUserIds`, `countActiveByCoachIds`); paginação se a base crescer. |
| Ciclo entre módulos | `admin` só consome APIs públicas; nenhum módulo importa `admin`. Validado pelo Modulith. |
| Convite por e-mail falhar | Fallback do atleta: link retornado uma vez para o admin copiar e repassar. |
| Remoção do `/registro` quebrar links antigos | Redirecionar `/registro` para `/login` com mensagem "o cadastro de coach é por convite". |
| Multi-papel (coach + admin na mesma conta) | Fora do MVP; se necessário, migrar o claim `role` para `roles[]` em task dedicada. |

---

## 14. Resumo executivo

O caminho de menor risco é: **papel `ADMIN` separado + cadastro de coach apenas por convite + API administrativa read-mostly expondo identidade/status + gestão de admins com troca de senha obrigatória no primeiro acesso + auditoria em toda escrita**. Isso reaproveita o que já está pronto (ciclo de vida do `Coach`, convite hasheado do atleta, guards, `AuditLog`, componentes de UI e mocks), remove a superfície do cadastro público e mantém o admin fora do domínio clínico do coach. A implementação fica em 10 tasks (~22 dias), começando pelo acesso default e pelo convite.
