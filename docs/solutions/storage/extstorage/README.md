# storage / extstorage — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M9 Insecure Data Storage · **References:** MASVS-STORAGE-2 · MASTG-TEST-0x55

## How this family works

The export feature writes receipts to app-external storage (`getExternalFilesDir`), world-readable to anything with the same storage access, and survives the app.

**Vulnerable code:** `challenges/storage/ExtStorageFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Receipt Export

**Vulnerable behavior:** The receipt containing the secret is written to external storage as plain text.

**Exploit:**
1. Press the export action in insecure mode.
2. `adb shell cat /sdcard/Android/data/com.droidsiege/files/receipt.txt`.
3. The flag is in the file body.

**Flag:** `DS{storage_extstorage_L1_6c2e85}`

**Fix:** Keep exports inside internal storage, or encrypt before writing.

## L2 🟡 — Diagnostic Cache

**Vulnerable behavior:** The file is 'obfuscated' with a fixed byte rotation.

**Exploit:**
1. Export, pull the file, and apply the reverse rotation (bundled tool does it).
2. The rotation constant ships in the APK.

**Flag:** `DS{storage_extstorage_L2_f43a09}`

**Fix:** Real encryption before any external write — or no external writes.

## L3 🟠 — Transfer Share

**Vulnerable behavior:** The export appends the secret to a shared log file other components can read.

**Exploit:**
1. Export twice; read the appended tail of the shared file.
2. Any co-installed reader (or `adb shell cat`) sees the tail.

**Flag:** `DS{storage_extstorage_L3_91d6b3}`

**Fix:** App-private files only; shared storage gets encrypted, name-mangled data or nothing.

## L4 🔴 — Photowalk Metadata

**Vulnerable behavior:** The 'encrypted' export derives its key from the device IMEI via a printed formula.

**Exploit:**
1. Read the IMEI with `adb shell service call iphonesubinfo …` or the bundled tool.
2. The derivation (documented in the file header) turns it into the AES key.
3. Device identifiers are readable by the same attacker who reads the file.

**Flag:** `DS{storage_extstorage_L4_57ae20}`

**Fix:** Keys from AndroidKeyStore — bound to the keystore hardware, not to guessable identifiers.

## Tooling

- [`tools/collect_artifacts.sh`](tools/collect_artifacts.sh) — pulls every exported artifact and reverses each tier's encoding.

## Vulnerable vs hardened

```kotlin
// vulnerable
File(getExternalFilesDir(null), "receipt.txt").writeText(payload)

// hardened
// filesDir only, and encrypt with a Keystore key before writing
```
