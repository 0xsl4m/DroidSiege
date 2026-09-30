# network / pinning — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M5 Insecure Communication · **References:** MASVS-NETWORK-2 · MASTG-TEST-0x32

## How this family works

The pinned client mis-implements pinning in escalating ways: pin-to-cert (breaks on rotation), pin-to-wrong-host, pin set built from a request header, and a bypass knob.

**Vulnerable code:** `challenges/network/PinningFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — No Pin

**Vulnerable behavior:** The pin is the leaf certificate — any valid cert for the same CA fails open at rotation, and the demo pins to a cert the MITM also has.

**Exploit:**
1. Press the pinned call (insecure mode) with the MITM running.
2. The pin 'matched' the attacker's cert — the console shows the interception.

**Flag:** `DS{network_pinning_L1_e4c192}`

**Fix:** Pin the SubjectPublicKeyInfo of the intermediate/root — not the leaf cert.

## L2 🟡 — User CA Trust

**Vulnerable behavior:** The pin is checked against the URL host string the caller passes in.

**Exploit:**
1. Call with a look-alike host (bundled tool) — the pin 'verifies'.
2. Host-controlled pinning verifies nothing.

**Flag:** `DS{network_pinning_L2_570ad8}`

**Fix:** Pins bind to the server identity, not to caller-supplied strings.

## L3 🟠 — Wrong Pin

**Vulnerable behavior:** The pin set is loaded from a response header on first connect ('server tells us what to pin').

**Exploit:**
1. Serve the pin header from the MITM (bundled script does).
2. The client re-pins to the attacker's key — TOFU self-destruct.

**Flag:** `DS{network_pinning_L3_b26f45}`

**Fix:** Pins ship with the app; the server never dictates them.

## L4 🔴 — Custom Trust

**Vulnerable behavior:** A debug switch (Settings) disables pin validation 'for testing'.

**Exploit:**
1. Flip the debug switch, run the MITM.
2. The 'pinned' client accepts anything — a kill switch the attacker can also reach.

**Flag:** `DS{network_pinning_L4_93d7e0}`

**Fix:** No runtime off-switches for certificate validation; use build variants.

## Tooling

- [`tools/intercept.sh`](tools/intercept.sh) — runs each pinning tier against the attacker endpoint and prints the outcomes.

## Vulnerable vs hardened

```kotlin
// vulnerable
builder.hostnameVerifier { _, _ -> true } // "pinning" via caller-supplied host

// hardened
CertificatePinner on the SPKI, shipped in the APK, no runtime override
```
