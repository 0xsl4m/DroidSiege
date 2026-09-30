# supplychain / vulndep — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M2 Inadequate Supply Chain Security · **References:** MASVS-RESILIENCE-3 · MASTG-TEST-0x57

## How this family works

The dependency lab simulates the real-world CVE classes (the write-up names the actual analogues — e.g. CVE-2020-8913): an old decoder with a buffer over-read, a transitive crypto helper, native deserialization, and a writable plugin path.

**Vulnerable code:** `challenges/supplychain/SupplyChainFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Vulnerable Decoder

**Vulnerable behavior:** The bundled decoder trusts the header's declared height — over-read leaks adjacent memory.

**Exploit:**
1. Press Decode with the crafted header (the action simulates the huge height).
2. The over-read tail prints the recovery record — the flag.

**Flag:** `DS{supplychain_vulndep_L1_48d2b6}`

**Fix:** SCA: upgrade the dependency; validate untrusted headers before decode.

## L2 🟡 — Transitive Reach

**Vulnerable behavior:** A transitive dependency puts an ECB helper on the classpath, reachable from the vault render.

**Exploit:**
1. `./gradlew :app:dependencies` shows the chain (bundled script prints it).
2. Trigger the render — the helper decrypts with ECB and the flag appears.

**Flag:** `DS{supplychain_vulndep_L2_c75a19}`

**Fix:** Pin/exclude transitive deps; fail builds on vulnerable resolutions.

## L3 🟠 — Unsafe Deserialization

**Vulnerable behavior:** The sync path deserializes a file with ObjectInputStream.

**Exploit:**
1. Plant/inspect `files/sync_payload.ser` (the app action shows the flow).
2. The insecure readObject accepts anything — gadget-chain territory; the in-app reveal shows the flag.

**Flag:** `DS{supplychain_vulndep_L3_2e90f4}`

**Fix:** Never deserialize; JSON + schema validation.

## L4 🔴 — Writable Plugin

**Vulnerable behavior:** The dependency loads a config/plugin from external storage.

**Exploit:**
1. Write your plugin.dex to the loader path, re-trigger the load (bundled script).
2. The loader executes attacker code — the reveal shows the flag.

**Flag:** `DS{supplychain_vulndep_L4_6b13d8}`

**Fix:** Bundle plugins in the APK; verify signatures before load.

## Tooling

- [`tools/dependency_tree.sh`](tools/dependency_tree.sh) — prints the vulnerable chain and drives the decoder/plugin flows.

## Vulnerable vs hardened

```kotlin
// vulnerable
implementation("com.old:decoder:1.2") // CVE-class sim

// hardened
// SCA gate in CI + pinned, patched versions
```
