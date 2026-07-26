# Status

Honest state of the build. A phase is only "done" when every acceptance criterion in §13 passes and
has been *measured*, not argued.

Last updated: 0.3.0 — the Capsule (§4) exists: arbitration, permission-free system sources, the
public push API, and the pill itself. Phase 2 was started before Phase 1's two measured criteria
were met; that is a user-directed deviation from §0's phase order and is recorded as D18 rather
than papered over.

---

## Phase summary

| Phase | State | Notes |
|---|---|---|
| 1 — A real launcher | **Complete in code, two criteria unmeasured** | Everything below is implemented; cold-start and reboot-survival still need a device |
| 2 — Capsule core | **In progress** | Arbiter + sources + push API + UI landed; deck persistence and media source deliberately absent |
| 3 — Stacked + peel + control panel + search | Not started | `STACKED` landed early with Phase 2; peel, control panel and search are untouched |
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
| Settings skeleton | **Done in code** — Compose (not PreferenceScreen), collapsing header via cross-fade, model/columns/theme/motion/haptics/shape/Capsule |
| Room persistence | **Done in code** — transactional `replaceAll`, conflated writes, destructive-migration fallback so a corrupt DB can never crash-loop the home screen |
| §3 design tokens as the single source of truth | **Done** — implemented in `:core:design`, 21 unit tests pass |

## Phase 2 acceptance criteria — current state

| Criterion (§4 / §13) | State |
|---|---|
| Four states: `DORMANT` / `GLANCE` / `EXPANDED` / `STACKED` | **Done in code.** `DORMANT` is reachable but not the resting state — D19 keeps an ambient clock source alive at priority 100 so the Capsule is a permanent handle |
| Arbitration: 800ms front dwell, 400ms coalesce, dedupe by package + kind, 8s manual hold, priority order, expiry | **Done and tested** — 20 unit tests against a virtual clock, including the flicker case (a source re-pushing itself does not re-arm its own dwell) |
| Auto-expand once for 2.5s at priority ≥800 | **Done and tested** |
| Deck shuffle by horizontal swipe, width animates to each card | **Done in code** — `animateContentSize` on the `morph` spring; feel unverified on hardware |
| Dot rail showing depth | **Done in code** |
| Peel: drag down past 96dp into a floating home card | **Absent by choice (D22).** Drag down expands instead. The peeled home card is a Phase 3 item in §13 |
| Persist deck across process death | **Absent by choice (D20)** — `PendingIntent`s cannot be serialised, so a restored card would look alive and do nothing |
| Public intent API with validation, 4/sec/package rate limit, block-list screen | **Done and tested** — rate limiter has its own 3 tests; block list lives in Settings → Capsule and lists every package that has pushed |
| Sources: call, navigation, capture, media, transfer | **Absent.** All five need either notification-listener consent (§10) or a permission. Only the three permission-free sources ship: clock, battery, next alarm |
| Cards reorder with animation, never popping in or out | **Partial.** Entry and exit cross-fade and the container morphs, but reordering inside the deck is not yet a per-card animated transition |

## What exists right now

**Build (verified).** Ten Gradle projects configure and build: `:app`, `:core:design`, `:core:data`,
`:feature:{capsule,home,drawer,widgets,settings}`, `:benchmark`, plus the `build-logic` included
build holding four convention plugins. Module boundaries are enforced by dependency declarations:
`:feature:capsule` does not depend on `:feature:home`, and `:core:data` does not depend on
`:core:design`.

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
* `FrostedSurface` — the frosted material: reference-counted backdrop capture (D24), AGSL lens on
  API 33+, `RenderEffect` blur on 31–32, translucency on 30, per-`SurfaceRole` tokens, and redraw
  tickets so a still screen costs nothing.

**`:feature:capsule` — new.** `CapsuleArbiter` is pure and clock-injected, which is what makes §4's
timing rules testable at all; `CapsuleController` confines it to one coroutine behind a command
channel and sleeps exactly until the arbiter's next deadline rather than polling; `SystemSources`
publishes the three permission-free sources; `CapsulePushReceiver` implements §4.1 with validation,
truncation, the priority clamp and creator-package attribution; `CapsuleHost` draws the pill.

**65 unit tests pass** (21 design-token, 21 layout-engine, 20 arbiter, 3 rate-limiter).
`:core:data` carries the `LauncherApps` index (work profiles, incremental package reloads, category
heuristics), the two-tier icon cache with the debug main-thread assertion, Room layout persistence,
Proto DataStore prefs and usage stats, wallpaper offsets + luminance sampling for the contrast
floor, and RoleManager default-home plumbing.

## Known gaps and honest omissions

* **The Capsule is not yet verified on a running device.** It compiles, its logic is unit-tested,
  and it is wired into the composition root — but the emulator available here died mid-verification
  (system_server crash, then a window manager left with two windows and no SystemUI), so the pill
  has not been seen on a screen. Nothing in this file claims otherwise.
* **Optical sizing (§3) is unmet.** D4 defers the typeface to the platform variable sans, which has
  a weight axis but no `opsz` axis. Revisit before Phase 4.
* **Nothing is measured yet.** Every §11 budget is currently unproven. No number will appear in this
  file without the Macrobenchmark output behind it.
* **No screenshot matrix yet** (§12). Required for Phase 4 sign-off.
* **Baseline Profiles are not wired yet.** The `:benchmark` module exists for Macrobenchmark, but the
  `androidx.baselineprofile` producer/consumer wiring needs a device to generate against, so it is
  deferred rather than configured to fail.
* **Emulator-verified through 0.2.0, not hardware-verified.** An Android 15 emulator confirmed boot,
  onboarding, seeding, icon rendering and app launches for 0.2.0. Drag feel, haptics, the Capsule,
  widget hosting against real providers, Samsung's wallpaper restrictions, and every §11 number
  still need a physical device.
* **Crash visibility exists**: an uncaught-exception handler writes the trace to a file, and the
  next launch shows a "Lumen crashed last time" card with a Share action.
* **Phase 1 partials, stated plainly:** no uninstall tombstones, no 20-step undo stack, no
  multi-select drag, no pinch-in wiggle entry, shortcuts (`ShortcutItem`) render defensively but
  nothing creates them yet, FREEFORM hover-to-swap resolves on drop rather than live-swapping at
  200ms, and the second home-press does not yet open search (search is Phase 3).
* **Minification is disabled.** The 0.1.0 minified build painted a black screen (AGP 8.9.2's R8
  against Kotlin 2.2 metadata); AGP is now 8.13 and the shipped build is unminified. Re-enable only
  with an emulator/device pass over the minified output.
* The pre-spec scaffold contained two §1.1 anti-defaults (page overshoot, blur everywhere). Both are
  removed — see D10.
