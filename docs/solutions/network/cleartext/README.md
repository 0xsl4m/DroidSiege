# network / cleartext — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M5 Insecure Communication · **References:** MASVS-NETWORK-1 · MASTG-TEST-0x56

## How this family works

The sync feature speaks plain HTTP against a loopback endpoint (the in-app MockBackend, or the real lab backend via Settings). Everything is on the wire, plaintext.

**Vulnerable code:** `challenges/network/CleartextFamily.kt + network/MockBackend.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Plain Session

**Vulnerable behavior:** The recovery fetch is `http://` — body readable by any on-path observer.

**Exploit:**
1. Press Fetch (insecure mode) and read the console's full exchange; or capture with `tcpdump`/mitmproxy on the emulator.
2. The response body carries the flag.

**Flag:** `DS{network_cleartext_L1_61d4b8}`

**Fix:** TLS everywhere + `cleartextTrafficPermitted=false` in the network security config.

## L2 🟡 — One Odd Call

**Vulnerable behavior:** One sync call in a batch was never migrated to TLS.

**Exploit:**
1. Run the batch; the console lists every exchange's scheme.
2. The http straggler's body carries the flag.

**Flag:** `DS{network_cleartext_L2_c95e07}`

**Fix:** Platform-level cleartext blocking turns stragglers into crashes.

## L3 🟠 — Policy Exception

**Vulnerable behavior:** The cleartext host is allowlisted in the network security config.

**Exploit:**
1. Read the config (jadx → res/xml), press the fetch against the 'allowed' domain.
2. The exception carves TLS out for exactly the domain carrying secrets.

**Flag:** `DS{network_cleartext_L3_2fa836}`

**Fix:** No cleartext exceptions — configuration-as-allowlist is configuration-as-vulnerability.

## L4 🔴 — Downgrade

**Vulnerable behavior:** The app 'detects' proxies and falls back to HTTP 'for reliability'.

**Exploit:**
1. Set the emulator proxy, trigger the flow (bundled tool automates).
2. The fallback downgrade sends the secret in cleartext — exactly to the proxy.

**Flag:** `DS{network_cleartext_L4_8b71d4}`

**Fix:** Fail closed: no TLS, no call.

## Tooling

- [`tools/intercept.sh`](tools/intercept.sh) — captures the app's exchanges (or drives the real backend) and greps for flags.

## Vulnerable vs hardened

```kotlin
// vulnerable
Request.Builder().url("http://trusted.siege.local/clear/L1")

// hardened
https:// + network_security_config with cleartextTrafficPermitted="false"
```
