# supplychain / dynload — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M2 Inadequate Supply Chain Security · **References:** MASVS-RESILIENCE-3 · MASTG-TEST-0x57

## How this family works

Dynamic code delivery done wrong: DexClassLoader from external storage, unverified feature downloads, unsigned update payloads, and a writable native .so path.

**Vulnerable code:** `challenges/supplychain/SupplyChainFamily.kt (DynLoadLab)` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — External Dex

**Vulnerable behavior:** The plugin loader reads `Android/data/com.droidsiege/files/plugin.dex`.

**Exploit:**
1. Replace plugin.dex with your own (bundled script compiles one), press Load.
2. The swapped plugin executes — the loader reveal prints the flag.

**Flag:** `DS{supplychain_dynload_L1_a54e07}`

**Fix:** No code from writable storage; bundled + signature-verified plugins only.

## L2 🟡 — Unverified Feature

**Vulnerable behavior:** The dynamic feature downloads over http with no integrity check.

**Exploit:**
1. Intercept/serve a swapped module (bundled script hosts it).
2. The installer accepts it — the reveal prints the flag.

**Flag:** `DS{supplychain_dynload_L2_39c1b2}`

**Fix:** Signed delivery verified before install.

## L3 🟠 — Unsigned Update

**Vulnerable behavior:** The updater trusts whatever the channel serves.

**Exploit:**
1. Let the bundled script play the update server.
2. The unsigned payload installs — flag revealed.

**Flag:** `DS{supplychain_dynload_L3_f68d40}`

**Fix:** Update signatures pinned to the vendor key; failure is fatal.

## L4 🔴 — Writable .so

**Vulnerable behavior:** The native plugin loads `filesDir/plugins/native.so`.

**Exploit:**
1. Stage your .so (the app action writes the placeholder; the script compiles a real one), re-trigger.
2. The constructor runs — the reveal prints the flag.

**Flag:** `DS{supplychain_dynload_L4_71e5a9}`

**Fix:** System.loadLibrary from the APK only.

## Tooling

- [`tools/plugin_swap.sh`](tools/plugin_swap.sh) — builds the attacker dex/.so, swaps the writable paths and drives the loaders.

## Vulnerable vs hardened

```kotlin
// vulnerable
DexClassLoader(externalDex.path, …, classLoader)

// hardened
// plugins inside the APK; signature verification before a single byte executes
```
