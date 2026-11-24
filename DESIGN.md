---
name: ProCoach
description: Track & Field Federation Dossier visual system for coach-led sports training and nutrition.
colors:
  pavilion-navy: "#0f1b2d"
  pavilion-navy-raised: "#192a44"
  pavilion-navy-soft: "#e7ecf3"
  track-rust: "#b84a27"
  track-rust-soft: "#f7eae4"
  track-rust-ink: "#8d371b"
  limestone: "#f3f4f0"
  pure-chalk: "#ffffff"
  slate-muted: "#526071"
  chalk-ink: "#f3f4f0"
  rail-muted: "#9fb0c7"
  hairline-border: "#d8dcd3"
  rail-border: "#2a3b55"
  clinical-emerald: "#1f6e4a"
  clinical-emerald-soft: "#e3f1ea"
  clinical-crimson: "#b3261e"
  clinical-crimson-soft: "#fce8e6"
typography:
  display:
    fontFamily: "Archivo, ui-sans-serif, system-ui, sans-serif"
    fontSize: "2rem"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "Archivo, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1.5rem"
    fontWeight: 700
    lineHeight: 1.25
    letterSpacing: "-0.01em"
  title:
    fontFamily: "Archivo, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 600
    lineHeight: 1.35
    letterSpacing: "-0.01em"
  body:
    fontFamily: "Public Sans, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: "0em"
  label:
    fontFamily: "Public Sans, ui-sans-serif, system-ui, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 600
    lineHeight: 1.4
    letterSpacing: "0.05em"
  mono:
    fontFamily: "JetBrains Mono, ui-monospace, SFMono-Regular, monospace"
    fontSize: "0.75rem"
    fontWeight: 500
    lineHeight: 1.4
    letterSpacing: "0em"
rounded:
  sm: "4px"
  md: "6px"
spacing:
  "1": "4px"
  "2": "8px"
  "3": "12px"
  "4": "16px"
  "5": "24px"
  "6": "32px"
  "8": "48px"
  "10": "64px"
components:
  button-primary:
    backgroundColor: "{colors.pavilion-navy}"
    textColor: "{colors.pure-chalk}"
    rounded: "{rounded.sm}"
    padding: "0 24px"
    height: "44px"
  button-primary-hover:
    backgroundColor: "{colors.pavilion-navy-raised}"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.pavilion-navy}"
    rounded: "{rounded.sm}"
    padding: "0 24px"
    height: "44px"
  input-default:
    backgroundColor: "{colors.pure-chalk}"
    textColor: "{colors.pavilion-navy}"
    rounded: "{rounded.sm}"
    padding: "0 16px"
    height: "44px"
  card-default:
    backgroundColor: "{colors.pure-chalk}"
    textColor: "{colors.pavilion-navy}"
    rounded: "{rounded.md}"
    padding: "24px"
  badge-success:
    backgroundColor: "{colors.clinical-emerald}"
    textColor: "{colors.pure-chalk}"
    rounded: "{rounded.sm}"
    padding: "2px 8px"
  badge-muted:
    backgroundColor: "{colors.track-rust-soft}"
    textColor: "{colors.track-rust-ink}"
    rounded: "{rounded.sm}"
    padding: "2px 8px"
  badge-neutral:
    backgroundColor: "transparent"
    textColor: "{colors.slate-muted}"
    rounded: "{rounded.sm}"
    padding: "2px 8px"
---

# Design System: ProCoach

## Overview

**Creative North Star: "Dossiê de Federação"**

ProCoach adopts the visual discipline of an international athletics federation dossier: high-clarity tabular precision, warm limestone ground, a committed deep pavilion navy navigation rail, and Track Rust markers reserved strictly for version lineage and active states. Every prescription screen opens with live clinical context (`ProtocolLineageBar`) so coaches never lose physiological baselines (`Peso`, `GET`, `TMB`) mid-consultation.

**Key Characteristics:**
- Precision over ornament: `1px` hairline borders, `tabular-nums`, and `JetBrains Mono` strictly for real clinical data.
- Committed desktop navigation rail (`248px`, `#0f1b2d`) collapsing to a thumb-zone bottom bar (`44px+` touch targets) on mobile (`<= 640px`).
- WCAG AA contrast discipline: small text and badges use `--color-accent-ink` (`#8d371b`) rather than raw `--color-accent` (`#b84a27`).
- Zero external UI kits (no MUI or Chakra); all primitives live in `src/components/ui` and consume `src/styles/tokens.css`.

## Colors

### Primary
- **Pavilion Navy** (`pavilion-navy`): Primary actions, desktop navigation rail, and focus outlines.
- **Pavilion Navy Raised** (`pavilion-navy-raised`): Hover state for primary buttons and active rail navigation items.
- **Pavilion Navy Soft** (`pavilion-navy-soft`): Subtle unread notification surface tint.

### Secondary
- **Track Rust** (`track-rust`): Active navigation dot indicator and version lineage accents.
- **Track Rust Soft** (`track-rust-soft`) & **Track Rust Ink** (`track-rust-ink`): High-contrast WCAG AA background and text for lineage tags (`v2 -> v3`), raw-weight badges, and supplement headers.

