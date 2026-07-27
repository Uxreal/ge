# Releases

**Permanent link to the newest build** (same URL every release):
`releases/lumen-latest.apk`

| File | What it is |
|---|---|
| `lumen-latest.apk` | Always the newest build. Currently identical to 1.1.0. |
| `lumen-1.1.0-essentials.apk` | **2.9 MB, R8-minified.** The essentials: a biometric hidden shelf in the App Library (hide apps from the long-press menu; the padlock tile unlocks with fingerprint/face/PIN and re-locks when the drawer closes), an always-tappable Apps button on the dock bar, and one-time factory seeding of your phone's real default dialer/messages/browser/camera to the dock with clock and settings on the grid. Same dev.lumen.launcher2 identity and permanent key as 1.0.0 — installs over it cleanly. |
| `lumen-0.1.1-phase1.apk` | Phase 1 only, no Capsule, unminified. The old fallback. |

sha256 of `lumen-1.1.0-essentials.apk` / `lumen-latest.apk`:
`e88267096600f4d0d1b454bc75a3c78bfe13845dac310d31bf57f9b20e6b7001`

Not screenshot-verified this round: the build emulator's system_server is in a terminal ANR storm
(host fault, documented since 0.3.0). Compile-verified with 77 unit tests passing, including two
new cases for the lens cap (a padded round hole is tightened; a wide dual-lens cutout is not).

Verify a download with `apksigner verify --print-certs <apk>` — the signer is
`CN=Lumen Personal Build` from `signing/lumen-release.keystore` in this repo.
