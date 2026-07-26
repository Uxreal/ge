# Releases

| File | What it is |
|---|---|
| `lumen-0.3.0-capsule.apk` | Phase 1 + the Capsule (§4). Unminified, debug-signed sideload build. **Compiles and its logic is unit-tested (68 tests), but the Capsule has not been seen on a screen** — the emulator available during the build lost `system_server` repeatedly, with and without Lumen installed. First real look at the pill will be on your phone. |
| `lumen-0.1.1-phase1.apk` | Phase 1 only, no Capsule. **Verified booting to a working home screen on an Android 15 emulator** (onboarding → model choice → seeded grid → app launch). Keep this one as the fallback if 0.3.0 misbehaves. |

sha256 of `lumen-0.3.0-capsule.apk`:
`9d78064bce804bff4daf2f9a994b2eea1f4f6d94a3ac517c886202dcf56cfdd8`

## Why the APK is 46 MB

Because minification is off, and 45 MB of that is unshrunk dex — the whole of Compose, Material 3,
Hilt and Room with nothing stripped. 0.1.0 shipped R8-minified at 2.8 MB and painted a black
screen: AGP 8.9.2's R8 predates Kotlin 2.2 metadata and silently mangled the output. AGP is 8.13
now and its R8 very likely handles it, but "very likely" is not a verification, and this is exactly
the failure that cost a release once already. Minification goes back on when someone can put the
minified build on a screen and watch it work — see `STATUS.md`.

Verify a download with `apksigner verify --print-certs <apk>`.
