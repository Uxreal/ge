# Releases

| File | What it is |
|---|---|
| `lumen-0.6.0-library.apk` | **2.9 MB, R8-minified.** The drawer is an App Library: category shelves as tiles (three direct-launch icons plus a mini-cluster that opens the shelf), A–Z one chip away, search unchanged. The pill draws at its own near-stadium curvature after the "looks like a square" report, and media playback gained a main-thread-safe refresh plus a once-a-minute self-heal. Everything from 0.5.0 (pitch-black ring on the camera, Lumen-owned top band, diagnostics) is in. |
| `lumen-0.1.1-phase1.apk` | Phase 1 only, no Capsule, unminified. The old fallback. |

sha256 of `lumen-0.6.0-library.apk`:
`b76de20e18c84b593d1151f271db3b686316962f1d149d52a2ccfe9a6a04e175`

The Library view is compile-verified and unit-tested but not screenshot-verified: the build
emulator's system_server degenerated into a permanent ANR storm before the drawer could be opened
(with and without Lumen installed — a host fault, on record since 0.3.0). The pill's new
curvature and the top band were screenshot-verified on a fresh boot before it died.

If media still shows nothing: Settings → Capsule → the media status line says exactly which stage
is failing (consent absent / granted but idle / following a named package).

Verify a download with `apksigner verify --print-certs <apk>`.
