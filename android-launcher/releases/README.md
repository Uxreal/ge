# Releases

**Permanent link to the newest build** (same URL every release):
`releases/lumen-latest.apk`

| File | What it is |
|---|---|
| `lumen-latest.apk` | Always the newest build. Currently identical to 1.1.1. |
| `lumen-1.1.1-onscreen.apk` | **2.9 MB, R8-minified.** The on-screen fix: factory-seeded grid apps (clock, settings) were being placed against a guessed 4×5 grid before the real, shorter grid was measured, stranding them below the visible rows. Seeding now waits for the measured grid, a reclaim pass moves anything stranded off-grid back to the first free cell (so this build heals a 1.1.0 install on update), and Settings → Home gained "Apply factory layout". Same identity and key as 1.1.0 — installs over it cleanly. |
| `lumen-0.1.1-phase1.apk` | Phase 1 only, no Capsule, unminified. The old fallback. |

sha256 of `lumen-1.1.1-onscreen.apk` / `lumen-latest.apk`:
`16acc47cf3a2253908fefd641459c782b781a9f8d1756a2adc34106ea061971d`

Not screenshot-verified this round: the build emulator's system_server is in a terminal ANR storm
(host fault, documented since 0.3.0). Compile-verified with 80 unit tests passing, including
three new layout-engine cases for the off-grid reclaim (a stranded item is rescued without moving
in-bounds items; a fitting layout is untouched; widgets are exempt and a full page overflows).

Verify a download with `apksigner verify --print-certs <apk>` — the signer is
`CN=Lumen Personal Build` from `signing/lumen-release.keystore` in this repo.
