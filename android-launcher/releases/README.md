# Releases

| File | What it is |
|---|---|
| `lumen-0.1.0-phase1.apk` | Phase 1, R8-minified, debug-signed (sideload build — see DECISIONS D2) |

Built from the commit that contains it; verify with
`apksigner verify --print-certs lumen-0.1.0-phase1.apk`.

If the minified build ever misbehaves on your device, build the unminified equivalent yourself:
`./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
