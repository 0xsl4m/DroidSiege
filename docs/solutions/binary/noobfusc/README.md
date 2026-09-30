# binary / noobfusc — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M7 Insufficient Binary Protections · **References:** MASVS-RESILIENCE-1 · MASTG-TEST-0x55

## How this family works

The release build ships without R8 obfuscation: readable smali, string resources with base64 'secrets', signature checks the attacker controls, and a debuggable flag.

**Vulnerable code:** `app/build.gradle.kts (release config) + challenges/binary/NoobfuscFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Plaintext String

**Vulnerable behavior:** The APK strings contain the flag constant — no obfuscation at all.

**Exploit:**
1. `apktool d app-debug.apk && grep -r 'DS{' .` (bundled script).
2. The L1 flag is in the resources/strings.

**Flag:** `DS{binary_noobfusc_L1_6e29f5}`

**Fix:** R8 with obfuscation; secrets out of the binary entirely.

## L2 🟡 — Encoded Resource

**Vulnerable behavior:** A base64 'hidden' string resource decodes to the L2 flag.

**Exploit:**
1. `echo RFN7YmluYXJ5X25vb2JmdXNjX0wyX2I4NDFkMH0= | base64 -d` (or the bundled script).
2. Encoding is transport, not concealment.

**Flag:** `DS{binary_noobfusc_L2_b841d0}`

**Fix:** No secret strings in resources; server-side redemption.

## L3 🟠 — Readable Smali

**Vulnerable behavior:** Readable smali shows the unlock logic comparing a magic constant.

**Exploit:**
1. Open the smali of the challenge class (bundled script prints the exact file).
2. Read the magic constant; enter it as the unlock input.

**Flag:** `DS{binary_noobfusc_L3_27c6a8}`

**Fix:** Obfuscation + logic moved server-side.

## L4 🔴 — Signature Tamper

**Vulnerable behavior:** The signature check trusts `PackageManager.GET_SIGNATURES` output compared to a constant — and the app is debuggable.

**Exploit:**
1. Re-sign with the public debug key (bundled script) — the check compares a known value.
2. The tampered build passes its own integrity check and prints the flag.

**Flag:** `DS{binary_noobfusc_L4_f03d91}`

**Fix:** Play Integrity / key attestation; debuggable=false in release.

## Tooling

- [`tools/unpack_noobfusc.sh`](tools/unpack_noobfusc.sh) — apktool/jadx unpack + greps that recover every tier's artifact.
- [`tools/resign_check.py`](tools/resign_check.py) — re-signs the APK with the debug key and walks the L4 signature check.

## Vulnerable vs hardened

```kotlin
// vulnerable
minifyEnabled = false // no obfuscation

// hardened
minifyEnabled = true + shrinkResources; no secrets in the binary
```
