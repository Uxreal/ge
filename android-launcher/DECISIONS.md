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

## D17 — 0.1.0 black screen: root cause and the response

**What happened:** the first field install ("it does not load… nothing after") was reproduced on an
Android 15 emulator: the 0.1.0 APK ran, held window focus, and painted nothing — no crash, no
trace. The same code built without minification renders and works end to end (onboarding → model
choice → seeded bottom-anchored grid → app launch, all screenshot-verified).

**Root cause:** AGP 8.9.2 bundles an R8 older than Kotlin 2.2, which parsed our 2.2.20 metadata
with errors (it warned at build time) and silently produced broken output.

**Chosen:** upgrade AGP to 8.13.0; ship the release build unminified until the minified output is
itself verified end-to-end; install an uncaught-exception handler whose trace surfaces in-app with
a share action on the next launch, so no future field report is ever trace-less.

**Rejected:** keeping minification on the new R8 without re-verification (burned once), and
treating the report as un-reproducible without device logs (the emulator reproduced it exactly).

## D18 — Phase 2 started before Phase 1's acceptance was measured

**Chosen:** build `:feature:capsule` now, with Phase 1's "cold start < 350ms, proven by
Macrobenchmark" and "20 reboots + 50 force-stops" criteria still unmeasured.

**Rejected:** holding the Capsule until a physical device is available to measure against.

**Why:** the user directed it explicitly ("make the entire launcher very custom and unique"), and
the Capsule *is* §1's identity thesis — a launcher without it is a grid. §0 requires phase order,
so this is a deviation, and the honest response is to record it rather than to quietly redefine
Phase 1 as done. `STATUS.md` still shows those two criteria as unverified, and they stay unverified
until there is Macrobenchmark output behind them.

## D19 — The ambient clock is a permanent Capsule source

**Chosen:** a clock/date card at priority 100 that never withdraws, so the Capsule is visible at
rest rather than only during events.

**Rejected:** true `DORMANT` at rest, which is what §4's table literally describes ("Not drawn. No
active sources").

**Why:** §1 calls the Capsule "the OS's only handle" and the single anchor the whole layout is
built around — the grid reserves 56dp for it and gravity pushes icons away from it. A handle that
is absent 95% of the day is not a handle, and the reserved strip would read as a mistake. §4's
`DORMANT` state is still implemented and still reachable: turning the Capsule off in Settings, or
any future build where the clock source is disabled, lands there. This is a default, not a removal.

## D20 — Deck state is not persisted across process death

**Chosen:** the deck rebuilds from live sources on cold start. Third-party pushes do not survive a
launcher restart.

**Rejected:** §4's "persist deck state across launcher process death; restore without animation".

**Why:** a card's `tapIntent` and actions are `PendingIntent`s, which cannot be serialised. A
restored card would render but do nothing when tapped — worse than absent, because it looks alive.
The built-in sources (clock, battery, alarm) re-publish within a second of process start, so what
persistence would actually buy is a few seconds of stale third-party text with dead buttons.
Revisit if a push API gains a re-delivery handshake. Listed as a Phase 2 gap in `STATUS.md`.

## D21 — Push identity comes from `PendingIntent.getCreatorPackage()`, not from an extra

**Chosen:** attribute a pushed card to the creator package of its `tapIntent` (or first action
intent), falling back to a self-declared `pkg` extra and marking the card unverified.

**Rejected:** trusting the `pkg` extra outright, or requiring a signature permission to push.

**Why:** a broadcast receiver has no trustworthy caller identity, so a block list keyed on a
self-declared string can be defeated by changing the string. `getCreatorPackage()` is filled in by
the system and cannot be forged, and any push worth blocking almost certainly carries a tap intent.
§4.1 says explicitly that no permission is required, so a permission wall was out; the defences are
validation, the 0..500 priority clamp, four pushes/second per package, and a visible block list.

## D22 — Drag-down-to-peel is not wired to a half-implementation

**Chosen:** in the Capsule, drag down expands. The §4 peel gesture is left for Phase 3, which is
where §13 puts the peeled home card anyway.

**Rejected:** starting the peel animation and dropping the card, or peeling into a floating overlay
with nowhere to live.

**Why:** §0.2 — no stubs presented as features. A gesture that begins something it cannot finish is
worse than one that is not there.

## D23 — The page transition is depth, not bounce

**Chosen:** pages recede (0.90×), dim (0.65), parallax their contents at 80% of the swipe, and
hinge 6° on their trailing edge.

**Rejected:** a plain translate (indistinguishable from every other launcher), and spring overshoot
on the page settle.

