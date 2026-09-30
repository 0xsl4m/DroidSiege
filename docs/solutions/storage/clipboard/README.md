# storage / clipboard — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M9 Insecure Data Storage · **References:** MASVS-PLATFORM-2 · MASTG-TEST-0x67

## How this family works

The copy button puts the recovery code on the system clipboard, where any app — or the user's next paste target — can read it.

**Vulnerable code:** `challenges/storage/ClipboardFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Copy Recovery Code

**Vulnerable behavior:** The secret is copied verbatim to the clipboard.

**Exploit:**
1. Press Copy in insecure mode, then `adb shell service call clipboard …` — or simply paste somewhere / read the in-app echo.
2. The clipboard is system-global: the value is exposed.

**Flag:** `DS{crypto_ecbiv_L1_7a2e46}`

**Fix:** Never place secrets on the clipboard; offer in-app reveal with auto-expiry instead.

## L2 🟡 — Coupon Field

**Vulnerable behavior:** The copy is 'guarded' by clearing after 30 seconds.

**Exploit:**
1. Copy, then read the clipboard within the window (any background app can).
2. A window is not a lock — the exposure is the design.

**Flag:** `DS{crypto_ecbiv_L2_d5f981}`

**Fix:** No secret copies; or FLAG_SECURE-style sensitive views + immediate clear on background.

## L3 🟠 — Clipboard History

**Vulnerable behavior:** The copy label previews the first half of the secret in the toast.

**Exploit:**
1. Press Copy and read the toast / the clipboard preview in the challenge console.
2. Half a secret in a system surface is half published.

**Flag:** `DS{crypto_ecbiv_L3_38c1b7}`

**Fix:** UI surfaces (toasts, previews, recents) must never carry secret fragments.

## L4 🔴 — Keyboard Cache

**Vulnerable behavior:** The paste-audit feature logs clipboard reads — including the secret it just copied.

**Exploit:**
1. Copy, paste, then read the audit log via run-as.
2. The audit trail captured the value it was auditing.

**Flag:** `DS{crypto_ecbiv_L4_82ea6f}`

**Fix:** Audit logs record events, never payloads.

## Tooling

- [`tools/clipboard_watch.sh`](tools/clipboard_watch.sh) — watches the clipboard around the copy action and dumps the audit log.

## Vulnerable vs hardened

```kotlin
// vulnerable
clipboard.setPrimaryClip(ClipData.newPlainText("code", FLAG))

// hardened
// no clipboard writes for secrets; reveal-on-screen with blur + expiry
```
