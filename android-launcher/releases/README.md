# Releases

**Permanent link to the newest build** (same URL every release):
`releases/lumen-latest.apk`

| File | What it is |
|---|---|
| `lumen-latest.apk` | Always the newest build. Currently identical to 1.2.1. |
| `lumen-1.2.1-lifeline.apk` | **2.9 MB, R8-minified.** The lifeline, after "it crashes on launch" on 1.2.0: any crash now writes its stack trace to the phone's **Downloads** folder (`lumen-crash-….txt`) so it can be shared even if the launcher never opens; after two dead boots the third comes up in **safe mode** (Capsule off, status bar visible, banner with Share crash log / Try full mode) so the Home button always works; everything at process start is individually guarded. All 1.2.0 features included, hardened. |
| `lumen-0.1.1-phase1.apk` | Phase 1 only, no Capsule, unminified. The old fallback. |

sha256 of `lumen-1.2.1-lifeline.apk` / `lumen-latest.apk`:
`632262c16b382105a1195744bb485df168756f513fa32108a021ac5ca54d87b9`

Not screenshot-verified this round: this container has no KVM, so no emulator can boot at all.
Compile-verified with 86 unit tests passing (two new for the safe-mode boot protocol). The 1.2.0
crash's root cause is unknown until a trace arrives — this build exists to deliver that trace
and to keep the launcher usable regardless.

Verify a download with `apksigner verify --print-certs <apk>` — the signer is
`CN=Lumen Personal Build` from `signing/lumen-release.keystore` in this repo.
