# Releases

**Permanent link to the newest build** (same URL every release):
`releases/lumen-latest.apk`

| File | What it is |
|---|---|
| `lumen-latest.apk` | Always the newest build. Currently identical to 0.8.0. |
| `lumen-0.8.0-live.apk` | **2.9 MB, R8-minified.** The reference-parity round: tap opens the app (long-press expands), a second activity detaches into a satellite bubble beside the pill, media shows album artwork with animated equaliser bars and live rim progress, and the expanded media card gets artwork/title/progress/transport. Includes everything from 0.7.0 (tight lens cap, dim-behind-island, anchored menus). |
| `lumen-0.1.1-phase1.apk` | Phase 1 only, no Capsule, unminified. The old fallback. |

sha256 of `lumen-0.8.0-live.apk` / `lumen-latest.apk`:
`0fb7544ac2b2131b36043bffdab1c4bc0e6e2a16a60f3afee3e3ea6846675cd8`

Not screenshot-verified this round: the build emulator's system_server is in a terminal ANR storm
(host fault, documented since 0.3.0). Compile-verified with 77 unit tests passing, including two
new cases for the lens cap (a padded round hole is tightened; a wide dual-lens cutout is not).

Verify a download with `apksigner verify --print-certs <apk>`.
