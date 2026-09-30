# privacy / recents-priv — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M6 Inadequate Privacy Controls · **References:** MASVS-PLATFORM-2 · MASTG-TEST-0x63

## How this family works

The private-notes screen leaks through task-switcher snapshots and the app-switch preview — the same surface family as storage/screens but focused on privacy flows.

**Vulnerable code:** `challenges/privacy/RecentsPrivacyFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Recents Snapshot

**Vulnerable behavior:** The notes content is visible in the recents thumbnail.

**Exploit:**
1. Open the notes screen, press recents, screenshot the switcher.
2. The note (and flag) is on the thumbnail.

**Flag:** `DS{privacy_recents-priv_L1_b4f861}`

**Fix:** FLAG_SECURE on the screen, or blank the content in onPause.

## L2 🟡 — Lockscreen Notification

**Vulnerable behavior:** The screen sets FLAG_SECURE only after the first render.

**Exploit:**
1. Open and IMMEDIATELY screenshot (bundled script races it).
2. The first frame — unblurred — is captured.

**Flag:** `DS{privacy_recents-priv_L2_27a0c3}`

**Fix:** Set the flag in onCreate before setContentView.

## L3 🟠 — Autofill Exposure

**Vulnerable behavior:** The 'screenshot blocked' build still exposes content via the share preview.

**Exploit:**
1. Trigger share; pull the preview bitmap (bundled tool).
2. The share path renders without the protection.

**Flag:** `DS{privacy_recents-priv_L3_e815d4}`

**Fix:** All render paths respect the same protection.

## L4 🔴 — Accessibility Field

**Vulnerable behavior:** The 'incognito' mode still writes the note to the task description.

**Exploit:**
1. Read the task description via `dumpsys activity recents` (bundled).
2. The 'private' note is in the task metadata.

**Flag:** `DS{privacy_recents-priv_L4_93c2f7}`

**Fix:** Never place content in task descriptions; metadata leaks too.

## Tooling

- [`tools/recents_grab.sh`](tools/recents_grab.sh) — races the first frame, pulls thumbnails and dumps task descriptions.

## Vulnerable vs hardened

```kotlin
// vulnerable
// FLAG_SECURE set after first frame

// hardened
window.setFlags(FLAG_SECURE, FLAG_SECURE) in onCreate, before content
```
