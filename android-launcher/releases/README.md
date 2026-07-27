# Releases

**Permanent link to the newest build** (same URL every release):
`releases/lumen-latest.apk`

| File | What it is |
|---|---|
| `lumen-latest.apk` | Always the newest build. Currently identical to 1.0.0. |
| `lumen-1.0.0-fresh.apk` | **2.9 MB, R8-minified.** Fresh install identity: the app is now `dev.lumen.launcher2`, signed with a permanent key committed to the repo, because the old package on the phone was signed by a rotated build key and could never be updated again. Installs cleanly beside the old Lumen; uninstall the old one after switching, then redo onboarding, default-home, media consent and the dock (new id = new app data). All of 0.9.x included: one shape system, wallpaper theming, dock, App Library, the island. |
| `lumen-0.1.1-phase1.apk` | Phase 1 only, no Capsule, unminified. The old fallback. |

sha256 of `lumen-1.0.0-fresh.apk` / `lumen-latest.apk`:
`aed5d4aebce4d58bea621131f342bc5fc61bf1a8c585dae585fa5185d41308bf`

Not screenshot-verified this round: the build emulator's system_server is in a terminal ANR storm
(host fault, documented since 0.3.0). Compile-verified with 77 unit tests passing, including two
new cases for the lens cap (a padded round hole is tightened; a wide dual-lens cutout is not).

Verify a download with `apksigner verify --print-certs <apk>` — the signer is
`CN=Lumen Personal Build` from `signing/lumen-release.keystore` in this repo.
