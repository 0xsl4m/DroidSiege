# binary / rootdetect — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M7 Insufficient Binary Protections · **References:** MASVS-RESILIENCE-2 · MASTG-TEST-0x15

## How this family works

The vault checks for root with three escalating client-side checks — all of which Frida/objection neutralize in one hook.

**Vulnerable code:** `challenges/binary/BinaryFamily.kt (RootDetect)` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Su File Check

**Vulnerable behavior:** Root is detected by testing for the `su` binary.

**Exploit:**
1. On a rooted image: the check fires; on the emulator it doesn't — either way, hook `File.exists` with Frida (bundled script) to flip the verdict.
2. The console prints the unlocked flag.

**Flag:** `DS{binary_rootdetect_L1_91e3a7}`

**Fix:** Client checks raise cost, not security — pair with server-side signals.

## L2 🟡 — Emulator Props

**Vulnerable behavior:** Checks Build.TAGS for test-keys.

**Exploit:**
1. Frida hook on Build.TAGS (bundled script) returns 'release-keys'.
2. The check passes.

**Flag:** `DS{binary_rootdetect_L2_46b8d2}`

**Fix:** Same: defense-in-depth only.

## L3 🟠 — Debugger Check

**Vulnerable behavior:** Native check via the vault .so (nativeCount returns a magic value when 'clean').

**Exploit:**
1. Hook the JNI bridge with Frida (bundled script) — intercept the native result.
2. The native verdict is replaced at the bridge.

**Flag:** `DS{binary_rootdetect_L3_c0f159}`

**Fix:** Native checks add cost; integrity of the check itself is still client-side.

## Tooling

- [`tools/root_bypass.js`](tools/root_bypass.js) — Frida script flipping every root-check verdict (File.exists, Build.TAGS, JNI bridge).

## Vulnerable vs hardened

```kotlin
// vulnerable
if (File("/system/bin/su").exists()) lockDown()

// hardened
// client signals + server-side attestation (Play Integrity)
```
