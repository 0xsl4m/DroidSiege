# privacy / perms — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M6 Inadequate Privacy Controls · **References:** MASVS-PLATFORM-4 · MASTG-TEST-0x21

## How this family works

The media picker requests broad permissions up front and uses legacy storage flags; tiers escalate from blanket READ_EXTERNAL_STORAGE to pretending an in-app grant is a system grant.

**Vulnerable code:** `AndroidManifest.xml + challenges/privacy/PermsFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Over-Collection

**Vulnerable behavior:** The app requests READ_EXTERNAL_STORAGE for a photo-picker feature.

**Exploit:**
1. Open the challenge; the request dialog demands broad media access.
2. `adb shell pm grant com.droidsiege android.permission.READ_EXTERNAL_STORAGE` (or accept) — then the console shows everything the app can now read.

**Flag:** `DS{privacy_perms_L1_5a3d98}`

**Fix:** Photo Picker / READ_MEDIA_IMAGES scoped to the use case.

## L2 🟡 — Scoped Cache

**Vulnerable behavior:** requestLegacyExternalStorage=true keeps the app on the full-file-model.

**Exploit:**
1. Check the manifest flag (jadx → apktool).
2. The app bypasses scoped storage entirely on the configured target.

**Flag:** `DS{privacy_perms_L2_82e6c1}`

**Fix:** Scoped storage; no legacy flags.

## L3 🟠 — Background Poll

**Vulnerable behavior:** The app reads media not just for the picker but enumerates everything it can.

**Exploit:**
1. Grant, then run the bundled enumeration against the app's query.
2. The console lists far more than the picker needs — purpose creep.

**Flag:** `DS{privacy_perms_L3_40b7f2}`

**Fix:** Least privilege: request at use-time, use only what was picked.

## L4 🔴 — Shared Identity

**Vulnerable behavior:** The 'granted' state is stored in prefs and consulted instead of the system check.

**Exploit:**
1. Flip the prefs bit with the bundled script — no system grant at all.
2. The feature proceeds: client-side 'permission' state is decoration.

**Flag:** `DS{privacy_perms_L4_69c3ae}`

**Fix:** CheckContextSelfPermissions / Runtime permission APIs — never prefs.

## Tooling

- [`tools/perms_probe.sh`](tools/perms_probe.sh) — grants, enumerates and flips the permission-state prefs for all tiers.

## Vulnerable vs hardened

```kotlin
// vulnerable
requestLegacyExternalStorage = true + blanket READ

// hardened
Photo Picker (ACTION_PICK_IMAGES) — no runtime permission needed
```
