# Releases

**Permanent link to the newest build** (same URL every release):
`releases/lumen-latest.apk`

| File | What it is |
|---|---|
| `lumen-latest.apk` | Always the newest build. Currently identical to 0.9.0. |
| `lumen-0.9.0-dock.apk` | **2.9 MB, R8-minified.** Wallpaper-derived theming (the launcher recolours itself to your wallpaper, live), a five-slot dock above the page dots (pin apps from the drawer's long-press menu, long-press a dock icon to unpin), and the drawer no longer throws the keyboard over the App Library. Includes all of 0.8.x (tap-to-open with the Android 14+ launch fix, satellite bubble, artwork, EQ bars, live rim, restricted-settings guidance). |
| `lumen-0.1.1-phase1.apk` | Phase 1 only, no Capsule, unminified. The old fallback. |

sha256 of `lumen-0.9.0-dock.apk` / `lumen-latest.apk`:
`6caf0b7756d1f37ff10e76e3e3e1b2b25e2e79c63ff7850b308c87dabaa1227a`

Not screenshot-verified this round: the build emulator's system_server is in a terminal ANR storm
(host fault, documented since 0.3.0). Compile-verified with 77 unit tests passing, including two
new cases for the lens cap (a padded round hole is tightened; a wide dual-lens cutout is not).

Verify a download with `apksigner verify --print-certs <apk>`.
