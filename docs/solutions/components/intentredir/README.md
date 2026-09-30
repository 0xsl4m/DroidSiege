# components / intentredir — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M8 Security Misconfiguration · **References:** MASVS-PLATFORM-1 · MASTG-TEST-0x7

## How this family works

The proxy activity forwards attacker extras to privileged internal components (intent redirection), and a PendingIntent is built mutable & implicit.

**Vulnerable code:** `challenges/components/IntentRedirLab.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Blind Forward

**Vulnerable behavior:** ProxyActivity launches whatever component the extras name.

**Exploit:**
1. `adb shell am start -n …/.ProxyActivity --es component <internal activity> --es extra …` (bundled tool).
2. The internal component runs with the proxy's identity and leaks its payload.

**Flag:** `DS{components_intentredir_L1_b3d670}`

**Fix:** Never reflect extras into startActivity; allowlist explicit targets.

## L2 🟡 — Token Courier

**Vulnerable behavior:** The redirect accepts nested Intent extras and forwards them verbatim.

**Exploit:**
1. Build the nested-intent bundle (bundled tool) and start the proxy.
2. The nested intent (with its extras) executes inside the app.

**Flag:** `DS{components_intentredir_L2_25f8c4}`

**Fix:** Extract only validated primitives from extras; strip Intent-typed extras.

## L3 🟠 — Mutable Pending

**Vulnerable behavior:** The PendingIntent is mutable + implicit: anyone can fill it.

**Exploit:**
1. Run the bundled hijack that fills the mutable PendingIntent with an attacker intent.
2. The app fires the hijacked intent with its own privileges.

**Flag:** `DS{components_intentredir_L3_71a3e9}`

**Fix:** IMMUTABLE PendingIntents + explicit component.

## L4 🔴 — Confused Deputy

**Vulnerable behavior:** The result-receiver flow grants its callback to whatever component the caller supplies.

**Exploit:**
1. Supply the attacker receiver component in the extras (bundled tool).
2. The privileged result (with the flag) is delivered to the attacker component.

**Flag:** `DS{components_intentredir_L4_d84c16}`

**Fix:** Bind result delivery to fixed components; validate receiver identity.

## Tooling

- [`tools/drive_proxies.sh`](tools/drive_proxies.sh) — drives the proxy with nested intents, hijacks the mutable PendingIntent, steals the result.

## Vulnerable vs hardened

```kotlin
// vulnerable
PendingIntent.getActivity(ctx, 0, implicit, FLAG_MUTABLE)

// hardened
PendingIntent.getActivity(ctx, 0, explicit, FLAG_IMMUTABLE)
```
