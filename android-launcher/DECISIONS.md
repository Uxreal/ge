# Decisions

Every non-obvious choice, the alternative rejected, and why. Newest last.

---

## D1 — Capsule scope: both surfaces, in-window default, overlay opt-in

**Chosen:** `CapsuleSurface` is an interface with two implementations. `InWindowSurface` is the
default and the only one enabled on first run; `OverlaySurface` (`SYSTEM_ALERT_WINDOW`) is behind a
user-facing feature flag alongside the other §7 modular surfaces.

**Rejected:** in-window only (the Capsule stops existing the moment you leave home, which undercuts
"the OS's only handle"), and overlay only (forces a permission prompt and a persistent foreground
service on every user before they have any reason to trust the app).

**Why:** the thesis needs the overlay to be *possible*, but a launcher has to be usable and honest
the second it is set as default. Defaulting to in-window means zero permissions and no service for
the common case; the overlay becomes a deliberate upgrade. The cost is real and accepted: both paths
carry the full §12 permission matrix, and `OverlaySurface` must yield on fullscreen video, games and
secure windows.

## D2 — Distribution: sideload only

**Chosen:** APK releases, no Play Store listing.

**Rejected:** Play Store (and the dual-flavour build).

**Why:** the feature set argues for it. `SYSTEM_ALERT_WINDOW` plus a persistent service, device-admin
force-lock for a sleep gesture, `QUERY_ALL_PACKAGES`, and usage-stats access are each a policy
conversation on Play and together they are a rejection risk. Sideloading removes the ceiling
entirely, so no feature gets designed around review. Consequence recorded honestly: there is no
auto-update path, so the release notes and an in-app version check against a static file are the
only update mechanism, and the app still makes no network calls of its own (§0.5).

## D3 — Product name: Lumen, package `dev.lumen.launcher`

**Chosen:** Lumen. The public Capsule intent namespace is therefore
`dev.lumen.launcher.capsule.PUSH` / `.CLEAR`.

**Rejected:** Keystone, Perch, Halo.

**Why:** the pre-spec scaffold on this branch already declares `dev.lumen.launcher`, so this is the
only option with zero rename cost across every file, the manifest, the Room database name and the
documented intent API. Naming is reversible later; a package rename after Phase 1 is not free.

## D4 — Typeface: platform variable sans behind one token, revisit before Phase 4

**Chosen:** `FontFamily.Default` (Roboto Flex or the OEM variable sans) referenced through a single
`LumenTypography` token set, so swapping in a bundled font is a one-file change.

**Rejected:** bundling Inter (the only common OFL sans with a real `opsz` axis), Figtree, Manrope.

**Why:** the type *scale* in §3 is what carries the design; the specific face can land later without
reworking layout. Deferring keeps the APK free of an 800 KB font while the layout engine is still
moving. Consequence: §3's "optical sizing" is unmet until a face with an `opsz` axis is chosen — this
is a known open item on `STATUS.md`, not a silent omission.

## D5 — Phase 1 module set; `:feature:capsule` and `:feature:controls` deferred to their phases

**Chosen:** create `:app`, `:core:design`, `:core:data`, `:feature:home`, `:feature:drawer`,
`:feature:widgets`, `:feature:settings`, and `:benchmark` now. `:feature:capsule` arrives in Phase 2
and `:feature:controls` in Phase 3.

**Rejected:** scaffolding all nine modules up front with empty source sets.

**Why:** §0 forbids starting a phase before the previous one passes, and an empty module invites
exactly the stub-presented-as-progress that §0.2 rules out. The boundary rule that matters
(`:feature:capsule` must not depend on `:feature:home`) is enforced when the module exists. The home
layout still reserves the top region the Capsule will occupy, so nothing has to move later.

## D6 — Build logic in `buildSrc` convention plugins

**Chosen:** three convention plugins (`lumen.android.library`, `lumen.android.library.compose`,
`lumen.android.application`) plus a Hilt convention, applied by every module.

**Rejected:** duplicating ~40 lines of Android/Kotlin/Compose config across eight module build files.

**Why:** eight modules configured by copy-paste drift within a week, and §3 hinges on there being one
source of truth. Convention plugins make compile SDK, Java target, Compose enablement and the Hilt
wiring literally single-definition. Cost: `buildSrc` invalidates the whole build when edited, which
is acceptable because it changes rarely.

## D7 — Annotation processors are now in (reverses the pre-spec scaffold)

**Chosen:** KSP, for Room and Hilt. Proto DataStore brings the protobuf Gradle plugin and a protoc
binary.

**Rejected:** keeping the pre-spec scaffold's manual DI container and kotlinx.serialization-in-a-typed
DataStore, which needed no processors at all.

**Why:** §2 pins Room, DataStore(Proto) and Hilt, and §0.4 requires asking before deviating from the
pinned list — so the pinned list wins over the earlier preference. The earlier reasoning (fast builds,
an obvious graph) is still true and is now paid for as build time rather than argued about.

## D8 — Salvaged from the pre-spec scaffold, and what was thrown away

**Kept and ported:** the superellipse icon path (§3 asks for exactly `|x/a|^n + |y/b|^n = 1` with `n`
exposed; the scaffold already generated per-corner superellipse paths and shared the exponent with
its shader), the AGSL frosted-surface material, the spring/haptic vocabulary, the pure grid algebra
and its unit tests, the `LauncherApps` indexing and icon pipeline, and the serialization round-trip
tests.

