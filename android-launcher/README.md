# Lumen

An Android home-screen replacement with one identity commitment (§1 of the build spec): **a single
anchor at the top of the screen is the OS's only handle** — the **Capsule** — and a
**bottom-anchored grid**: page 1 fills from the bottom up, so icons live where your thumb is and
empty space collects at the top, where the Capsule and widgets belong.

Kotlin, Jetpack Compose, Room, Proto DataStore, Hilt. minSdk 30. **No network access, ever** — no
analytics, no crash SDKs, no telemetry. Sideload distribution by design (`DECISIONS.md` D2).

Current state: **Phase 1 complete in code, Phase 2 (the Capsule) underway.** `STATUS.md` is the
honest ledger of what is done, what is coded but unverified on hardware, and what is deliberately
absent.

## The Capsule

A pill docked under the status bar, and the only chrome Lumen puts on your home screen. It has four
states — nothing at all, a compact glance, an expanded card, and a deck when more than one thing is
happening — and it decides what to show with the arbitration rules in §4 of the spec: an 800ms dwell
before a source may claim the front (so a notification that immediately rewrites itself cannot make
the pill flicker), a 400ms coalesce window, dedupe by package and activity kind, and an 8-second
manual hold when you swipe the deck yourself. Those rules are unit-tested against a virtual clock.

Three things make it look like Lumen's rather than like everyone else's take on a pill:

* **Its outline is the progress indicator.** Nothing is added inside the pill — a segment of the
  superellipse silhouette itself is stroked, clockwise from top-centre for determinate progress and
  as a short travelling comet for indeterminate.
* **The deck is drawn as shoulders**, not as a stack of cards: cards behind the front one appear as
  narrower slivers emerging from underneath it.
* **The container morphs; the type cross-fades.** Width, height and corner radius animate on the
  `morph` spring while the content swaps on the 120ms cross-fade — §1.1 forbids scaling text during
  a container morph, and this is what honouring that looks like.

Interactions: tap expands, long-press expands with actions, swipe left/right shuffles the deck,
flick up dismisses a dismissible card, drag down expands. Every transition fires the `state` haptic.

**Built-in sources need no permissions and make no network calls:** the ambient clock and date, the
battery (a card on plug/unplug, a persistent one below 15%), and the next alarm from
`AlarmManager.getNextAlarmClock()` — priority 850 inside the final minute, 400 before that. Media
playback is deliberately absent: `MediaSessionManager` needs notification-listener consent, which is
a decision for you to make rather than a default to ship.

### Pushing your own cards

Any app, Tasker task or shell script can push a card. No permission is required.

```bash
adb shell am broadcast \
  -a dev.lumen.launcher.capsule.PUSH \
  --es id "build.status" \
  --es pkg "com.my.tool" \
  --es collapsedText "CI 62%" \
  --es title "Deploying" \
  --es subtitle "3 of 5 services live" \
  --ef progress 0.62 \
  --ei priority 400

# and to take it away again
adb shell am broadcast -a dev.lumen.launcher.capsule.CLEAR --es id "build.status"
```

| Extra | Type | Notes |
|---|---|---|
| `id` | String | Required, unique per source and key. What `CLEAR` matches on. |
| `priority` | Int | Clamped to 0..500 — a third party can never outrank a call. |
| `collapsedText` | String | ≤12 characters, shown in the compact pill. |
| `title` / `subtitle` | String | Shown when expanded. |
| `progress` | Float | 0.0..1.0, or -1 for indeterminate. Drives the rim. |
| `iconUri` | String | `content://` or `android.resource://` only. |
| `accentColor` | Int | Blended toward the theme, never used raw. |
| `tapIntent` | PendingIntent | Also how Lumen proves who you are — see below. |
| `actionLabel0..2` | String | With matching `actionIntent0..2` PendingIntents. |
| `expiresAt` | Long | Epoch ms. Past, absent or beyond 24h means no expiry. |
| `dismissible` | Boolean | Default true. |

Malformed pushes are dropped silently and logged at debug. Pushes are rate-limited to four per
second per package. A broadcast carries no trustworthy caller identity, so Lumen attributes a card
to `PendingIntent.getCreatorPackage()` when one is supplied — the system fills that in and it cannot
be forged — and falls back to a self-declared `pkg` extra otherwise. Every package that has pushed
appears in **Settings → Capsule**, where you can switch it off.

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

Grab `releases/lumen-0.3.0-capsule.apk` from this repo (or build it: see below), then on the
phone:

1. Copy the APK over (USB, Quick Share, cloud link — anything).
2. Open it from **My Files** / **Files**. Android will ask to allow installs from that app —
   allow it (Settings → *Install unknown apps*), then install.
3. Launch **Lumen**, pick a home model, and accept the **default home** prompt.
   If you skip it: Settings → Apps → **Choose default apps** → **Home app** → Lumen.
4. Press Home. The Capsule is the pill under the status bar showing the time — tap it to expand,
   long-press for actions. To see the deck and the rim progress, push a card at it:

   ```bash
   adb shell am broadcast -a dev.lumen.launcher.capsule.PUSH \
     --es id demo --es pkg com.demo --es collapsedText "62%" \
     --es title "Sync" --es subtitle "Two of three folders" --ef progress 0.62
   ```

The APK is ~46 MB because minification is off — see `releases/README.md` for why that is a
deliberate choice rather than an oversight.

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
./gradlew test                        # unit tests (design tokens, layout engine, Capsule arbiter)
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
feature/capsule/           CapsuleArbiter (pure; §4's dwell/coalesce/dedupe/override rules),
                           permission-free system sources, the §4.1 push receiver, and the pill
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
