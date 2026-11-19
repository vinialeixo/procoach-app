---
name: ProCoach
description: Clinical-ledger visual system for coach-led sports training and nutrition.
colors:
  clinical-navy: "#1f3b57"
  clinical-navy-deep: "#16293d"
  copper-amber: "#c97a2b"
  copper-amber-soft: "#f3e3d1"
  copper-amber-ink: "#8a4f12"
  cool-paper: "#eef1f4"
  pure-surface: "#ffffff"
  graphite-dark: "#121826"
  graphite-raised: "#1a2233"
  slate-muted: "#5b6472"
  ice-ink: "#e9ecf1"
  slate-on-dark: "#8891a3"
  hairline-border: "#d9dee4"
  dark-border: "#2a3346"
  clinical-green: "#2f6e4c"
  clinical-red: "#a23b3b"
  clinical-red-soft: "#f6e4e4"
typography:
  display:
    fontFamily: "Space Grotesk, ui-sans-serif, system-ui, sans-serif"
    fontSize: "2rem"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "-0.01em"
  headline:
    fontFamily: "Space Grotesk, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1.5rem"
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: "-0.01em"
  title:
    fontFamily: "Space Grotesk, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 600
    lineHeight: 1.35
    letterSpacing: "-0.01em"
  body:
    fontFamily: "Inter, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: "0em"
  label:
    fontFamily: "Inter, ui-sans-serif, system-ui, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 600
    lineHeight: 1.4
    letterSpacing: "0.04em"
  mono:
    fontFamily: "IBM Plex Mono, ui-monospace, SFMono-Regular, monospace"
    fontSize: "0.75rem"
    fontWeight: 400
    lineHeight: 1.4
    letterSpacing: "0em"
rounded:
  sm: "4px"
  md: "8px"
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
    backgroundColor: "{colors.clinical-navy}"
    textColor: "{colors.pure-surface}"
    rounded: "{rounded.sm}"
    padding: "0 24px"
    height: "44px"
  button-primary-hover:
    backgroundColor: "{colors.clinical-navy-deep}"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.clinical-navy}"
    rounded: "{rounded.sm}"
    padding: "0 24px"
    height: "44px"
  input-default:
    backgroundColor: "{colors.pure-surface}"
    textColor: "{colors.graphite-dark}"
    rounded: "{rounded.sm}"
    padding: "0 16px"
    height: "44px"
  card-default:
    backgroundColor: "{colors.pure-surface}"
    textColor: "{colors.graphite-dark}"
    rounded: "{rounded.md}"
    padding: "24px"
  badge-success:
    backgroundColor: "{colors.clinical-green}"
    textColor: "{colors.pure-surface}"
    rounded: "{rounded.sm}"
    padding: "2px 8px"
  badge-muted:
    backgroundColor: "{colors.copper-amber-soft}"
    textColor: "{colors.copper-amber-ink}"
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

**Creative North Star: "O Ledger Clínico"**

ProCoach is built around the visual discipline of a clinical ledger: an instrument for evidence, immutable protocol versioning, and auditability. Rather than borrowing the loud, consumer-oriented tropes of generic fitness apps, the interface treats training plans, anthropometric assessments, and nutrition prescriptions as structured clinical records.

The system pairs a cool, high-legibility paper canvas with deep navy primary actions and restrained copper-amber accents reserved for active state indicators and version markers. Controls are precise and restrained—firm 4px and 8px geometry, crisp 1px borders, and fast 120ms transitions without decorative flourishes.

**Key Characteristics:**
- Precision and restraint over decoration: every visual element serves scannability, data comparison, or state clarity.
- Monospace typography is strictly reserved for real data (versions, timestamps, IDs, metrics) and never used as ornament.
- Contained hybrid elevation: 1px hairline borders anchor the layout, supported by subtle ambient card shadows only on elevated containers, tables, and modals.
- Zero external UI kits (no MUI or Chakra); all primitives live in `src/components/ui` and consume `src/styles/tokens.css`.

## Colors

The palette is anchored in cool neutral paper and deep graphite ink, paired with a single primary navy and a disciplined copper-amber accent.

### Primary
- **Navy Clínico** (`clinical-navy`): Primary actions, active focus outlines, and interactive card hover borders.
- **Navy Clínico Profundo** (`clinical-navy-deep`): Hover state for primary buttons.

