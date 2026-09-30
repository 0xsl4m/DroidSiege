# storage / backup — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M9 Insecure Data Storage · **References:** MASVS-STORAGE-1 · MASTG-TEST-0x58

## How this family works

The manifest allows backup and ships sweeping backup rules, so `adb backup` (and cloud restore) exfiltrates every app-private artifact.

**Vulnerable code:** `AndroidManifest.xml + challenges/storage/BackupFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Backed Up Account

**Vulnerable behavior:** `android:allowBackup=true` with default rules — everything is backed up.

**Exploit:**
1. `adb backup -noapk com.droidsiege` (or the bundled script), then `dd bs=24 skip=1 | tar xvf -`.
2. The prefs/DB files in the archive carry the flag.

**Flag:** `DS{storage_backup_L1_8c45f2}`

**Fix:** allowBackup=false, or rules that exclude secret files.

## L2 🟡 — Support Escalation

**Vulnerable behavior:** Backup rules exclude the 'obvious' file but not the real artifact.

**Exploit:**
1. Run the backup and inspect the extracted tree.
2. The excluded path was a decoy; the real secret file is included.

**Flag:** `DS{storage_backup_L2_10e7b6}`

**Fix:** Deny-by-default backup rules; enumerate every file the app writes.

## L3 🟠 — Cloud Rules

**Vulnerable behavior:** The device-transfer (D2D) path exports a 'settings bundle' containing the secret.

**Exploit:**
1. Trigger the export-activity in the challenge, pull the bundle.
2. Transfer flows bypass the classic backup exclusion lists.

**Flag:** `DS{storage_backup_L3_d29a3f}`

**Fix:** Exclude transfer bundles too; treat every export path as a backup path.

## L4 🔴 — Recovery Key

**Vulnerable behavior:** The restored-from-backup flow re-mints the secret with a key stored… in the backup.

**Exploit:**
1. Extract the archive; read the wrapped key stored beside the data.
2. Unwrap and decrypt — the restore path is self-contained for the attacker.

**Flag:** `DS{storage_backup_L4_47c610}`

**Fix:** Wrap restore data with a Keystore key that never leaves the device (and is not upgradable).

## Tooling

- [`tools/run_backup.sh`](tools/run_backup.sh) — creates the adb backup archive and unpacks it.
- [`tools/decrypt_backup_bundle.py`](tools/decrypt_backup_bundle.py) — greps the archive and decrypts the transfer bundle for every tier.

## Vulnerable vs hardened

```kotlin
// vulnerable
android:allowBackup="true"

// hardened
android:allowBackup="false" (or dataExtractionRules excluding secrets)
```
