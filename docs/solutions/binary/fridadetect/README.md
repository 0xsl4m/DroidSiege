# binary / fridadetect — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M7 Insufficient Binary Protections · **References:** MASVS-RESILIENCE-2 · MASTG-TEST-0x15

## How this family works

The app tries to detect Frida: named-thread scan, port scan, and a maps-file scan. Each tier falls to a slightly stealthier Frida configuration.

**Vulnerable code:** `challenges/binary/BinaryFamily.kt (FridaDetect)` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Frida Port Check

**Vulnerable behavior:** Scans running threads for 'frida' / 'gum-js-loop' names.

**Exploit:**
1. Spawn with a renamed gadget or use the bundled rename config.
2. The thread names no longer match; the detector passes.

**Flag:** `DS{binary_fridadetect_L1_82c4a9}`

**Fix:** Detection raises cost; assume a determined attacker wins client-side.

## L2 🟡 — Loaded Lib Scan

**Vulnerable behavior:** Scans open TCP ports 27042/27043.

**Exploit:**
1. Run Frida on a non-default port (bundled config).
2. The port scan finds nothing.

**Flag:** `DS{binary_fridadetect_L2_5f71e3}`

**Fix:** Same lesson.

## L3 🟠 — Native Anti-Hook

**Vulnerable behavior:** Greps /proc/self/maps for frida-agent paths.

**Exploit:**
1. The bundled script maps the agent under a benign path (or uses Stalker-free injection).
2. The maps scan reads only benign paths.

**Flag:** `DS{binary_fridadetect_L3_d90b26}`

**Fix:** Same lesson — move the trust server-side.

## Tooling

- [`tools/frida_stealth.js`](tools/frida_stealth.js) — renames threads, remaps the agent and dodges the port scan; plus the launcher config.

## Vulnerable vs hardened

```kotlin
// vulnerable
// thread/port/maps scans in Java

// hardened
// attestation + server-side risk signals; client checks as speed bumps only
```