### Secondary
- **Âmbar Cobre** (`copper-amber`): Active navigation underline indicator and version tags in the authentication ledger panel.
- **Âmbar Cobre Suave** (`copper-amber-soft`) & **Âmbar Cobre Escuro** (`copper-amber-ink`): Muted/draft status badge background and high-contrast label ink.

### Neutral
- **Papel Frio** (`cool-paper`): Global application canvas and table header background.
- **Superfície Pura** (`pure-surface`): Cards, form inputs, data table rows, modals, and top navigation bar.
- **Grafite Escuro** (`graphite-dark`): Primary body/heading ink and the dark brand panel background on authentication screens.
- **Grafite Elevado** (`graphite-raised`): Subtle radial highlight on the dark authentication panel.
- **Ardósia Neutra** (`slate-muted`): Secondary descriptions, table column headers, and inactive navigation links.
- **Tinta Gelo** (`ice-ink`) & **Ardósia sobre Escuro** (`slate-on-dark`): Primary and muted text on dark surfaces.
- **Borda Fina** (`hairline-border`) & **Borda Escura** (`dark-border`): 1px structural dividers on light and dark surfaces respectively.

### Semantic
- **Verde Clínico** (`clinical-green`): Approved/active status badges and success state messages.
- **Vermelho Clínico** (`clinical-red`) & **Vermelho Clínico Suave** (`clinical-red-soft`): Validation errors, `aria-invalid` field borders, and error banners.

### Named Rules
**The Restrained Accent Rule.** Copper-amber (`copper-amber`) is reserved strictly for active navigation indicators, version markers, and muted status badges—never for primary buttons or large surface fills.

## Typography

**Display Font:** Space Grotesk (with ui-sans-serif, system-ui, sans-serif fallback)
**Body Font:** Inter (with ui-sans-serif, system-ui, sans-serif fallback)
**Label/Mono Font:** IBM Plex Mono (with ui-monospace, SFMono-Regular, monospace fallback)

**Character:** Space Grotesk brings geometric authority to page titles and section headings, Inter keeps dense clinical tables and forms effortless to scan, and IBM Plex Mono gives version numbers, timestamps, and audit entries unmistakable tabular precision.

### Hierarchy
- **Display** (700, 2rem / 32px, line-height 1.2, letter-spacing -0.02em): Brand wordmark on authentication split screens.
- **Headline** (600–700, 1.5rem / 24px, line-height 1.25, letter-spacing -0.01em): Page-level `<h1>` headings inside the main shell.
- **Title** (600, 1.125rem / 18px, line-height 1.35, letter-spacing -0.01em): Card titles, modal titles, and shell header wordmark.
- **Body** (400, 1rem / 16px, line-height 1.5): Default prose, form input values, and descriptions.
- **Label** (600, 0.75rem / 12px, line-height 1.4, letter-spacing 0.04em, uppercase): Data table column headers and compact status badges.
- **Mono** (400, 0.75rem / 12px, line-height 1.4): Version identifiers, audit actions, and real clinical/numeric metadata.

### Named Rules
**The Real-Data Mono Rule.** `IBM Plex Mono` is used exclusively for real structured data (versions, IDs, timestamps, audit logs) and never as decorative styling on headings or marketing copy.

## Layout

The application shell uses a full-width sticky-feeling top header (`pure-surface` with a bottom `hairline-border`) and a centered single-column content container constrained to `960px` max-width with `64px` vertical margin (`48px`/`24px`–`32px` horizontal padding on desktop).

Dashboard card grids use responsive CSS Grid (`repeat(auto-fit, minmax(14rem, 1fr))`) with a `16px` gap. Authentication flows use a 50/50 split grid (`AuthLayout`) pairing the dark `graphite-dark` audit-ledger panel with a centered `380px` form column on `cool-paper`.

Two responsive breakpoints govern layout shifts:
- **Mobile (`max-width: 640px`):** The header wraps so the navigation bar drops to row 3 with horizontal scroll and `44px` minimum touch-target link height; content margins tighten to `24px` vertical and `16px` horizontal padding.
- **Auth split (`max-width: 860px`):** Collapses the 50/50 authentication grid to a single column, hiding the dark brand panel to prioritize the form.

## Elevation & Depth

ProCoach follows a contained hybrid elevation model: surfaces are flat by default and anchored by `1px` `hairline-border` strokes, while elevated containers (data tables, home action cards, and modals) carry a single subtle ambient shadow (`--shadow-card`: `0 1px 2px rgba(18, 24, 38, 0.06), 0 8px 24px rgba(18, 24, 38, 0.06)`). Modals sit over a `50%` `graphite-dark` backdrop (`rgba(18, 24, 38, 0.5)`).

