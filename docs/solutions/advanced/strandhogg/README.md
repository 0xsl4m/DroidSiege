# advanced / strandhogg — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M8+ Advanced Attacks · **References:** MASVS-PLATFORM-1 · MASTG-TEST-0x65

## How this family works

The recovery activity keeps default taskAffinity and allows reparenting — the classic StrandHogg setup where an attacker task pulls the victim activity in and overlays it.

**Vulnerable code:** `challenges/advanced/AdvancedFamily.kt (StrandhoggL1) + AndroidManifest.xml` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Task Affinity

**Vulnerable behavior:** No taskAffinity, allowTaskReparenting — an attacker task can reparent and overlay.

**Exploit:**
1. Press 'Open recovery screen' (insecure mode): the simulated attacker task reparents and overlays the screen, harvesting the recovery entry.
2. A companion PoC app (see Phase 5 notes) does the real-world version from a second app.

**Flag:** `DS{advanced_strandhogg_L1_b37f92}`

**Fix:** taskAffinity="" + launchMode=singleTask + allowTaskReparenting=false.

## Tooling

- [`tools/strandhogg_poc.md`](tools/strandhogg_poc.md) — the companion attacker-app PoC recipe (task shape + overlay flow).

## Vulnerable vs hardened

```kotlin
// vulnerable
<activity android:allowTaskReparenting="true" />

// hardened
android:taskAffinity="" android:allowTaskReparenting="false"
```
