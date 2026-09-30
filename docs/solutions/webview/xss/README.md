# webview / xss — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M4 Insufficient Input/Output Validation · **References:** MASVS-PLATFORM-5 · MASTG-TEST-0x25

## How this family works

The offers screen renders user-controlled strings into a WebView with JavaScript enabled. Four tiers escalate from reflected script injection to DOM-based sinks.

**Vulnerable code:** `challenges/webview/XssFamily.kt + WebViewPages.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Reflected Note

**Vulnerable behavior:** The comment field is interpolated into the HTML body unescaped.

**Exploit:**
1. Enter the payload from the hint (`<script>…</script>` shape) and load the page.
2. The script runs and exfiltrates the page's embedded secret to the console.

**Flag:** `DS{webview_xss_L1_f7a24e}`

**Fix:** Escape all interpolation (or build DOM via the WebView's APIs); CSP where possible.

## L2 🟡 — Stored Note

**Vulnerable behavior:** The title lands inside an attribute — break out of the quotes.

**Exploit:**
1. Use the `"><img src=x onerror=…>`-shaped payload from the hint.
2. The attribute breakout executes the handler.

**Flag:** `DS{webview_xss_L2_3d61b9}`

**Fix:** Context-aware escaping: attribute context needs quote-safe encoding.

## L3 🟠 — DOM Sink

**Vulnerable behavior:** The page uses innerHTML with a location.hash payload — DOM XSS with no server round-trip.

**Exploit:**
1. Open the crafted `#…` link (the hint shows the fragment payload).
2. The DOM sink evaluates the fragment.

**Flag:** `DS{webview_xss_L3_8c05e1}`

**Fix:** textContent over innerHTML; sanitize with DOMPurify-class libraries.

## L4 🔴 — XSS to Bridge

**Vulnerable behavior:** The 'sanitizer' strips `<script>` only — event handlers and pseudo-URLs survive.

**Exploit:**
1. Use the `<img onerror=…>` / `javascript:` payload from the hint.
2. The naive sanitizer passes it through and it executes.

**Flag:** `DS{webview_xss_L4_7192cd}`

**Fix:** Allowlist-based sanitization; denylists always miss a context.

## Tooling

- [`tools/webview_drives.sh`](tools/webview_drives.sh) — drives the WebView tiers with the working payload per tier.

## Vulnerable vs hardened

```kotlin
// vulnerable
"<body>" + userComment + "</body>"

// hardened
HtmlEncoder.escape(userComment) inside a template — or textContent
```