**Why:** §1.1 names bouncy horizontal paging as an anti-default and §3 pins `page` to a critically
damped 700/1.0, so the character cannot come from the spring. Depth is the axis left open. The
whole transform is skipped under reduce-motion, which is the setting's actual promise.

## D24 — Backdrop capture is reference-counted

**Chosen:** `BackdropCapture` counts mounted frosted surfaces and records the home surface into its
`GraphicsLayer` only while at least one exists.

**Rejected:** recording unconditionally (what the first implementation did).

**Why:** with no frosted surface on screen, every home-screen frame was being recorded into a
full-screen layer and then blitted back — the cost of a blur nobody was drawing. The redraw
*tickets* are a separate axis and remain: consumers say "someone is sampling us", tickets say "the
pixels are moving". Both must be true before the frame clock is read.

## D25 — The Capsule docks ON the camera and is opaque, not frosted

**Chosen:** the pill embraces the punch-hole cutout — sized from the hole, positioned so the hole
sits at its centre, content flanking the camera symmetrically — and is painted near-black opaque.
The window sets `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` to be allowed there.

**Rejected:** the 0.3.0 design (frosted translucent pill floating below the status bar), which the
first hardware report called what it was.

**Why:** §4 says GLANCE "grows around the cutout", and the only way a pill absorbs a camera is by
matching it: near-black on near-black, so the hole reads as part of the surface. A frosted lens
cannot do that — and at 34dp tall, blur + refraction + highlight + hairline read as a washed-out
blob, not a material. Opacity is also the performance fix: the frosted pill permanently mounted a
backdrop consumer, so the entire home surface was re-recorded whenever it redrew. The Capsule is
the one surface that is *always* on screen; it must be the one surface that costs nothing.

The docking policy lives in `CapsuleGeometry`, pure and unit-tested, because the first emulator
run proved the obvious heuristic wrong: a wide emulated notch passed a centred-only check and was
"embraced" into a full-width banner. Embrace requires a punch-hole — under a fifth of the screen
wide, under 44dp tall, centred. Notches, corner holes and cutout-less screens get a plain pill
under the status bar.

## D26 — Expansion is one coordinated move with a persistent header

**Chosen:** the header row (collapsed text left of the camera, glyph right) is identical in both
states; expanding widens the container and slides the card out from underneath on the same `morph`
spring, with a 4% press-squish on `micro` for tap feedback. Expanded width is content-sized
(≤356dp), not full-width. The deck's dot rail is gone; the shoulder slivers carry depth alone.

**Rejected:** 0.3.0's expansion — `animateContentSize`, a corner animation and an
`AnimatedContent` cross-fade of the entire contents running as three unsynchronised animations
into a full-width card.

**Why:** the field report said "feels slow", and the spec's `morph` spring was not the cause — the
choreography was. Three animations with different curves read as mush; one spring with a fixed
header reads as a single object changing shape. Less travel is faster at equal stiffness. The page
transition was tuned on the same report (dim 0.35→0.18, recede 0.10→0.05, parallax 0.20→0.12,
turn 6°→3.5°): heavy dim mid-swipe read as drag, not depth.

## D27 — The push API is served by a context-registered receiver; the manifest one is a fallback

**Chosen:** `CapsuleController.start()` registers an exported runtime receiver for PUSH/CLEAR;
the manifest `CapsulePushReceiver` stays for explicit-component broadcasts.

**Rejected:** manifest receiver only — which is what 0.3.0 shipped, and it could never have
worked: since Android 8 a manifest receiver does not receive implicit broadcasts, and
`am broadcast -a dev.lumen.launcher.capsule.PUSH` is exactly that. Found live on the emulator.

**Why:** the launcher is the HOME app, so its process is effectively always alive to hold a
runtime receiver; the manifest receiver still catches explicitly-addressed pushes if the process
is dead. §4.1's defences (validation, clamp, rate limit, block list) are unchanged — both paths
funnel through one parser.

## D28 — Media in the Capsule rides a notification listener the user enables by hand

**Chosen:** §4's media source (priority 700) reads `MediaSessionManager` through a
`NotificationListenerService` that exists only to be the consent token — it reads no
notifications. Nothing appears until the user turns the listener on from Settings → Capsule,
which deep-links to the system screen; the launcher re-checks on every resume and when the
listener connects. Transport controls (play/pause/next) act on the `MediaController` directly,
so `CapsuleAction` gained a `run` lambda beside the `PendingIntent` a push supplies. The media
card is not dismissible: the next playback callback would re-push it instantly, and a dismissal
that comes straight back reads as broken.

