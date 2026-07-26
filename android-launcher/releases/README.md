# Releases

| File | What it is |
|---|---|
| `lumen-0.1.1-phase1.apk` | Phase 1. Unminified, debug-signed sideload build — **verified booting to a working home screen on an Android 15 emulator** (onboarding → model choice → seeded grid → app launch). |

0.1.0 is withdrawn: its R8-minified output rendered a black screen (reproduced on the emulator —
the app ran and held window focus but painted nothing). Root cause: AGP 8.9.2's R8 predates
Kotlin 2.2 metadata and silently mangled the output. AGP is now 8.13; minification stays off until
the minified output is itself verified on hardware (STATUS.md tracks re-enabling).

Verify a download with `apksigner verify --print-certs <apk>`.
