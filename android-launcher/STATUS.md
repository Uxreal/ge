# Status

Honest state of the build. A phase is only "done" when every acceptance criterion in §13 passes and
has been *measured*, not argued.

Last updated: 0.1.1 — verified on an Android 15 emulator: onboarding renders, PACKED seeds a bottom-anchored grid with icons, taps launch apps. The 0.1.0 black-screen field report was reproduced and root-caused (R8/Kotlin metadata mismatch); minification is off until its output is re-verified.

---

## Phase summary

| Phase | State | Notes |
|---|---|---|
| 1 — A real launcher | **In progress** | Module restructure underway; §14 decisions recorded |
| 2 — Capsule core | Not started | Blocked on Phase 1 acceptance |
| 3 — Stacked + peel + control panel + search | Not started | Blocked on Phase 2 |
| 4 — Theming, edge panels, focus modes, adaptive, backup | Not started | Blocked on Phase 3 |

## Phase 1 acceptance criteria — current state

| Criterion (§13) | State |
|---|---|
| Set as default and survive 20 reboots + 50 force-stops, layout intact | **Not verified** — needs a device; persistence is transactional Room with an Application-time pre-warm |
| Drag/drop hits every timing in §5 | Implemented at the exact values (280/200/520/400/600ms, lift 1.08, morph-with-velocity settle, no-haptic invalid drop) — feel unverified on hardware |
| Cold start → first drawn frame < 350ms, proven by Macrobenchmark | **Not measured** — `:benchmark:StartupBenchmark` exists and compiles; needs a device |
| Zero main-thread icon decodes, asserted in debug | **Done** — `IconCache` throws on any main-thread decode in debuggable builds |
| Grid + both home models (`PACKED`, `FREEFORM`) | **Done in code** — bottom-gravity flow order, PACKED reflow around pinned widgets, FREEFORM never moves placed items; 21 engine tests |
| Drawer, folders, widget hosting | **Done in code** — drawer with A–Z rail/suggestions/type-to-launch; folders with dwell-create, rename, dissolve; real `AppWidgetHost` with bind/configure/resize |
| Wallpaper offsets | **Done in code** — `setWallpaperOffsets` from the pager, user parallax multiplier 0–1.5 |
| Settings skeleton | **Done in code** — Compose (not PreferenceScreen), collapsing header via cross-fade, model/columns/theme/motion/haptics/shape |
| Room persistence | **Done in code** — transactional `replaceAll`, conflated writes, destructive-migration fallback so a corrupt DB can never crash-loop the home screen |
| §3 design tokens as the single source of truth | **Done** — implemented in `:core:design`, 21 unit tests pass |

## What exists right now

**Build (verified).** Nine Gradle projects configure and `:core:design` compiles: `:app`,
`:core:design`, `:core:data`, `:feature:{home,drawer,widgets,settings}`, `:benchmark`, plus the
`build-logic` included build holding four convention plugins. The full pinned set resolves — Hilt,
Room, KSP, Proto DataStore with a real protoc binary, Macrobenchmark. Module boundaries are enforced
by dependency declarations, and `:core:data` deliberately does not depend on `:core:design`.

**`:core:design` — §3 tokens, complete and tested (21 unit tests, 0 failures).**

* `Superellipse` — `|x/a|^n + |y/b|^n = 1` with `n` exposed as the smoothness token (2.0–6.0,
  default 4.6), per-corner radii, path cached per size, and a `CornerBasedShape` so Material
  components inherit the curvature.
* `MotionTokens` — the six named tokens at exactly the spec's stiffness/damping, with `page` and
  `micro` verified non-overshooting, reduce-motion collapsing every spring to the 120ms cross-fade,
  and the 0.5×–1.5× speed dial clamped.
* `Haptics` — the six-verb vocabulary (`tick`/`snap`/`lift`/`state`/`edge`/`commit`) on one class,
  amplitude-scaled where the actuator allows it, with a global intensity including off.
* `ContrastScrim` — the §3 contrast floor, solved per region rather than a fixed tint, and verified
  to hold at **every** wallpaper luminance from 0 to 1.
* `LumenTypography` — the §3 scale verbatim, mapped onto Material's scale too.
* `Depth` — one shadow token, the 1dp inner highlight and the 0.5dp hairline. No elevation stack.
* `GridGeometry` — §3's derivation, tested against the formulas including both icon-size clamps and
  the five-row floor. Default gravity is `BOTTOM` (§1).
* `FrostedSurface` — the frosted material: backdrop capture, AGSL lens on API 33+, `RenderEffect`
  blur on 31–32, translucency on 30, per-`SurfaceRole` tokens, and redraw tickets so a still screen
  costs nothing.

**Everything compiles and 42 unit tests pass** (21 design-token, 21 layout-engine). `:core:data`
now carries the `LauncherApps` index (work profiles, incremental package reloads, category
heuristics), the two-tier icon cache with the debug main-thread assertion, Room layout persistence,
Proto DataStore prefs and usage stats, wallpaper offsets + luminance sampling for the contrast
floor, and RoleManager default-home plumbing. `:feature:home` has the full §5 interaction set;
`:feature:drawer`, `:feature:widgets`, `:feature:settings` and `:app` are wired end-to-end,
including the widget bind/configure Activity flows and the §5 home-press ladder.

## Known gaps and honest omissions

* **Optical sizing (§3) is unmet.** D4 defers the typeface to the platform variable sans, which has a
  weight axis but no `opsz` axis. Revisit before Phase 4.
* **Nothing is measured yet.** Every §11 budget is currently unproven. No number will appear in this
  file without the Macrobenchmark output behind it.
* **No screenshot matrix yet** (§12). Required for Phase 4 sign-off.
* **Baseline Profiles are not wired yet.** The `:benchmark` module exists for Macrobenchmark, but the
  `androidx.baselineprofile` producer/consumer wiring needs a device to generate against, so it is
  deferred rather than configured to fail.
* **Emulator-verified, not yet hardware-verified.** An Android 15 emulator confirms boot,
  onboarding, seeding, icon rendering and app launches. Drag feel, haptics, widget hosting against
  real providers, Samsung's wallpaper restrictions, and every §11 number still need a physical
  device.
* **Crash visibility now exists**: an uncaught-exception handler writes the trace to a file, and
  the next launch shows a "Lumen crashed last time" card with a Share action — field reports can
  carry stack traces from here on.
* **Phase 1 partials, stated plainly:** no uninstall tombstones, no 20-step undo stack, no
  multi-select drag, no pinch-in wiggle entry, shortcuts (`ShortcutItem`) render defensively but
  nothing creates them yet, FREEFORM hover-to-swap resolves on drop rather than live-swapping at
  200ms, and the second home-press does not yet open search (search is Phase 3).
* **Minification is disabled.** The 0.1.0 minified build painted a black screen (AGP 8.9.2's R8
  against Kotlin 2.2 metadata); AGP is now 8.13 and the shipped build is unminified. Re-enable only
  with an emulator/device pass over the minified output.
* The pre-spec scaffold contained two §1.1 anti-defaults (page overshoot, blur everywhere). Both are
  removed as part of the restructure — see D10.
