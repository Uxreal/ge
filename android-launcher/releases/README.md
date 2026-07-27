# Releases

| File | What it is |
|---|---|
| `lumen-0.5.0-island.apk` | **2.9 MB, R8-minified.** The island round: pitch-black pill that collapses to a bare ring around the camera at rest, the system status bar hidden on the home screen with Lumen drawing time and battery in its own type beside the pill (opt-out in Settings → Capsule), media playback cards, and field diagnostics (a self-test card, a live docking report, and a media status line, all under Settings → Capsule). |
| `lumen-0.1.1-phase1.apk` | Phase 1 only, no Capsule, unminified. The old fallback. |

sha256 of `lumen-0.5.0-island.apk`:
`74656500ffabdeddbd3cc932f7573f5b107ea81fa8d8b9c9538ebee4d0b61539`

The camera embrace is confirmed on a Z Flip as of 0.4.0 ("island covers camera"); 0.5.0 answers
the rest of that report — darker, and the top icons redone to fit. Swipe down from the very top
edge to summon the system bar and notification shade; they hide again on their own.

Verify a download with `apksigner verify --print-certs <apk>`.
