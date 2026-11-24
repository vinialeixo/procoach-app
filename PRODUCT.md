# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- **Primary user — Independent coaches & sports/nutrition consultants (`ROLE_COACH`):** Operate primarily on desktop during athlete consultations, protocol planning, and periodic check-ins to manage their roster, physical assessments, diets, and training plans.
- **Secondary user — Athletes (`ROLE_ATHLETE`):** Access the platform primarily on mobile to consult their current approved diet, active training plan/sessions, assessment history, and notifications in read-only mode.

## Product Purpose

ProCoach enables independent coaches and sports/nutrition consultancies to manage the full athlete lifecycle—onboarding via single-use invite, anthropometric assessments, versioned nutrition plans, periodized training prescriptions, and athlete notifications—in a single multi-tenant platform with strict coach ownership.

## Positioning

Clinical traceability and immutable versioning that replaces fragmented spreadsheets and WhatsApp threads with an auditable ledger where 100% of prescribing and approval decisions remain with the coach. Unlike generic consumer fitness apps or AI-automated planners, ProCoach never overwrites historical protocols (`Diet`, `TrainingPlan`, `Assessment`, `Exercise`) and keeps any future AI/RAG strictly consultative and read-only.

## Operating Context

- **Coach workflow:** Register/login (`/login`, `/registro`), create athletes (`/coach/athletes/new`), send single-use 7-day invite links (`/convite/:token`), record anthropometric assessments (`/coach/athletes/:id/assessments/new` with BMI, BMR/TMB, TDEE/GET, and side-by-side comparison), author and version diets (`DRAFT` → `IN_REVIEW` → `APPROVED` → `ARCHIVED` with meals, food items, and energy balance vs. TDEE), manage exercise library and training periodizations/plans/sessions (`/coach/training`), and inspect immutable history (`AuditLog`).
- **Athlete workflow:** Accept invite, review daily home summary (`/athlete`), read approved diet (`/athlete/dieta`), read active training plan (`/athlete/treino`), track assessment evolution (`/athlete/avaliacoes`), and manage notification preferences/push devices (`/athlete/avisos`).
- **Runtime environment:** React 19 + TypeScript + Vite SPA (`frontend/`, port `5173`) backed by a Java 17 Spring Boot / Spring Modulith REST API (`backend/`, port `8080`, proxied via `/api`), plus standalone MSW mock mode (`npm run dev:mocks`).

## Capabilities and Constraints

- **Multi-tenant & RBAC:** Strictly enforced in the backend (`ROLE_COACH`, `ROLE_ATHLETE`, server-side coach ownership / anti-IDOR). The frontend reflects role-based routes and handles `401`/`403` without making authorization decisions.
- **Immutable versioning & soft-delete:** `Diet`, `TrainingPlan`, `Assessment`, and `Exercise` are never overwritten in place—edits create chained versions; deletions are soft-delete only; significant writes append to `AuditLog`.
- **Coach-led boundary:** No automated approval or generation of protocols; any AI/RAG capability is strictly consultative and read-only.
- **Frontend architecture:** Custom UI component library (`frontend/src/components/ui`) and CSS design tokens (`frontend/src/styles/tokens.css`) without external UI kits (no MUI/Chakra).
- **Language:** Brazilian Portuguese (`pt-BR`).
- **Undecided / open:** Formal accessibility target (e.g., WCAG AA compliance) and multi-language support are not yet established.

## Brand Commitments

- **Product name:** ProCoach.
- **Language:** Brazilian Portuguese (`pt-BR`) UI copy and domain terminology.
- **Conceptual identity:** "Clinical ledger" (*ledger clínico*) — grounded in evidence, versioning, and auditability rather than generic gym/consumer-SaaS aesthetics.

## Evidence on Hand

- **Codebase & specs:** [README.md](README.md), [backend/SPEC.md](backend/SPEC.md), [docs/nutricao-plano-alimentar.md](docs/nutricao-plano-alimentar.md), [frontend/README.md](frontend/README.md), and [frontend/src/App.tsx](frontend/src/App.tsx).
- **Working mock dataset:** Standalone MSW mock database (`frontend/src/mocks/db.ts`) for local demonstration (`npm run dev:mocks`).
- **Absences:** No marketing testimonials, customer case studies, pricing tiers, or external press assets exist in the repository; future surfaces must not fabricate them.

## Product Principles

1. **Coach decides, system records:** Every clinical or training decision belongs to the coach; automation and AI only inform, never approve or overwrite.
2. **History is sacred:** Protocols, assessments, and exercises are versioned immutably and audited append-only so past prescriptions can always be inspected and compared.
3. **Precision over decoration:** Surface real clinical/training data (macros, energy balance, load, RPE/RIR, version lineage) clearly for fast scanning during consultations and workouts.
4. **Role-appropriate ergonomics:** Optimize coach workflows for dense desktop authoring and comparison, and athlete workflows for clear mobile consumption.
5. **Single source of truth:** Keep frontend state, MSW mocks, and backend domain contracts aligned without silent fallbacks.
