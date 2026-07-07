# Vira — AI Content Operating System

Vira is a design-system prototype for an AI-powered short-form content factory: a single operator manages
the full pipeline from **trend discovery** through **publishing and analytics** across dozens of channels,
with a human-in-the-loop approval gate at every stage.

This build is a fully **mocked, client-only** UX prototype — no backend, no auth, no real API calls, no
paid AI providers. Every screen is real, interactive React wired to deterministic fixture data, so the whole
product experience can be reviewed and demoed end-to-end before any backend or model-integration work begins.

## Stack

- **Vite + React 19 + TypeScript** (strict mode)
- **Tailwind CSS v4** — CSS-variable-driven theme (`src/index.css`), dark (default) and light mode
- **shadcn/ui** (`src/components/ui`) + **Radix primitives**
- **react-router-dom** for routing
- **recharts** for charts, **@xyflow/react** (React Flow) for the Production pipeline canvas
- **sonner** for toast feedback, **lucide-react** for icons

## Getting started

```bash
npm install
npm run dev      # start the dev server at http://localhost:5173
npm run build    # typecheck (tsc -b) + production build
npm run lint
```

## Project structure

```
src/
  App.tsx                  # route table — one route per pipeline stage
  main.tsx                 # entry point
  index.css                # design tokens (oklch), dark/light themes, glass utilities

  components/
    app/                    # app shell: sidebar, topbar, ⌘K command palette, theme toggle
    kit/                    # shared design-system primitives — reused on every page:
                            #   PageHeader, GlassPanel, ScoreBadge/MetricPill, SourceBadge,
                            #   StageBadge, StatCard, ProgressRing, EmptyState
    ui/                     # shadcn/ui component library
    <feature>/              # page-specific components (trend-discovery, ideas, scripts,
                            #   fact-check, production, media-library, platform-optimizer,
                            #   calendar, publishing, analytics, learning, channels, settings)

  lib/
    theme.tsx               # dark/light ThemeProvider (persisted to localStorage)
    utils.ts                # cn() class-merge helper
    mock/
      types.ts              # SHARED domain types (Channel, Trend, Idea, Project, Stage, ...)
      data.ts                # SHARED fixtures: channels, trends, ideas, projects, approvals,
                             #   activity, weekPerformance — consumed across the whole app
      <feature>.ts           # page-specific fixtures (scripts, factchecks, production, media,
                             #   platform, schedule, publishing, analytics, learning,
                             #   channels-extra, settings)

  pages/                    # one component per route, wired into App.tsx
```

## Pipeline coverage

Every stage of the spec's content pipeline has a working screen:

| Stage | Route | Notes |
|---|---|---|
| Trend Discovery | `/trends` | Filterable/sortable trend board across 8 sources, virality/growth/evergreen scoring, "why it's working" analysis |
| Idea Generator | `/ideas` | Scored concepts across 25 categories, original vs. trend-inspired, mocked "generate batch" flow |
| Script Writer | `/scripts` | 15s–90s variants, hook-first emphasis, retention-technique tags, alt-hook testing |
| Fact Check | `/fact-check` | Per-claim confidence scoring, source citations, flagged/outdated detection |
| Production | `/production` | **Centerpiece** — a React Flow node graph of all 14 stages per project, live status/progress, stage detail sheets |
| Media Library | `/media` | Asset grid (image/video/voice/music/sfx) with duplicate-detection flags |
| Platform Optimizer | `/platform-optimizer` | Distinct YouTube/TikTok/Instagram metadata per project — never a copy-paste post |
| Calendar | `/calendar` | Month grid of scheduled posts, per-day detail, mock scheduling flow |
| Publishing | `/publishing` | Upload queue with retry/preview/cancel and bulk actions |
| Analytics | `/analytics` | KPIs, views/revenue trend, top-hooks ranking, posting-time heatmap, channel comparison |
| Learning Loop | `/learning` | Categorized performance insights with human-in-the-loop auto-apply, pending-adjustment approvals |
| Channels | `/channels` | Multi-channel management, brand-voice editor, connect-new-channel flow, team workspace |
| Settings | `/settings` | Account, mock credential storage, plugins, prompt templates, team |
| Dashboard | `/` | Command-center overview tying every section together |

## Design system

- Dark is default; toggle in the top bar (`⌘` no shortcut — click the sun/moon icon).
- Color, spacing, and radius are all CSS variables in `src/index.css` (oklch color space) — no hardcoded
  hex values anywhere in component code. Adding a brand or accent color means editing the tokens in one place.
- `glass` / `glass-strong` utility classes provide the translucent panel treatment used throughout.
- `⌘K` opens the command palette from anywhere; `⌘B` toggles the sidebar.

## What's mocked vs. what's real

Everything is real, interactive React/TypeScript — state updates, filters, sorts, dialogs, toasts, and the
React Flow canvas all genuinely work. What's mocked is the **data source**: every number, trend, script, and
analytics figure comes from deterministic fixtures in `src/lib/mock/`, not a live AI provider or social
platform API. Swapping in real integrations means replacing the fixture imports in each page with real data
fetching — the component layer and design system don't need to change.

## Extending this

To wire up a real backend/AI provider for a given stage, the fixture module for that page
(`src/lib/mock/<feature>.ts`) is the seam: replace its exported constants/functions with real data-fetching
(e.g. React Query) while keeping the same shapes, and the page component needs no changes.
