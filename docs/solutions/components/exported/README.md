# components / exported — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M8 Security Misconfiguration · **References:** MASVS-PLATFORM-1 · MASTG-TEST-0x4/0x5

## How this family works

Three real components are exported in the manifest: an activity, a service and a receiver, plus a content provider. Any app on the device can start/bind/send at them with no permission gate.

**Vulnerable code:** `AndroidManifest.xml + challenges/components/ExportedComponents.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Recovery Portal

**Vulnerable behavior:** ExportedActivity reveals its secret to any caller.

**Exploit:**
1. `adb shell am start -n com.droidsiege/.challenges.components.ExportedActivity`.
2. The flag renders on the launched screen (and via the bundled tool).

**Flag:** `DS{components_exported_L1_5c9e14}`

**Fix:** `exported=false` unless the component must serve other apps — then gate with a signature permission.

## L2 🟡 — Keyed Export

**Vulnerable behavior:** ExportedService returns its binder result to any binding app.

**Exploit:**
1. Run the bundled script that binds the service and reads the reply (or use the in-app attacker card).
2. The reply carries the flag.

**Flag:** `DS{components_exported_L2_7d2f68}`

**Fix:** Same: no export, or permission + caller signature check (CallerGuard).

## L3 🟠 — Messenger Service

**Vulnerable behavior:** ExportedReceiver accepts the 'sync now' broadcast from anyone — and echoes its payload.

**Exploit:**
1. `adb shell am broadcast -a com.droidsiege.SYNC_NOW --es payload x`.
2. The receiver's acknowledgement leaks the secret into the broadcast reply.

**Flag:** `DS{components_exported_L3_94ba30}`

**Fix:** Exported receivers need permission protection and sender validation.

## L4 🔴 — Escrow Chain

**Vulnerable behavior:** The provider path accepts an extras-driven 'admin mode' that overrides the runtime gate.

**Exploit:**
1. Query the provider with the admin extras bundle the bundled tool constructs.
2. The gate consults caller-supplied extras — caller-controlled authorization.

**Flag:** `DS{components_exported_L4_c1e75b}`

**Fix:** Authorization from the platform (permissions/UID), never from intent contents.

## Tooling

- [`tools/drive_components.sh`](tools/drive_components.sh) — starts/binds/broadcasts/queries every exported component and prints the leaked flags.

## Vulnerable vs hardened

```kotlin
// vulnerable
<activity android:name=".ExportedActivity" android:exported="true" />

// hardened
android:exported="false" — or android:permission with a signature-level protection
```
