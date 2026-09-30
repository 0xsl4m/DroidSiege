# network / urlleak — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M5 Insecure Communication · **References:** MASVS-NETWORK-1 · MASTG-TEST-0x56

## How this family works

The referral feature puts the session token in the URL — query strings land in logs, Referer headers and browser history.

**Vulnerable code:** `challenges/network/UrlLeakFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Token in Query

**Vulnerable behavior:** The token is appended as `?token=`.

**Exploit:**
1. Press the share action; the console prints the full request line.
2. The flag rides the URL.

**Flag:** `DS{network_urlleak_L1_52e9af}`

**Fix:** Secrets travel in headers/bodies over TLS.

## L2 🟡 — Referer Trail

**Vulnerable behavior:** The token is base64'd 'for safety' but still in the URL.

**Exploit:**
1. Capture the request; decode the parameter (bundled tool).
2. Encoding in a URL is still publishing.

**Flag:** `DS{network_urlleak_L2_d7b310}`

**Fix:** Same as L1 — remove from the URL.

## L3 🟠 — WebView History

**Vulnerable behavior:** The URL is 'sent only over TLS' but logged client-side first.

**Exploit:**
1. Trigger the flow; read the app's own http-log tag via logcat.
2. The full URL (token included) is in the log.

**Flag:** `DS{network_urlleak_L3_84c26e}`

**Fix:** Never log URLs carrying credentials; redact at the logging layer.

## L4 🔴 — Analytics Trail

**Vulnerable behavior:** The leak chain: token in URL → Referer forwarded to a third-party pixel.

**Exploit:**
1. Serve the pixel endpoint with the bundled script; fire the flow.
2. The pixel's Referer header carries the token — cross-origin leak complete.

**Flag:** `DS{network_urlleak_L4_1a5f97}`

**Fix:** Strip referrers (`meta name=referrer`/policy), and again: no secrets in URLs.

## Tooling

- [`tools/intercept.sh`](tools/intercept.sh) — hosts the pixel endpoint and captures the request line + Referer chain.

## Vulnerable vs hardened

```kotlin
// vulnerable
"https://api.siege.local/share?token=" + token

// hardened
// token in an Authorization header; URL carries only an opaque share id
```
