# advanced / customtabs — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M8+ Advanced Attacks · **References:** MASVS-NETWORK-1 · MASTG-TEST-0x56

## How this family works

The app opens its offers page in a Custom Tab with the recovery code appended to the URL — browser history, redirects and logging all see it.

**Vulnerable code:** `challenges/advanced/AdvancedFamily.kt (CustomTabsL1)` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Secret in Tab

**Vulnerable behavior:** The code rides the URL to the tab.

**Exploit:**
1. Press 'Open offers in a tab' (insecure mode).
2. The launched URL carries `?code=<flag>` — visible in history/logs; the console confirms.

**Flag:** `DS{advanced_customtabs_L1_25e8a1}`

**Fix:** No secrets in URLs; redeem via POST from your own screen.

## Tooling

- [`tools/tab_capture.md`](tools/tab_capture.md) — how to observe the tab URL (history provider, logcat, or a local redirect target).

## Vulnerable vs hardened

```kotlin
// vulnerable
tabsIntent.dataUri = Uri.parse("https://…?code=$code")

// hardened
// code redeemed server-side via POST; tab URL carries nothing
```
