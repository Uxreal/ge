# Releases

| File | What it is |
|---|---|
| `lumen-0.3.1-capsule.apk` | The Capsule redesigned after the first hardware report: docks on the punch-hole camera (camera-black and opaque, so the hole disappears into it), one coordinated expand animation, working push API. **Emulator-verified by screenshot**: glance pill, a pushed card drawing its progress on the pill's own outline, tap-to-expand. The cutout embrace itself is unit-tested but visually unverified until it meets a real punch-hole — no emulated cutout is a small centred one. |
| `lumen-0.1.1-phase1.apk` | Phase 1 only, no Capsule. The known-good fallback, screenshot-verified end to end. |

sha256 of `lumen-0.3.1-capsule.apk`:
`a96454598c39f16542fb8c71ca23d472f6a0635d3f0b4bf504386ab624aeced4`

0.3.0 is withdrawn: its pill floated below the status bar as a frosted grey blob, its expand ran
three uncoordinated animations, and its push API could not receive the documented broadcast at all
(manifest receivers stopped seeing implicit broadcasts in Android 8). All three are fixed in 0.3.1
(DECISIONS D25–D27).

## Why the APK is 46 MB

Because minification is off, and most of that is unshrunk dex. 0.1.0 shipped R8-minified at 2.8 MB
and painted a black screen: AGP 8.9.2's R8 predates Kotlin 2.2 metadata and silently mangled the
output. AGP is 8.13 now and its R8 very likely handles it, but "very likely" is not a
verification, and this is exactly the failure that cost a release once already. Minification goes
back on when someone can put the minified build on a screen and watch it work. See `STATUS.md`.

Verify a download with `apksigner verify --print-certs <apk>`.
