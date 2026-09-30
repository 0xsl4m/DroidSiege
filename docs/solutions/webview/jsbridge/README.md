# webview / jsbridge — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M4 Insufficient Input/Output Validation · **References:** MASVS-PLATFORM-5 · MASTG-TEST-0x65

## How this family works

The WebView exposes a JS bridge via addJavascriptInterface. Any page loaded in that WebView — trusted or not — can call it; each tier mounts different bridge secrets.

**Vulnerable code:** `challenges/webview/WebViewKit.kt (BridgeSurface)` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Open Bridge

**Vulnerable behavior:** The recovery bridge returns its code to any caller.

**Exploit:**
1. Load the bridge-probe page (bundled payload) inside the challenge WebView.
2. `window.Siege.getRecoveryCode()` returns the flag.

**Flag:** `DS{webview_jsbridge_L1_a3f8c1}`

**Fix:** Bridge methods must authenticate the calling origin and expose no secrets.

## L2 🟡 — Token Gate

**Vulnerable behavior:** The premium bridge is gated by a JS-supplied 'token'.

**Exploit:**
1. Call `getPremiumCode("<token-from-hint>")` from the probe page.
2. Client-supplied gate — the probe passes it verbatim.

**Flag:** `DS{webview_jsbridge_L2_7e29b4}`

**Fix:** Authorization in native code from its own state, never from JS arguments.

## L3 🟠 — Chained Bridge

**Vulnerable behavior:** Two bridges exist; the page can reach the other tier's secret by calling both.

**Exploit:**
1. Probe both bridge objects from one page (bundled payload does).
2. The page exfiltrates whichever bridge holds a secret.

**Flag:** `DS{webview_jsbridge_L3_d65018}`

**Fix:** Per-WebView bridge scoping: mount only the bridge the page needs.

## L4 🔴 — Privileged Bridge

**Vulnerable behavior:** The report bridge renders its argument back into the page (bridge→DOM sink).

**Exploit:**
1. Call report with the payload from the bundled page.
2. The argument lands in innerHTML — XSS via the bridge, crossing the trust boundary.

**Flag:** `DS{webview_jsbridge_L4_2b8c73}`

**Fix:** Bridge args are untrusted input: escape before any DOM use.

## Tooling

- [`tools/webview_drives.sh`](tools/webview_drives.sh) — drives the bridge probes: enumerates bridges and pulls every mounted secret.

## Vulnerable vs hardened

```kotlin
// vulnerable
addJavascriptInterface(BridgeSurface(secret), "Siege")

// hardened
// per-WebView bridges; native-side origin checks; no secrets across the boundary
```
