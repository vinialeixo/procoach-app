---
version: 1
slug: "frontend-src-app-tsx"
primary_target: "frontend/src/App.tsx"
related_targets: []
---

# Surface Brief: ProCoach Application Redesign

- **Scope:** Full application (`Login`, `Coach Workbench`, `Athlete Mobile Area`)
- **Mode:** Operate
- **Chosen Sketch:** `.impeccable/mocks/decision/assigned.html`

## Direction contract

THESIS: Every prescription screen behaves like an athletics federation dossier—opening with live protocol lineage (`v2 -> v3`) and clinical deltas (`Peso`, `GET`, `Δ kcal`) instead of generic SaaS KPI cards or stacked CRUD forms.

OWN-WORLD: Limestone ground (`#F3F4F0`), pure chalk work surfaces (`#FFFFFF`), deep pavilion navy navigation rail (`#0F1B2D`) on desktop collapsing to a thumb-zone bottom bar on mobile (`<640px`), hairline `1px` borders (`#D8DCD3`), Track Rust (`#B84A27`) reserved strictly for version lineage and active markers, Clinical Emerald (`#1F6E4A`) for approved states, `Archivo` headings, `Public Sans` UI copy, and `JetBrains Mono` for real clinical/numeric data.

STORY: Coaches immediately see pending reviews, expiring invites, and the active athlete's physiological baseline without switching tabs; athletes on mobile see today's workout and approved meals in crisp tabular clarity.

FIRST VIEWPORT: Left `248px` `#0F1B2D` navigation rail (`Pulso Clínico`, `Atletas`, `Treinos & Biblioteca`, `Auditoria`); main `#F3F4F0` canvas led by a `ProtocolLineageBar` (`v2 -> v3`, `Peso`, `GET`, primary CTA) above a 3-card clinical pulse strip and a 2-column master-detail / comparison split.

FORM: Candidate 3 ("Dossiê de Federação" / Track & Field Federation Dossier), position 3 on the grounded list, `seed:9d39865fb11afc56`.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
