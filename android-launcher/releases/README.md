# Releases

**Permanent link to the newest build** (same URL every release):
`releases/lumen-latest.apk`

| File | What it is |
|---|---|
| `lumen-latest.apk` | Always the newest build. Currently identical to 0.8.1. |
| `lumen-0.8.1-unblock.apk` | **2.9 MB, R8-minified.** The unblock round: fires PendingIntents with the Android 14+ background-start opt-in (island taps and action chips were silent no-ops without it), walks through Android's restricted-settings wall that blocks media consent for sideloaded apps, and fixes drag-down launching instead of expanding. Includes all of 0.8.0 (tap-to-open, satellite bubble, artwork, EQ bars, live rim). |
| `lumen-0.1.1-phase1.apk` | Phase 1 only, no Capsule, unminified. The old fallback. |

sha256 of `lumen-0.8.1-unblock.apk` / `lumen-latest.apk`:
`1d1c6cdd65319a165d063504a73f4609ab0a79202849034914491f13ebf29aa2`

Not screenshot-verified this round: the build emulator's system_server is in a terminal ANR storm
(host fault, documented since 0.3.0). Compile-verified with 77 unit tests passing, including two
new cases for the lens cap (a padded round hole is tightened; a wide dual-lens cutout is not).

Verify a download with `apksigner verify --print-certs <apk>`.
