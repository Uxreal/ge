# Releases

**Permanent link to the newest build** (same URL every release):
`releases/lumen-latest.apk`

| File | What it is |
|---|---|
| `lumen-latest.apk` | Always the newest build. Currently identical to 0.9.1. |
| `lumen-0.9.1-round.apk` | **2.9 MB, R8-minified.** One shape system: every container shares superellipse n=3.0 on a five-stop radius ladder, capsule forms keep circular ends, icon masks rounder to match. Plus 0.9.0's wallpaper-derived theming (live), the five-slot dock, and the quieter drawer. |
| `lumen-0.1.1-phase1.apk` | Phase 1 only, no Capsule, unminified. The old fallback. |

sha256 of `lumen-0.9.1-round.apk` / `lumen-latest.apk`:
`2e302103ec1bd35a9209a8fec3bf7ba01f16539914e7dcc3157628a67375b0c5`

Not screenshot-verified this round: the build emulator's system_server is in a terminal ANR storm
(host fault, documented since 0.3.0). Compile-verified with 77 unit tests passing, including two
new cases for the lens cap (a padded round hole is tightened; a wide dual-lens cutout is not).

Verify a download with `apksigner verify --print-certs <apk>`.
