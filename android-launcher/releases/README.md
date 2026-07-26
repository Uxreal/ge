# Releases

| File | What it is |
|---|---|
| `lumen-0.4.0-media.apk` | **2.9 MB, R8-minified.** The Capsule on the camera cutout, plus media playback as a card (opt-in via Settings → Capsule → Media playback). The minified pipeline was walked end to end on an Android 15 emulator: cold install → onboarding → PACKED seeding with real icons → Capsule pill → an `am broadcast` push drawing rim progress → an app launched from the grid. |
| `lumen-0.1.1-phase1.apk` | Phase 1 only, no Capsule, unminified. The old fallback, kept until 0.4.0 survives on real hardware. |

sha256 of `lumen-0.4.0-media.apk`:
`a57081dde68b522a0a7241f51b27c61202807e4d3ccec721e6c2ef905ecf49f5`

0.3.x are withdrawn: 0.3.0's pill floated below the status bar and its push API was unreachable;
0.3.1 fixed both but shipped unminified at 46 MB. Minification is back on as of 0.4.0 — the gate
("watch the minified output work on a screen") is met, see DECISIONS D29.

Still unverified anywhere: the cutout embrace against a real punch-hole (unit-tested geometry; no
emulated cutout is a small centred one), the media card (this emulator has no active MediaSession
to show), and everything about feel. Verify a download with `apksigner verify --print-certs <apk>`.
