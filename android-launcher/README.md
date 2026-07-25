# Lumen

An Android home-screen replacement built around one idea: **Google's structure, Apple's feel, and
glass as the material that binds them.** Every surface of chrome — dock, drawer, folders, menus,
settings — is a slab of real refracting glass, and nearly everything about the launcher can be
reshaped by the user.

Kotlin, Jetpack Compose, Material 3. No annotation processors, no DI framework, no network calls.

---

## The design thesis

Neither platform's home screen is better; they are good at different things.

| Borrowed from Google | Borrowed from Apple |
| --- | --- |
| Material You — the whole palette derives from the wallpaper | Spring physics: nothing eases, everything settles |
| Material 3 typography, colour roles, and controls in settings | Continuous-curvature (superellipse) corners, not circular arcs |
| Themed monochrome icons | Paged home screen with dot indicators |
| The search pill in the dock | Long-press context menus that grow out of the icon |
| Widgets, notification dots, app shortcuts | Folders that zoom open from the icon you tapped |
| A drawer that is a real drawer | Jiggle-mode editing, and a floating dock |

And then Liquid Glass over the top of both.

## Liquid Glass, for real

Translucency on Android is usually a white rectangle at 12% alpha. This is not that.

The problem: a launcher window is translucent, and the wallpaper is composited by the system
*behind* it. `RenderEffect` can only ever sample a layer's **own** content, so there is nothing
behind the panel for it to blur. Every launcher that "does glass" runs into this.

Lumen solves it in three steps:

1. **Own the backdrop.** The wallpaper is drawn *inside* the app — either a snapshot of the user's
   real wallpaper (`WallpaperManager`, when the platform lets a home app read it) or a bundled
   GPU-rendered field (an animated mesh gradient, or aurora ribbons). Either way the pixels belong
   to the launcher.
2. **Capture it once.** The wallpaper and workspace are recorded into a single `GraphicsLayer`
   (`Modifier.backdropSource`). One capture per frame, no matter how many glass panels exist.
3. **Lens it per panel.** Each glass surface re-draws that layer into a private layer, translated so
   the pixels line up, with a chained `RenderEffect`: a Gaussian blur, then an **AGSL** shader that

   * builds a signed-distance field of the panel's own superellipse silhouette,
   * displaces the backdrop along the SDF normal — strongest at the rim, falling off toward the
     centre — so content genuinely bends around the bevel,
   * samples red and blue at slightly different displacements for chromatic dispersion,
   * adds a specular rim keyed to a virtual light, with a matching shadow on the opposite edge,
   * optionally sweeps a travelling sheen, and dithers to kill banding.

   Colour maths runs un-premultiplied, so brightening the rim never overshoots alpha.

The shader and the clip path share the same superellipse exponent, so the refraction rim lands
exactly on the visible edge instead of drifting at the corners.

**The light is real.** With `tiltReactive` on, the accelerometer steers the virtual light direction:
tilt the phone and the highlight slides across every glass surface at once.

**It degrades honestly.**

| Device | What it does |
| --- | --- |
| API 33+ (Android 13+) | Full AGSL: refraction, dispersion, specular, sheen, grain |
| API 31–32 | `RenderEffect` blur + tint + bevel + rim (no refraction) |
| API 26–30 | Translucent tint + bevel + rim, no blur |
| Low-RAM devices | Auto-drops to the Balanced tier |

`GlassQuality.AUTO` picks the tier from the device; the user can pin it, and `OFF` disables glass
everywhere in one switch. Glass panels only redraw when the pixels behind them actually change — a
still home screen costs nothing, and any surface that animates the backdrop (page swipe, drag,
drawer transition, animated wallpaper, sheen) holds a redraw ticket for exactly as long as it needs
one.

## What it does

**Home** — Multiple pages with eight page-transition styles (slide, depth, zoom, cube, carousel,
flip, liquid, parallax), optional infinite scroll, four page-indicator styles, wallpaper parallax, a
glance pill (date / battery / dynamic-island), and a fully configurable grid from 2×2 to 12×12.

**Drag and drop** — Long-press to enter jiggle mode; drag icons between pages, into the dock, onto
each other to make folders, out of folders, or onto Remove/Uninstall targets. Edge-hover flips
pages, hover-to-combine makes folders, and every drop lands through one shared code path so the
dock, grid, folders and drawer all behave identically.

**Dock** — Floating glass, edge glass, transparent or solid; 2–8 columns, 1–3 rows, its own icon
size, and a Google-style search pill (or a glass pill, or a compact orb) above, below or inside it.

**App drawer** — Five layouts (sheet, fullscreen, paged grid, vertical list, category tabs), five
sort orders including usage and colour, A–Z fast-scroll rail, section headers, prediction row, work
profile tab, hidden apps, and four open animations. The default sheet is liquid glass over a
blurred, dimmed home screen.

**Search** — Fuzzy app matching, deep shortcuts, an actual expression calculator, unit conversion,
and a web fallback across five providers.

