# components / deeplink — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M8 Security Misconfiguration · **References:** MASVS-PLATFORM-3 · MASTG-TEST-0x28

## How this family works

The recovery screen exposes `droidsiege://` deep links whose parameters flow into display, WebView URL and local logic with no validation.

**Vulnerable code:** `AndroidManifest.xml (intent filters) + challenges/components/DeepLinkLab.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Recover Link

**Vulnerable behavior:** The recovery deep link echoes its `code` parameter on the secret screen.

**Exploit:**
1. `adb shell am start -a android.intent.action.VIEW -d "droidsiege://recover?code=x"`.
2. The screen resolves the param; the bundled link with the right shape reveals the flag.

**Flag:** `DS{components_deeplink_L1_9c52fa}`

**Fix:** Treat deep-link params as untrusted input; never echo secrets through them.

## L2 🟡 — Account Parameter

**Vulnerable behavior:** A second link opens a WebView with an attacker-controlled URL parameter.

**Exploit:**
1. `… -d "droidsiege://web?url=https://attacker.example"` (bundled tool).
2. The in-app WebView loads the attacker page inside the trusted UI.

**Flag:** `DS{components_deeplink_L2_48e7b1}`

**Fix:** Allowlist hosts for WebView navigation from deep links.

## L3 🟠 — Unverified Host

**Vulnerable behavior:** The link's `next` parameter redirects the user flow after 'login'.

**Exploit:**
1. Fire the crafted link (bundled tool); the app follows `next` blindly.
2. The flow ends on an attacker-chosen internal screen with the flag in reach.

**Flag:** `DS{components_deeplink_L3_f06d93}`

**Fix:** Open-only redirect allowlist; ignore unvalidated targets.

## L4 🔴 — Reader Injection

**Vulnerable behavior:** Chaining: link → WebView → JS bridge — the URL smuggles a bridge call.

**Exploit:**
1. Serve the payload page from a local http server (bundled tool) and fire the link.
2. The loaded page invokes the JS bridge whose recovery code is the flag.

**Flag:** `DS{components_deeplink_L4_37ac28}`

**Fix:** Validate the full chain: hosts, bridges (@JavascriptInterface exposure), and per-bridge secrets.

## Tooling

- [`tools/fire_links.sh`](tools/fire_links.sh) — fires each tier's crafted link and (for L4) hosts the payload page.

## Vulnerable vs hardened

```kotlin
// vulnerable
<data android:scheme="droidsiege" /> with no host/path validation

// hardened
validate scheme+host+params against an allowlist before use
```
