# webview / fileaccess — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M4 Insufficient Input/Output Validation · **References:** MASVS-PLATFORM-5 · MASTG-TEST-0x26

## How this family works

The WebView loads `file://` URLs with access enabled, so a page can read app-private files via XHR/fetch; tiers escalate to the content provider escrow.

**Vulnerable code:** `challenges/webview/WebViewKit.kt (settings) + webview assets` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Universal File Access

**Vulnerable behavior:** allowFileAccess=true and the page fetches a `file:///` path.

**Exploit:**
1. Load the challenge page; it XHRs the planted file.
2. The flag is in the fetched content.

**Flag:** `DS{webview_fileaccess_L1_e94d07}`

**Fix:** allowFileAccess=false; assets via WebViewAssetLoader.

## L2 🟡 — Unvalidated Load

**Vulnerable behavior:** allowFileAccessFromFileURLs lets the page read ANY app file.

**Exploit:**
1. The bundled payload page reads shared_prefs via XHR.
2. The flag from the prefs file renders in the WebView.

**Flag:** `DS{webview_fileaccess_L2_51c8a2}`

**Fix:** The three file-access settings all false; never load file:// content.

## L3 🟠 — Provider Traversal Read

**Vulnerable behavior:** The escrow file is planted under a 'hidden' path and the page reads it.

**Exploit:**
1. Press the plant action, then load the reader page (bundled).
2. fetch('file:///data/data/com.droidsiege/files/wv/wv_provider_escrow.txt') returns it.

**Flag:** `DS{webview_fileaccess_L3_68b3f5}`

**Fix:** Hidden paths are not access control — the setting is.

## L4 🔴 — Full Chain

**Vulnerable behavior:** The provider-backed escrow serves through content:// with a traversal-prone path.

**Exploit:**
1. Load the bundled page that XHRs the crafted content:// URI.
2. The provider resolves it and the WebView displays the L4 escrow.

**Flag:** `DS{webview_fileaccess_L4_cd2719}`

**Fix:** Same: file/content access off; provider paths canonicalized.

## Tooling

- [`tools/webview_drives.sh`](tools/webview_drives.sh) — drives the file-access tiers: XHRs file:// and content:// targets.

## Vulnerable vs hardened

```kotlin
// vulnerable
settings.allowFileAccessFromFileURLs = true

// hardened
// all file-access settings false; WebViewAssetLoader for local content
```