**Folders** — Five preview styles (2×2 stack, 3×3 grid, fan, ring, cluster), four open animations
including an Apple-style zoom from the tapped icon and a liquid morph, auto-naming from app
category, glass or content-tinted backgrounds.

**Widgets** — Full `AppWidgetHost` implementation: picker with previews, bind/configure flows,
drag-to-place, edge-handle resizing that snaps to the grid, and an optional glass tile behind each
widget so third-party widgets stop looking pasted on.

**Icons** — Ten mask shapes (squircle, circle, rounded square, square, teardrop, hexagon, cookie,
clover, pebble, system) with adjustable corner radius and squircle smoothing, icon-pack support
(ADW/Nova/GO `appfilter.xml`), Material You themed icons, per-icon colour grading, optional gloss,
per-app custom icons and renaming, and an optional glass tile under every icon.

**Notification dots** — Dot, large dot or count, four positions, per-app muting, folder aggregation.

**Gestures** — Ten triggers (swipe up/down, two-finger swipes, double tap, long-press, pinch in/out,
edge swipes, home press) each bindable to any of eighteen actions, including launching a specific
app.

**Settings** — A schema-driven surface over the whole `LauncherSettings` tree: eighteen sections,
searchable, with a live preview of the home screen that updates while you drag a slider, seven
presets (Cupertino, Material You, Liquid Max, Minimal, Performance, One-handed, Hybrid), a glass
playground with per-surface overrides, and JSON export/import of your entire setup.

## Build

Requires JDK 17+ and the Android SDK (compileSdk 36, build-tools 36).

```bash
cd android-launcher
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then set Lumen as the default home app (Settings → Apps → Default apps → Home app, or press Home and
pick it). Two optional grants unlock optional features:

* **Notification access** — notification dots. Settings → Badges → Notification access.
* **Usage access** — usage-based drawer sorting and predictions. Settings → Drawer → Usage access.
* **Device admin (force-lock only)** — requested the first time you use a "sleep" gesture, and used
  for nothing else.

Wallpaper reads are best-effort: recent Android versions restrict `WallpaperManager` for apps that
are not the default home. When the read fails, Lumen falls back to a bundled animated wallpaper so
the glass always has something to refract — or set the source to **System** to show the real
wallpaper through the window, at the cost of refraction over empty areas.

## Architecture

```
app/src/main/java/dev/lumen/launcher/
  LauncherApplication.kt      process-lifetime service graph (manual DI, no Hilt)
  MainActivity.kt             the HOME activity: edge-to-edge, wallpaper window, home-press handling
  data/
    Services.kt               every service interface + the DI container + composition locals
    model/Models.kt           AppKey, AppInfo, workspace items, widget/shortcut models
    workspace/
      WorkspaceOps.kt         pure layout algebra: placement, folders, pages, reflow, bootstrap
      WorkspaceStore.kt       persisted layout (typed DataStore, JSON, debounced)
    prefs/
      LauncherSettings.kt     the entire settings schema, serializable, all defaults
      SettingsStore.kt        optimistic in-memory writes, conflated persistence
      SettingsCatalog.kt      the registry that generates the settings UI
      Presets.kt              bundled looks
    apps/                     LauncherApps enumeration, work profiles, usage tracking
    icons/                    icon rendering, masking, theming, icon packs, two-level cache
    widgets/                  AppWidgetHost controller
    notifications/            notification listener → badge counts
    shortcuts/                deep shortcuts
    wallpaper/                wallpaper snapshot for the glass backdrop
  ui/
    glass/                    the material: AGSL shaders, backdrop capture, style resolution, shapes
    theme/                    Material You colour, typography, spring motion tokens
    wallpaper/                the in-app wallpaper layer (snapshot + GPU wallpapers)
    common/                   AppIcon, labels, badges, haptics, press handling
    drag/                     cross-surface drag session and the single drop-application path
    home/                     pager, grid, dock, indicator, glance pill, folders, context menu
    drawer/                   the app drawer
    search/                   the search engine (no Compose types)
    widgets/                  widget host view and picker
    gestures/                 the gesture layer
    settings/                 the generated settings experience
  system/SystemActions.kt     status bar, lock, assistant, system settings deep links
```

Deliberate choices:

* **No annotation processors.** No Hilt, no Room, no KSP — a manual container and typed DataStore
  keep the build fast and the graph obvious.
* **Settings are one immutable tree.** Persistence, the generated UI, presets, search and
  backup/restore are all derived from `LauncherSettings`, so adding a knob means adding a field and
  one catalog entry.
* **Layout logic is pure.** `WorkspaceOps` has no Android or Compose imports, so drag behaviour is
  reasoned about (and tested) as data transformation.
* **One settings snapshot per frame.** The root collects the settings flow once and publishes it
  through a composition local; leaves never re-collect.
* **Never crash the home screen.** Every cross-app call — `PackageManager`, `LauncherApps`,
  `AppWidgetHost`, wallpaper reads, reflection into `StatusBarManager` — is wrapped and degrades to
  a visible fallback.
