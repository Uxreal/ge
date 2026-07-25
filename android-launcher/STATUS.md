# Status

Honest state of the build. A phase is only "done" when every acceptance criterion in §13 passes and
has been *measured*, not argued.

Last updated: module restructure complete; §3 tokens implemented and unit-tested (Phase 1 in progress).

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
| Set as default and survive 20 reboots + 50 force-stops, layout intact | Not verified |
| Drag/drop hits every timing in §5 | Not implemented |
| Cold start → first drawn frame < 350ms, proven by Macrobenchmark | Not measured |
| Zero main-thread icon decodes, asserted in debug | Not implemented |
| Grid + both home models (`PACKED`, `FREEFORM`) | Not implemented |
| Drawer, folders, widget hosting | Not implemented |
| Wallpaper offsets | Not implemented |
| Settings skeleton | Not implemented |
| Room persistence | Not implemented |
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

**Ported, not yet reworked.** `:core:data` holds the salvaged `LauncherApps` indexing and usage
tracking; `:feature:home` holds the pure layout algebra and its 30 tests. Both still carry pre-spec
package names and APIs and do not compile yet — that is the next task, alongside the Room schema,
the Proto preferences, and the bottom-gravity grid.

## Known gaps and honest omissions

* **Optical sizing (§3) is unmet.** D4 defers the typeface to the platform variable sans, which has a
  weight axis but no `opsz` axis. Revisit before Phase 4.
* **Nothing is measured yet.** Every §11 budget is currently unproven. No number will appear in this
  file without the Macrobenchmark output behind it.
* **No screenshot matrix yet** (§12). Required for Phase 4 sign-off.
* **Baseline Profiles are not wired yet.** The `:benchmark` module exists for Macrobenchmark, but the
  `androidx.baselineprofile` producer/consumer wiring needs a device to generate against, so it is
  deferred rather than configured to fail.
* **`:core:data` and `:feature:home` do not compile yet** — they hold ported pre-spec code mid-rework.
  Nothing in them is claimed as working.
* The pre-spec scaffold contained two §1.1 anti-defaults (page overshoot, blur everywhere). Both are
  removed as part of the restructure — see D10.
