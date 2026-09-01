# AGENTS.md — Contexto do projeto ProCoach

> Lido automaticamente pelo Abacus AI Desktop no início de cada sessão neste projeto.
> Fontes completas: `AGENT.md`, `SPEC.md`, `ProCoach_System_Design_UML.md`, `TASKS.md`,
> `USER_STORIES.md` — todos na raiz do repo. Leia-os antes de gerar código.

## Sobre o projeto

ProCoach é um SaaS B2B coach-led para fisiculturismo. O coach é o cliente (tenant): gerencia
atletas, monta avaliações, dietas versionadas e treinos, e valida decisões com um serviço de IA
(RAG) que é apenas consultivo e read-only. Núcleo em Java 21 + Spring Boot 4 (Modular Monolith,
Spring Modulith), RAG em Python + LlamaIndex, Postgres + pgvector, Flyway, RabbitMQ.

## Regras inegociáveis (nunca violar)

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

## Comandos úteis

```bash
docker compose up -d          # sobe Postgres+pgvector local
./mvnw flyway:migrate         # roda migrações
./mvnw test                   # testes + verificação de boundaries do Spring Modulith
./mvnw spring-boot:run        # roda o núcleo
```