### Neutral
- **Limestone** (`limestone`): Global application canvas and table header ground.
- **Pure Chalk** (`pure-chalk`): Work cards, form inputs, modals, and data table rows.
- **Slate Muted** (`slate-muted`): Secondary descriptions, table column headers, and muted labels.
- **Chalk Ink** (`chalk-ink`) & **Rail Muted** (`rail-muted`): Primary and secondary text inside the dark navigation rail and `AuthLayout` ledger panel.
- **Hairline Border** (`hairline-border`) & **Rail Border** (`rail-border`): `1px` structural dividers on light and dark surfaces.

### Semantic
- **Clinical Emerald** (`clinical-emerald`) & **Clinical Emerald Soft** (`clinical-emerald-soft`): Approved/active protocol badges and success states.
- **Clinical Crimson** (`clinical-crimson`) & **Clinical Crimson Soft** (`clinical-crimson-soft`): Validation errors and alert banners.

### Named Rules
**The Restrained Lineage Rule.** Track Rust (`track-rust` / `track-rust-ink`) is reserved for version lineage, active navigation dots, and high-contrast clinical tags—never for primary CTAs or decorative fills.

## Typography

**Display Font:** Archivo (with ui-sans-serif, system-ui, sans-serif fallback)
**Body Font:** Public Sans (with ui-sans-serif, system-ui, sans-serif fallback)
**Label/Mono Font:** JetBrains Mono (with ui-monospace, SFMono-Regular, monospace fallback)

### Hierarchy
- **Display** (700, 2rem / 32px, line-height 1.2, letter-spacing -0.02em): Authentication split-screen wordmark and primary KPI figures.
- **Headline** (700, 1.5rem / 24px, line-height 1.25): Page-level `<h1>` headings.
- **Title** (600, 1.125rem / 18px, line-height 1.35): Card titles, modal titles, and `ProtocolLineageBar` headings.
- **Body** (400, 1rem / 16px, line-height 1.5): Default UI copy and form inputs.
- **Label** (600, 0.75rem / 12px, line-height 1.4, letter-spacing 0.05em, uppercase): Table headers and KPI labels.
- **Mono** (500, 0.75rem / 12px, `tabular-nums`): Version tags, `kcal`, `g/kg`, weights, loads, and audit timestamps.

### Named Rules
**The Real-Data Mono Rule.** `JetBrains Mono` with `tabular-nums` is reserved exclusively for real structured metrics, versions, and timestamps.

## Layout

Desktop (`> 640px`) uses a `248px` sticky left navigation rail (`pavilion-navy`) paired with a `1120px` max-width main content area on `limestone`. Multi-pane workbenches (`TrainingPage`) expand into a 2-column master-detail split (`1fr 1.15fr`) at `>= 960px`. Mobile (`<= 640px`) collapses the rail into a compact top utility header and a fixed bottom navigation bar with `44px+` touch targets.

## Elevation & Depth

Surfaces are flat by default and anchored by `1px` `hairline-border` strokes, with a quiet ambient `--shadow-card` (`0 1px 2px rgba(15, 27, 45, 0.05), 0 6px 18px rgba(15, 27, 45, 0.04)`) on cards, tables, lineage bars, and modals.

### Named Rules
**The Border-First Rule.** Structural hierarchy comes from `1px` borders and surface contrast (`limestone` vs. `pure-chalk`) first; `--shadow-card` provides subtle lift without heavy drop shadows.

## Shapes

Tight two-step architectural radius scale: `4px` (`--radius-sm`) for buttons, inputs, badges, and rail links; `6px` (`--radius-md`) for cards, tables, lineage bars, and modals.

## Components

- **Buttons:** `44px` height, `4px` radius, `fullWidth` opt-in (`pc-button--auto`), fast `120ms ease` transitions.
- **ProtocolLineageBar:** Clinical context strip (`lineageTag`, `title`, `metrics` in `JetBrains Mono`, optional `action`) anchored at the top of diet/training workflows.
- **Navigation Rail / Bottom Bar:** `248px` sticky dark rail on desktop with `6px` Track Rust active dot; fixed bottom bar on `<= 640px`.
- **Confirmation Modals:** Required for irreversible protocol transitions (`PlanOverviewView`) and meal removals (`DietMealsPage`).

## Do's and Don'ts

### Do:
- **Do** reuse `src/components/ui` primitives (`ProtocolLineageBar`, `Modal`, `Button`, `DataTable`, `StateMessage`) and `tokens.css`.
- **Do** use `--color-accent-ink` (`#8d371b`) for small text and badges to preserve WCAG AA contrast.
- **Do** maintain `>= 44px` touch targets on all buttons, inputs, day-filter pills, and mobile nav links.

### Don't:
- **Don't** use `border-left` or `border-right` thicker than `1px` on cards, list items, or callouts.
- **Don't** introduce external UI kits (MUI, Chakra) or generic consumer-fitness aesthetics.
