# Lumen

An Android home-screen replacement with one identity commitment (§1 of the build spec): **a single
anchor at the top of the screen is the OS's only handle** — the Capsule, arriving in Phase 2 — and a
**bottom-anchored grid**: page 1 fills from the bottom up, so icons live where your thumb is and
empty space collects at the top, where the Capsule and widgets belong.

Kotlin, Jetpack Compose, Room, Proto DataStore, Hilt. minSdk 30. **No network access, ever** — no
analytics, no crash SDKs, no telemetry. Sideload distribution by design (`DECISIONS.md` D2).

Current state: **Phase 1** (a real launcher). `STATUS.md` is the honest ledger of what is done,
what is coded but unverified on hardware, and what is deliberately absent.

## What Phase 1 gives you

* **Two home models, chosen at first run, switchable later without data loss.**
  `PACKED` — every app on a page, iOS-like, gaps close themselves. `FREEFORM` — pages hold only
  what you place, holes are preserved exactly, and the app drawer exists (swipe up from the bottom
  edge).
* **The §5 drag system at its specified timings.** 280ms lift with a haptic and a 1.08× scale, 1:1
  `follow` under the finger, live reflow preview in PACKED after a 200ms hover, folder creation
  after a 520ms dwell with the target dilating, page-edge auto-advance at 400ms then every 600ms,
  and a release that carries the finger's velocity into the settle spring. An invalid drop springs
  home with *no* haptic — the absence is the signal.
* **Wiggle mode**: long-press empty wallpaper; ±1.5° at ~0.9Hz with per-icon phase offsets; X
  badges hit at 48dp; Widgets / Settings / Done chips.
* **Folders**: dwell-create, full-screen frosted sheet over the blurred home surface, inline
  rename, auto-dissolve at one item.
* **Real widgets**: `AppWidgetHost` with the full bind → configure Activity flow, previews in the
  picker, cell-snapped resize handles in wiggle mode, and a quiet placeholder if a provider dies.
* **A drawer** (FREEFORM): alphabetical grid, A–Z fast-scroll rail, usage-ranked Suggested row,
  and type-to-launch — typing filters, Enter launches the top hit.
* **Wallpaper parallax** via `setWallpaperOffsets`, with a 0–1.5× multiplier in Settings.
* **The §3 token system** as the single source of truth: superellipse shapes (`n` exposed as a
  smoothness slider, 2.0–6.0), six named motion springs with reduce-motion collapsing everything to
  a 120ms cross-fade, a six-verb haptic vocabulary, and a contrast floor computed from the
  wallpaper region behind each label — flipping label polarity on bright wallpapers instead of
  stacking scrim.
* **Accessibility**: every icon is one TalkBack node with custom actions (move between pages,
  app info, uninstall, remove) so rearranging never requires a drag.
* **Survives process death**: layout pre-warms from Room at `Application.onCreate`; a corrupt
  database yields an empty rebuildable home screen, never a crash loop.

## Install on a phone (sideload)

Grab `releases/lumen-<version>.apk` from this repo (or build it: see below), then on the phone:

1. Copy the APK over (USB, Quick Share, cloud link — anything).
2. Open it from **My Files** / **Files**. Android will ask to allow installs from that app —
   allow it (Settings → *Install unknown apps*), then install.
3. Launch **Lumen**, pick a home model, and accept the **default home** prompt.
   If you skip it: Settings → Apps → **Choose default apps** → **Home app** → Lumen.

### Galaxy Z Flip / Fold notes

* One UI buries the default-home picker at **Settings → Apps → Choose default apps → Home app**.
* The **cover screen** cannot run a third-party launcher without Samsung's Good Lock **MultiStar**
  module; Lumen owns the main display only. A dedicated cover-screen layout is Phase 4 (§9).
* Samsung restricts wallpaper reads for non-default launchers: the per-region label scrim
  activates after Lumen becomes the default home and can read the wallpaper. Until then labels use
  a conservative fixed shadow.

To go back to One UI Home: Settings → Apps → Choose default apps → Home app.

## Build from source

Requires JDK 17+ and the Android SDK (compileSdk 36).

```bash
cd android-launcher
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:assembleRelease        # R8-minified, debug-signed (sideload build)
./gradlew test                        # unit tests (design tokens + layout engine)
./gradlew :benchmark:connectedBenchmarkAndroidTest   # §11 budgets — physical device required
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Architecture

```
build-logic/               convention plugins: one definition of SDK/Java/Compose/Hilt for all modules
core/design/               §3 tokens: Superellipse, MotionTokens, Haptics, ContrastScrim,
                           LumenTypography, Depth (one shadow, 1dp highlight, 0.5dp hairline),
                           GridGeometry (bottom gravity), FrostedSurface (SDF lens, API-tiered)
core/data/                 Room grid persistence, Proto DataStore prefs + usage, LauncherApps index,
                           two-tier IconCache (main-thread decode asserted), wallpaper offsets +
                           luminance map, RoleManager plumbing.  No dependency on core/design.
feature/home/              LayoutEngine (pure; flow order, PACKED reflow, FREEFORM invariants),
                           HomeScreen (pager, grid, §5 drag, wiggle, folders, indicator)
feature/drawer/            drawer + fast-scroll + suggestions + type-to-launch
feature/widgets/           AppWidgetHost wrapper, picker, host frame with resize handles
feature/settings/          Compose settings skeleton with the §7 collapsing header
app/                       Hilt graph, HOME activity, widget bind/configure flows, composition root
benchmark/                 Macrobenchmark for the §11 budgets
```

Module rules (§2): features never import each other; `:core:data` never imports `:core:design` —
icons are cached **unmasked** and masked at draw time, which is why a shape change is instant and
costs no cache invalidation.

## The paper trail

* `DECISIONS.md` — every non-obvious choice, the alternative rejected, and why.
* `STATUS.md` — the phase ledger. Nothing is claimed done without the §13 criterion behind it, and
  no performance number appears without Macrobenchmark output.
