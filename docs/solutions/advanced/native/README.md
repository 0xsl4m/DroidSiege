# advanced / native — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M8+ Advanced Attacks · **References:** MASVS-RESILIENCE-3 · MASTG-TEST-0x53

## How this family works

The real JNI library `libdroidsiege-vault.so` (NDK r27c, CMake) ships four tiers: a magic-value check, an over-read, a format-string sink, and a hidden export.

**Vulnerable code:** `app/src/main/cpp/native-vault.cpp + challenges/advanced/NativeFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Magic Value Check

**Vulnerable behavior:** nativeCheck compares a magic value the caller can read from the .so.

**Exploit:**
1. `strings libdroidsiege-vault.so | grep <magic>` (bundled script finds it) or hook the bridge.
2. Feed the magic value — check passes, flag reveals.

**Flag:** `DS{advanced_native_L1_64f3b7}`

**Fix:** Client-side checks are speed bumps; nothing secret in native code.

## L2 🟡 — Pool Over-read

**Vulnerable behavior:** nativeParse trusts the caller's declared length — over-read.

**Exploit:**
1. Declare a length past the buffer (the in-app input).
2. The over-read tail contains the secret.

**Flag:** `DS{advanced_native_L2_09d5c8}`

**Fix:** Clamp lengths to the actual allocation.

## L3 🟠 — Format String

**Vulnerable behavior:** nativeLog uses the user string AS the format — %s leaks the secret argument.

**Exploit:**
1. Enter `%s` as the format input.
2. The log sink prints the adjacent secret argument — the flag.

**Flag:** `DS{advanced_native_L3_72a1e4}`

**Fix:** Fixed format strings; user text is always an argument, never the format.

## L4 🔴 — Hidden Flag Function

**Vulnerable behavior:** hiddenPrintFlag is exported but never called — reachable once you control a function pointer (the L2 over-read provides it).

**Exploit:**
1. Enumerate exports: `readelf -sW libdroidsiege-vault.so | grep hidden` (bundled).
2. The challenge's reveal (or a Frida call to the export) returns the flag.

**Flag:** `DS{advanced_native_L4_3b96f2}`

**Fix:** Strip secret-bearing symbols; secrets don't belong in native code.

## Tooling

- [`tools/native_recon.sh`](tools/native_recon.sh) — strings/readelf/Frida one-liners against libdroidsiege-vault.so for all four tiers.

## Vulnerable vs hardened

```kotlin
// vulnerable
__android_log_print(INFO, TAG, userFmt, secret)

// hardened
__android_log_print(INFO, TAG, "%s", userFmt)
```