**Rejected:** shipping without media (it is the single most-used real-world card), and asking for
notification access during onboarding (§10: absence of a permission is a normal state, not a
nag — the launcher is fully functional without it).

## D29 — Minification is back on, with the evidence STATUS demanded

**Chosen:** `isMinifyEnabled = true` + resource shrinking for release. 46 MB → 2.9 MB.

**The gate was** "watch the minified output work on a screen", set after 0.1.0's R8/Kotlin-2.2
dead screen. Met on an Android 15 emulator with AGP 8.13's R8, on the exact artifact shipped:
cold install → onboarding → PACKED chosen → grid seeded with real icons bottom-anchored → Capsule
clock pill → an `am broadcast` push rendering rim progress → an app launched from the grid. Those
paths cover every reflective surface that R8 could plausibly break (Room, protobuf lite, Hilt,
`LauncherApps`, the broadcast parser).

**Rejected:** re-enabling on the argument that AGP 8.13 "probably fixed it". That reasoning was
free once and cost a release.

## D30 — Lumen owns the top of the screen: the system status bar is hidden on the home surface

**Chosen:** with the Capsule enabled (and a Settings switch to opt out), the launcher hides the
system status bar and draws its own clusters in the pill band — time left, battery right, in §3's
type — so the top reads as one composed row: time … camera pill … battery. A top-edge swipe still
summons the bar and the shade transiently.

**Rejected:** leaving the system bar to fight the pill (its icons flank the island in a different
visual language, which is what "all of the top icons need to be redone to fit it" was reporting),
and trying to restyle the bar (a launcher cannot).

**Why:** the field report asked for exactly this, and §1's thesis already claims the top of the
screen for the Capsule as "the OS's only handle". It also fixes an interaction fault by
construction: a visible status bar window consumes every touch in its band, which made the
camera-docked pill look tappable while actually being inert — with the bar hidden the band belongs
to the launcher and the pill's tap, long-press and swipes work. Signal/Wi-Fi indicators are
deliberately not replicated (they would need connectivity callbacks and privileged state the strip
does not want); the transient bar is one swipe away.

## D31 — Pitch black, and a bare ring at rest

**Chosen:** the pill fill is pure opaque black (user-directed: "needs to be darker, pitch black"),
with no edge stroke — the physical camera it swallows is pitch black, and any lighter fill or
hairline reads as a sticker next to a hole rather than one object. At rest — ambient clock as the
only card, docked on a real camera, with the D30 strip already showing the time — the pill
collapses to a content-free ring hugging the lens; any real card grows it back. In fallback mode
(no camera) the clock text stays, since there is no lens to ring.

**Rejected:** the near-black translucent fill with a hairline (previous build), and keeping the
time inside the pill at rest, which duplicated the strip's clock a centimetre away.

**Noted:** §1.1 lists "a flat black rounded rect island" as an anti-default *as sole identity*.
The identity here is carried by everything the flat-black clone lacks — the superellipse
silhouette, the rim-as-progress, the shoulder deck, the ring-at-rest behaviour — and the colour is
what the physical trick requires.

## D32 — The drawer is an App Library, not a list

**Chosen:** the FREEFORM drawer's default view is category shelves in the App Library manner
(user-directed: "give it categories, take inspiration from apple"): two columns of tiles, each
tile launching its first three apps directly and opening the full shelf from a mini-cluster of
tiny icons in the fourth slot. Categories come from the manifest-declared category when a
developer set one and the existing package-name heuristics otherwise; single-app categories fold
into Other, and System/Other always sort last. The alphabetical grid with the A–Z rail survives
behind a one-tap "A to Z" chip, and typing in search still overrides everything with the flat
filtered list.

**Rejected:** replacing the alphabetical grid outright (the rail is the fastest path to a known
app and §6 asked for it), and copying Apple's layout verbatim — the tiles here are Lumen's
superellipse surfaces on the frosted sheet, not iOS folder chrome.

## D33 — The pill's curvature is not the launcher's curvature

**Chosen:** everything the Capsule draws uses its own superellipse exponent n=2.15 — a hair
softer than a true stadium — instead of the launcher-wide default n=4.6. Field report: "the
island looks like a square, should be rounded."

**Why:** n=4.6 is the token that makes icons and sheets read considered rather than default, but
at pill scale, with the corner radius equal to half the height, a 4.6 exponent flattens the arc
into visibly squared ends — around a *circular* camera lens, unmistakably wrong. The smoothness
token exists to be tuned per surface; this is the first surface whose job requires the round end
of its range.
