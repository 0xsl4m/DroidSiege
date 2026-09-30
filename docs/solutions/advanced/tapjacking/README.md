# advanced / tapjacking — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M8+ Advanced Attacks · **References:** MASVS-PLATFORM-1 · MASTG-TEST-0x65

## How this family works

The confirm button does not filter obscured touches: an overlay can ride the user's tap.

**Vulnerable code:** `challenges/advanced/AdvancedFamily.kt (TapjackingL1)` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Obscured Tap

**Vulnerable behavior:** The button accepts taps through an overlay.

**Exploit:**
1. With a bubble/overlay drawn (or the challenge's simulated overlay), press Confirm.
2. The tap fires through the overlay — the flag reveal prints.

**Flag:** `DS{advanced_tapjacking_L1_8c04d6}`

**Fix:** filterTouchesWhenObscured=true on sensitive views.

## Tooling

- [`tools/tapjack_overlay.md`](tools/tapjack_overlay.md) — overlay PoC recipe (TYPE_APPLICATION_OVERLAY window) + the adb-only variant.

## Vulnerable vs hardened

```kotlin
// vulnerable
// no obscured-touch filtering

// hardened
view.filterTouchesWhenObscured = true
```
