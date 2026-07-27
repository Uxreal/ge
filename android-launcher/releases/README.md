# Releases

**Permanent link to the newest build** (same URL every release):
`releases/lumen-latest.apk`

| File | What it is |
|---|---|
| `lumen-latest.apk` | Always the newest build. Currently identical to 1.2.0. |
| `lumen-1.2.0-polish.apk` | **2.9 MB, R8-minified.** The polish round: swipe down anywhere on home opens the notification shade, notification dots on icons everywhere (grid, dock, folders, drawer — same consent as media, package name only, never content, toggle in Settings → Home), tap the strip's time for the clock and its battery for the battery screen, press feedback on the dock's Apps button, and page dots only when there is more than one page. Installs cleanly over 1.1.x. |
| `lumen-0.1.1-phase1.apk` | Phase 1 only, no Capsule, unminified. The old fallback. |

sha256 of `lumen-1.2.0-polish.apk` / `lumen-latest.apk`:
`521c31c34ee12e23d3342d3bc64c93ce48b1bd689ee7a7f8773d96d650a33f01`

Not screenshot-verified this round: the build emulator's system_server is in a terminal ANR storm
(host fault, documented since 0.3.0). Compile-verified with 84 unit tests passing, including four
new cases for the notification-dot policy (ongoing notifications never dot; many notifications
from one app are one dot).

Verify a download with `apksigner verify --print-certs <apk>` — the signer is
`CN=Lumen Personal Build` from `signing/lumen-release.keystore` in this repo.
