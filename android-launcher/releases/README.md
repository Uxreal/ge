# Releases

**Permanent link to the newest build** (same URL every release):
`releases/lumen-latest.apk`

| File | What it is |
|---|---|
| `lumen-latest.apk` | Always the newest build. Currently identical to 0.7.0. |
| `lumen-0.7.0-polish.apk` | **2.9 MB, R8-minified.** The tight-pill round: the assumed lens size is capped at 22dp for round holes (OEMs pad the reported cutout rect, which is why the pill ran large) with 3dp margins; the expanded island dims the screen behind it and collapses on an outside tap; the drawer's long-press menu anchors to the pressed icon and springs from it. Everything from 0.6.0 (App Library, round pill, media self-heal, owned top band, diagnostics) included. |
| `lumen-0.1.1-phase1.apk` | Phase 1 only, no Capsule, unminified. The old fallback. |

sha256 of `lumen-0.7.0-polish.apk` / `lumen-latest.apk`:
`bc94976a7c05cf404679d9065d20d8eb24ff0d7ac273e0b97ce8718d8600e297`

Not screenshot-verified this round: the build emulator's system_server is in a terminal ANR storm
(host fault, documented since 0.3.0). Compile-verified with 77 unit tests passing, including two
new cases for the lens cap (a padded round hole is tightened; a wide dual-lens cutout is not).

Verify a download with `apksigner verify --print-certs <apk>`.
