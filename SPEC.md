# ProCoach — Especificação (SPEC)

> Plataforma **coach-led** baseada em evidências para profissionais de fisiculturismo.
> Alta rastreabilidade e auditabilidade. Núcleo em Java 21 + Spring Boot 3.x; RAG read-only em Python.

---

## 1. Visão Geral

ProCoach é um SaaS B2B onde o **coach é o cliente (tenant)**. Ele gerencia atletas, monta
avaliações, dietas versionadas e planos de treino, e valida decisões com um assistente de IA (RAG)
que é **apenas consultivo e read-only**.

### Princípios inegociáveis
- A plataforma **não substitui o coach**.
- A **decisão final sempre pertence ao coach**.
- **Alterações nunca são sobrescritas** — versionamento + auditoria imutável.

---

## 2. Personas

| Persona | Papel | Descrição |
|---|---|---|
| Coach | `COACH` | Cliente da plataforma. Autor de dietas/treinos, gerencia atletas, valida com RAG. |
| Atleta | `ATHLETE` | Recebe planos, envia check-ins (MVP2), visualiza apenas os próprios dados. |

---

## 3. Requisitos Funcionais (RF)

| ID | Requisito |
|---|---|
| RF-01 | Coach autentica com e-mail/senha + MFA. |
| RF-02 | Coach adiciona e remove atletas (soft-delete). |
| RF-03 | Coach cria avaliação física; sistema calcula IMC, TMB e TDEE automaticamente. |
| RF-04 | Coach monta dieta com refeições e macros. |
| RF-05 | Dieta passa por workflow de aprovação; cada alteração gera nova versão. |
| RF-06 | Coach valida dieta/treino consultando o RAG (evidências + citações), sem obrigatoriedade. |
| RF-07 | Coach monta plano de treino com periodização, séries, reps e carga. |
| RF-08 | Treino é versionado; alterações nunca sobrescrevem versões anteriores. |
| RF-09 | Biblioteca de exercícios reutilizável referenciada nos treinos. |
| RF-10 | Toda ação relevante é registrada no AuditLog (autor, ação, entidade, timestamp). |
| RF-11 | Atleta só acessa os próprios dados (RBAC). |
| RF-12 | (MVP2) Atleta envia check-ins; IA faz análise comparativa. |
| RF-13 | (MVP2) Base de conhecimento científica versionada, com ingestão de PDFs. |

---

## 4. Requisitos Não Funcionais (RNF)

| ID | Requisito |
|---|---|
| RNF-01 | Auditabilidade total: AuditLog append-only, imutável. |
| RNF-02 | Rastreabilidade: versionamento de agregados críticos (Diet, TrainingPlan, Assessment). |
| RNF-03 | Segurança: RBAC + MFA; isolamento multi-tenant por coach. |
| RNF-04 | RAG stateless e read-only — nunca escreve no domínio. |
| RNF-05 | Arquitetura modular (Spring Modulith) com boundaries verificáveis. |
| RNF-06 | Testes automatizados com Testcontainers para persistência/auditoria. |
| RNF-07 | Observabilidade: Actuator (health, métricas). |
| RNF-08 | Migrações versionadas (Flyway). |

---

## 5. Modelo de Domínio (resumo)

| Agregado | Módulo | Chaves de referência | Observação |
|---|---|---|---|
| Coach | `coach` | userId | Cliente/tenant; autor de dietas/treinos/avaliações |
| Athlete | `athlete` | coachId | Gerenciado pelo coach; soft-delete |
| Assessment | `assessment` | coachId, athleteId | Imutável; cálculos automáticos |
| Diet | `nutrition` | coachId, athleteId | Versionada; workflow de aprovação |
| TrainingPlan | `training` | coachId, athleteId | Versionado; periodização |
| Exercise | `library` | — | Catálogo reutilizável |
| AuditLog | `audit` | author (=coach) | Append-only; rastreia todas as ações |

Referências entre agregados são feitas por **ID** (DDD), não por objeto aninhado.

---

## 6. Regras de Negócio (RN)

- **RN-01** — Apenas o coach dono (coachId) pode alterar atletas, dietas e treinos.
- **RN-02** — Cada aprovação/alteração de Diet/TrainingPlan cria uma **nova versão**; versões anteriores permanecem consultáveis.
- **RN-03** — O RAG apenas retorna sugestões e citações; a gravação da decisão é feita pelo núcleo (coach).
- **RN-04** — Exclusão de atleta é **lógica** (soft-delete), preservando histórico.
- **RN-05** — Toda operação de escrita registra evento no AuditLog.

---

## 7. Contratos de API (esboço REST)

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/v1/auth/login` | Login + MFA |
| POST | `/api/v1/athletes` | Adicionar atleta |
| DELETE | `/api/v1/athletes/{id}` | Remover atleta (soft-delete) |
| POST | `/api/v1/athletes/{id}/assessments` | Criar avaliação |
| POST | `/api/v1/athletes/{id}/diets` | Criar/nova versão de dieta |
| POST | `/api/v1/diets/{id}/approve` | Aprovar dieta |
| POST | `/api/v1/athletes/{id}/training-plans` | Criar/nova versão de treino |
| POST | `/api/v1/advisory/validate` | (MVP2) Validar dieta/treino com RAG |
| GET | `/api/v1/audit` | Consultar trilha de auditoria |

Comunicação Core ↔ RAG: **gRPC read-only**.

---

## 8. Stack

Java 21, Spring Boot 3.x, Spring Modulith, JPA/Hibernate, Flyway, Spring Security (RBAC+MFA),
RabbitMQ/Kafka, PostgreSQL + pgvector, S3 (MinIO/AWS), Python + LlamaIndex + LlamaParse (RAG), gRPC.

---

## 9. Escopo por Release

- **MVP (v1.0)** — Fundação, security, audit, coach, athlete, assessment, nutrition, training, library. Web-first, sem IA.
- **MVP2 (v1.1)** — Knowledge Base, RAG/IA (validação com citações), check-ins.
- **Pós-MVP2** — App mobile, analytics avançado, expansão de nicho.

---

## 10. Critérios de Aceite (globais)

- Nenhuma alteração sobrescreve dados: sempre nova versão.
- Toda ação de escrita gera registro auditável.
- Atleta nunca acessa dados de outro atleta/coach.
- RAG não possui permissão de escrita no domínio.
