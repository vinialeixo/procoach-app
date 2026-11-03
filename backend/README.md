# Pro Coach

> An evidence-based, **coach-led** platform for bodybuilding professionals — built for high traceability, auditability, and scientific rigor.

[![Status](https://img.shields.io/badge/status-in%20development-blue)]()
[![Backend](https://img.shields.io/badge/backend-Java%2017%20%2B%20Spring%20Boot%203.x-green)]()
[![RAG](https://img.shields.io/badge/RAG-Python%20%2B%20LlamaIndex-yellow)]()
[![DB](https://img.shields.io/badge/db-PostgreSQL%20%2B%20pgvector-blue)]()

---

## Overview

**procoach** is a B2B SaaS platform that helps bodybuilding coaches manage athletes,
physical assessments, versioned diets and training programs, and a scientific knowledge base.

The product is built on a single, non-negotiable philosophy:

- **The platform does not replace the coach.**
- **The final decision always belongs to the coach.**
- **Changes are never overwritten** — every modification is versioned and auditable.

An optional AI assistant (RAG) suggests, justifies, and cites evidence — but it is strictly
**read-only** and never makes decisions on its own.

---

## Core Principles

- **Domain-Driven Design (DDD)** — rich domain model organized by bounded contexts.
- **Clean Architecture** — the domain layer is framework-agnostic.
- **SOLID & Clean Code** — maintainable, testable, decoupled components.
- **Event-Driven Architecture** — domain events + asynchronous integration.
- **Coach-led AI** — assistive, evidence-based, and read-only.
- **Full auditability** — immutable audit log and chained versioning.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Transactional core | Java 17 + Spring Boot 3.x |
| Modular monolith | Spring Modulith |
| Persistence | JPA/Hibernate + Flyway |
| Event Sourcing / CQRS | Axon Framework (or a manual PostgreSQL event store) |
| Security | Spring Security (RBAC + MFA) |
| Messaging | RabbitMQ / Kafka |
| Database | PostgreSQL + pgvector |
| Object storage | S3-compatible (MinIO / AWS S3) |
| RAG service | Python + LlamaIndex + LlamaParse |
| Core <-> RAG communication | gRPC (read-only) |
| Frontend (MVP) | Web SPA |

---

## Architecture

The system is a **modular monolith** (Spring Modulith) with clearly bounded contexts. A separate,
**stateless and read-only** Python RAG service handles AI-assisted suggestions over a corpus of
scientific PDFs.

```
                 Web SPA
                    |
                    v  HTTPS / REST
   +--------------------------------------------+
   |  Transactional Core (Java / Spring Boot)   |
   |  Athletes | Assessment | Nutrition |       |
   |  Training | Library | Check-ins |          |
   |  Security/RBAC | Audit / Event Store        |
   +------+-----------------------+-------------+
          | JPA/Hibernate         | gRPC (read-only)
          v                       v
   PostgreSQL + pgvector   RAG Service (Python)
          ^                  LlamaIndex + LlamaParse
          | Object Storage (S3)      | events
   PDFs / Photos            Message Broker (RabbitMQ/Kafka)
```

### Key Architecture Decisions (ADRs)
- **ADR-001** — Modular monolith in Java/Spring Boot with Spring Modulith.
- **ADR-002** — Selective Event Sourcing for critical aggregates (Diet, Training, Protocol).
- **ADR-003** — Isolated, read-only, stateless Python RAG service (the AI never writes to the domain).
- **ADR-004** — LlamaIndex + LlamaParse for high-quality PDF parsing and cited answers.
- **ADR-005** — Web-first delivery; mobile app in a later phase.

---

## Bounded Contexts (Epics)

| Epic | Context |
|---|---|
| E1 | Athlete management (multiple protocols, status, soft-delete) |
| E2 | Physical assessment (auto calculations: BMI, BMR, TDEE, evolution) |
| E3 | Nutrition (versioned diets + approval workflow) |
| E4 | Training (periodization, sets/reps/load/intensity) |
| E5 | Check-ins (athlete submissions + AI comparative analysis) |
| E6 | AI assistant (RAG, with citations and read-only guardrail) |
| E7 | Knowledge base (versioned scientific concepts) |
| E8 | Exercise library |
| E9 | Security (RBAC: Coach vs. Athlete + MFA) |
| E10 | Auditing & versioning (cross-cutting) |

---

## Roadmap

### MVP (v1.0) — Coach-led core (Web)
Foundation (Spring Modulith, event store, RBAC + MFA, audit log, infra) + E1, E2, E3, E4, E8, E9, E10.
**No AI/RAG and no mobile yet.**

### MVP2 (v1.1) — Intelligence & engagement
E7 Knowledge Base, E6 RAG/AI assistant (cited suggestions + read-only guardrail), E5 Check-ins.

### Post-MVP2
Mobile app (API reuse), advanced analytics/reports, expansion to other sports/niches.

---

## Getting Started

> Prerequisites: JDK 17, Maven/Gradle, Docker, Python 3.11+ (for the RAG service).

```bash
# 1. Clone the repository
git clone https://github.com/<org>/procoach.git
cd procoach

# 2. Start infrastructure (PostgreSQL + pgvector, broker, object storage)
docker compose up -d

# 3. Run database migrations
./mvnw flyway:migrate

# 4. Start the transactional core
./mvnw spring-boot:run

# 5. (MVP2) Start the RAG service
cd rag-service
pip install -r requirements.txt
python -m app.main
```

Environment variables (example):

```env
DB_URL=jdbc:postgresql://localhost:5432/team_kel_souza
DB_USER=postgres
DB_PASSWORD=changeme
S3_ENDPOINT=http://localhost:9000
BROKER_URL=amqp://localhost:5672
RAG_GRPC_ENDPOINT=localhost:50051
```

---

## Testing

```bash
# Unit + integration tests (core)
./mvnw test

# RAG service tests
cd rag-service && pytest

# Sem Docker: testes de integração em H2 (modo PostgreSQL); o schema vem das entidades JPA
# e as migrações Flyway não são exercitadas. Use o comando acima (Testcontainers) para validá-las.
./mvnw test -Dspring.profiles.active=h2
```

Testing focuses heavily on **auditability and non-overwriting versioning**, RBAC authorization,
diet approval workflow, and the AI read-only guardrail.

---

## Contributing

1. Create a feature branch from `main`.
2. Follow DDD, Clean Architecture, and Clean Code conventions.
3. Every domain change must preserve versioning and produce audit entries.
4. Open a Pull Request with a clear description and passing tests.

---

## License

Proprietary — All rights reserved to procoach.