### Named Rules
**The Border-First Rule.** Structural hierarchy comes from `1px` borders and surface contrast (`cool-paper` vs. `pure-surface`) first; `--shadow-card` is a quiet ambient lift for cards, tables, and modals, never a heavy drop shadow.

## Shapes

The form language is crisp and architectural with a tight two-step radius scale:
- **Small (`4px`):** Interactive controls—buttons, form inputs, textareas, status badges, and skip links.
- **Medium (`8px`):** Container surfaces—cards, data table wrappers, state message banners, and modals.

Borders are consistently `1px solid`, switching to `1px dashed` only for empty table states to signal an unpopulated slot.

## Components

Components are precise and restrained: firm `4px`/`8px` geometry, crisp `1px` borders, `44px` minimum touch targets on primary controls, and fast `120ms ease` transitions (honoring `prefers-reduced-motion: reduce`).

### Buttons
- **Shape:** Tight architectural corner (`4px` radius), `44px` height (`36px` in compact header actions), `0 24px` horizontal padding, `600` weight `0.875rem` text.
- **Primary:** `clinical-navy` background with `pure-surface` text; transitions to `clinical-navy-deep` on hover. Full-width by default inside forms; content-width when rendered as a `ButtonLink`.
- **Ghost:** Transparent background, `clinical-navy` text, `1px` `hairline-border`; border shifts to `clinical-navy` on hover.
- **Focus / Disabled:** Global `2px solid` `clinical-navy` outline with `2px` offset on `:focus-visible`; `0.6` opacity and `not-allowed` cursor when disabled.

### Chips / Status Badges
- **Style:** Compact `4px` radius pill-free tag (`2px 8px` padding, `0.75rem` `600` weight, `1px` border).
- **Variants:** `success` (`clinical-green` fill, `pure-surface` text), `muted` (`copper-amber-soft` fill, `copper-amber-ink` text and border), `neutral` (transparent fill, `slate-muted` text, `hairline-border`), and `dark` (`graphite-dark` fill, `slate-on-dark` text).

### Cards / Containers
- **Corner Style:** Softened architectural corner (`8px` radius).
- **Background & Border:** `pure-surface` background with `1px solid` `hairline-border` and `--shadow-card`.
- **Hover:** Interactive home cards shift border color to `clinical-navy`.
- **Internal Padding:** `24px` (`--space-5`) for cards; `32px` (`--space-6`) for modals.

### Inputs / Fields
- **Style:** `44px` height (`96px` min-height for vertical-resize textareas), `0 16px` padding, `4px` radius, `1px solid` `hairline-border` on `pure-surface`.
- **Focus:** Removes default outline in favor of `clinical-navy` border and `--focus-ring` (`0 0 0 3px rgba(31, 59, 87, 0.25)`).
- **Error:** `aria-invalid="true"` shifts border to `clinical-red` paired with a `0.75rem` `clinical-red` error message below.

### Navigation
- **Style:** Horizontal header bar on `pure-surface` with `1px` bottom `hairline-border`, `Space Grotesk` `700` wordmark, and `0.875rem` `500` weight nav links in `slate-muted`.
- **Active State:** Shifts text to `graphite-dark` and adds a `2px` bottom `copper-amber` indicator (`box-shadow: 0 2px 0 var(--color-accent)`).
- **Mobile:** Wraps onto a horizontally scrollable row with `44px` minimum link height below `640px`.

## Do's and Don'ts

### Do:
- **Do** reuse `src/components/ui` primitives and `src/styles/tokens.css` variables for every new screen.
- **Do** preserve `44px` minimum height on buttons, inputs, and mobile navigation links for reliable touch and desktop ergonomics.
- **Do** use `IBM Plex Mono` only for real structured data (versions, IDs, timestamps, clinical metrics).
- **Do** keep `1px` `hairline-border` strokes and `--shadow-card` subtle so data density stays calm and readable.

### Don't:
- **Don't** introduce external UI kits (MUI, Chakra, etc.) or generic consumer-fitness aesthetics (neon gradients, heavily rounded pill cards, decorative illustrations).
- **Don't** use `copper-amber` for primary CTAs or large background fills; keep it for active nav underlines, version markers, and muted badges.
- **Don't** invent new radius, shadow, or font-family values outside `tokens.css`.