**Thrown away:** the in-app procedural wallpaper layer (GPU mesh/aurora). It existed so a glass panel
could refract the wallpaper, which §5 replaces with `setWallpaperOffsets` parallax and `onZoomChanged`
against the real system wallpaper.

**Repurposed:** the wallpaper bitmap read now feeds the §3 contrast floor — a scrim computed from the
luminance of the wallpaper region behind each text run, rather than a fixed black tint. That is a
requirement the scaffold happened to have already solved the hard part of.

## D9 — "Liquid Glass" naming removed; the material is a frosted approximation

**Chosen:** the identity feature is **the Capsule**. The material is `FrostedSurface`, described as an
approximation of frosted glass. Per §3 its depth model is layered translucency, a 1dp inner top
highlight, a 0.5dp hairline border and exactly one shadow token.

**Rejected:** the pre-spec scaffold's framing, which named the feature after Apple's material in code
(`LiquidGlass.kt`, `LIQUID_GLASS`), in the README, and in user-facing copy.

**Why:** §1.2. The names are Apple's, and shipping them in code and store copy is both a legal risk
and a tell that the product is a clone rather than its own thing.

## D10 — Two anti-defaults were already in the scaffold and are removed

**Removed:** overshoot on horizontal paging (the scaffold's `LIQUID_SLIDE` page transition sprang past
the target), and blur applied to nearly every surface.

**Why:** both are named in §1.1. Paging now uses the `page` token at 700/1.0 with no overshoot —
only `morph` and `enter` may overshoot. Frosted translucency is now reserved for surfaces that need
to establish layering over content (sheets, the Capsule, the control panel), so hierarchy survives.

## D11 — minSdk 30

**Chosen:** minSdk 30, per §2. The scaffold targeted 26.

**Why:** it deletes a large amount of branching the launcher would otherwise carry — `RenderEffect`
blur (31) is still guarded, but predictive back, `RoleManager`, monochrome adaptive-icon layers and
the modern `AppWidgetHost` sizing APIs stop needing dual paths. Consequence: Android 10 and earlier
are unsupported, which is acceptable for a sideloaded launcher aimed at current hardware.

## D12 — PACKED is "items in flow order"; positions are derived

**Chosen:** the layout engine gives every cell a *flow index* counted from the bottom-left upward
(§1's gravity as arithmetic). `PACKED` state is the ordered list of flowing items; `compact()`
re-derives every position from that order around pinned items (widgets and anything spanning),
and a drag-move is a list reorder, not a coordinate edit.

**Rejected:** storing coordinates as the source of truth and writing bespoke gap-collapse and
overflow-cascade passes against them.

**Why:** deriving positions from order makes §5's hard cases fall out for free — a removal near a
full page pulls an item back across the page boundary because the queue refills page by page, and
the live drag preview is just "run the move, render the result". The one subtlety is recorded in
the code: a reorder must feed its explicit order to placement directly, never through the sort in
`compact()`, or the old cells win. A test pins this.

## D13 — Widgets are pinned; only 1×1 items flow

**Chosen:** in `PACKED`, widgets (and any spanning item) keep their exact cells and the flow fills
around them.

**Rejected:** letting widgets participate in the flow the way iOS reflows icons around widgets on
insertion.

**Why:** flowing spans means bin-packing on every mutation, with layouts that shift in ways the
user did not ask for — the exact failure §5's `FREEFORM` rule ("never move anything the user
placed") exists to prevent. Pinning widgets keeps both models predictable and keeps `compact()`
linear. Cost: in `PACKED`, a widget drop does not shove icons aside mid-drag; icons reflow around
its landing cell instead.

## D14 — Drag implemented on Compose's detectors with an overridden long-press timeout

**Chosen:** `detectDragGesturesAfterLongPress` (immediate `detectDragGestures` in wiggle mode) with
`LocalViewConfiguration` overridden so the long-press threshold is §5's 280ms, plus a separate tap
detector per icon. Hover/dwell logic lives in effects keyed on what they watch (520ms folder dwell,
200ms reflow dwell, 400/600ms edge advance).

**Rejected:** a fully hand-rolled `awaitEachGesture` state machine.

**Why:** the platform detectors already handle slop, cancellation and consumption correctly, and
the only thing wrong with them for §5 was the timeout constant — which `ViewConfiguration` is
designed to carry. Less bespoke pointer code means fewer subtle input bugs on OEM builds.

## D15 — Per-app usage stays in Proto DataStore, not Room

**Chosen:** launch counts and recency live in a `UsageStats` proto next to the prefs store; the
drawer's Suggested row ranks by frequency decayed with a ~4-day half-life.

**Rejected:** a Room table, and seeding from `UsageStatsManager` at first run.

**Why:** it is a small map with no relational shape, and keeping it out of the layout database
keeps `grid_items` the only thing the home screen's first frame depends on. `UsageStatsManager`
seeding needs the usage-access grant, which is not worth a permission prompt in Phase 1 — recorded
as a gap, revisit with §6's Library view.

## D16 — The one shared icon-loading composable is duplicated, not shared

**Chosen:** `:feature:home` and `:feature:drawer` each carry a ~15-line `rememberAppIcon`.

**Rejected:** a `:core:ui` module, or letting one feature import the other.

**Why:** §2 forbids cross-feature imports, and a new module for fifteen lines is structure without
substance. The moment a third consumer appears (search, Phase 3), the helper graduates into a real
shared module and both copies die.
