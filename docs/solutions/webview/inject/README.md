# webview / inject — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M4 Insufficient Input/Output Validation · **References:** MASVS-CODE-2 · MASTG-TEST-0x55

## How this family works

A debug-only evaluateJavascript path feeds user text straight into the JS context — arbitrary script execution inside the app's WebView.

**Vulnerable code:** `challenges/webview/InjectLab.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Notes Search

**Vulnerable behavior:** The input is wrapped in a JS string literal — break out with a quote.

**Exploit:**
1. Enter `');<payload>//` shaped input from the hint.
2. The console prints the evaluated result — code execution.

**Flag:** `DS{webview_inject_L1_9e27b4}`

**Fix:** No evaluateJavascript on user input; postMessage bridges instead.

## L2 🟡 — Note File Open

**Vulnerable behavior:** The payload can call the bridge with the page's origin.

**Exploit:**
1. Evaluate `Siege.getRecoveryCode()` via the injection (hint shows the shape).
2. The bridge answers the injected call — trust boundary crossed.

**Flag:** `DS{webview_inject_L2_6c3f81}`

**Fix:** Same: remove the sink; bridge calls come from loaded pages only, with origin checks.

## L3 🟠 — Format Receipt

**Vulnerable behavior:** The input passes through a format-style template — %s placeholders in a JS template.

**Exploit:**
1. Craft input whose %s substitution closes the template (hint).
2. The substituted string injects the payload.

**Flag:** `DS{webview_inject_L3_4a98d2}`

**Fix:** Template engines don't sanitize; the sink is still the sink.

## L4 🔴 — Parser Over-read

**Vulnerable behavior:** Chained: injection → file read → bridge exfil.

**Exploit:**
1. Inject a fetch of the planted file, then a bridge call with its contents (bundled payload).
2. The console shows the file's flag leaving through the bridge.

**Flag:** `DS{webview_inject_L4_b8516e}`

**Fix:** Depth of chain doesn't matter — close the first sink.

## Tooling

- [`tools/webview_drives.sh`](tools/webview_drives.sh) — drives the injection tiers with a working payload each.

## Vulnerable vs hardened

```kotlin
// vulnerable
webView.evaluateJavascript("handle('" + input + "')", null)

// hardened
// remove the debug sink; use WebMessagePort if a channel is truly needed
```
