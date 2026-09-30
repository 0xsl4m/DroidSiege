# network / trustall — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M5 Insecure Communication · **References:** MASVS-NETWORK-2 · MASTG-TEST-0x30

## How this family works

The TLS clients here accept any certificate: an empty X509TrustManager, hostname verification disabled, and an SDK whose TLS stack ships its own trust-all.

**Vulnerable code:** `challenges/network/TrustAllFamily.kt + X509TrustManagerCompat.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Accept Everything

**Vulnerable behavior:** checkServerTrusted is a no-op — any cert is accepted.

**Exploit:**
1. The in-app MITM endpoint presents an attacker cert; press the call (insecure mode).
2. The MITM reads (and can rewrite) the response — the flag shows in the proxy log.

**Flag:** `DS{network_trustall_L1_78b4c2}`

**Fix:** Default platform trust; no custom TrustManagers.

## L2 🟡 — Any Host Will Do

**Vulnerable behavior:** Hostname verification is switched off (sslSocketFactory + permissive HostnameVerifier).

**Exploit:**
1. The MITM cert has the wrong CN; the call still succeeds (insecure mode).
2. The console shows the substitution the attacker performed.

**Flag:** `DS{network_trustall_L2_0d96e5}`

**Fix:** Never replace the default HostnameVerifier; if you must, verify strictly.

## L3 🟠 — Debug Trust

**Vulnerable behavior:** A bundled 'SDK' initializes its own trust-all TLS context globally.

**Exploit:**
1. Trigger the SDK path; the bundled mitm script intercepts the SDK call.
2. The global trust-all poisons every connection, including the app's own.

**Flag:** `DS{network_trustall_L3_41f7a3}`

**Fix:** Audit dependencies for TLS tampering; pin or drop such SDKs.

## L4 🔴 — SDK Shortcut

**Vulnerable behavior:** Chained: MITM + trust-all + a cleartext fallback the MITM can force.

**Exploit:**
1. Run the full mitm script; force the handshake failure.
2. The app falls back to http; the proxy captures the flag in the clear.

**Flag:** `DS{network_trustall_L4_6c28d9}`

**Fix:** TLS errors must fail closed, never downgrade.

## Tooling

- [`tools/intercept.sh`](tools/intercept.sh) — MITM capture flow for the trust-all tiers; prints every substituted body.

## Vulnerable vs hardened

```kotlin
// vulnerable
object : X509TrustManagerCompat { override fun checkServerTrusted(...) = Unit }

// hardened
// platform default trust; certificate pinning where it matters
```
