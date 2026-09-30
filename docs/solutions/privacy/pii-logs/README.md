# privacy / pii-logs — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M6 Inadequate Privacy Controls · **References:** MASVS-STORAGE-3 · MASTG-TEST-0x84

## How this family works

The analytics stub logs PII (email, location, the recovery record) through Android logcat and its own rotating log file; the pseudonymizer is MD5 of a guessable id.

**Vulnerable code:** `challenges/privacy/PiiLogsFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Sync Log

**Vulnerable behavior:** track() logs the user's email verbatim.

**Exploit:**
1. `adb logcat -s SiegePii` while pressing the track action.
2. The email — and the recovery record — appear in the log.

**Flag:** `DS{crypto_ecbiv_L1_7a2e46}`

**Fix:** Log identifiers only; strip PII at the logging layer.

## L2 🟡 — Analytics Payload

**Vulnerable behavior:** The log line carries precise GPS coordinates.

**Exploit:**
1. Same capture; the coords are in the JSON payload.
2. Location is PII — logged in plaintext.

**Flag:** `DS{crypto_ecbiv_L2_d5f981}`

**Fix:** Coarsen or drop location; never log it.

## L3 🟠 — SDK Breadcrumb

**Vulnerable behavior:** The 'pseudonymized' user id is md5(email).

**Exploit:**
1. Take any known email, compute md5 (bundled tool), match the logged id.
2. Re-identification is one hash away.

**Flag:** `DS{crypto_ecbiv_L3_38c1b7}`

**Fix:** Random per-install opaque ids; salted, keyed hashes at minimum.

## L4 🔴 — Reversible Pseudonym

**Vulnerable behavior:** The flush() ships the whole log file — including the recovery record — to the 'endpoint'.

**Exploit:**
1. Trigger flush and read the loopback capture (or the in-app console echo).
2. The full PII file leaves the app.

**Flag:** `DS{crypto_ecbiv_L4_82ea6f}`

**Fix:** Data minimization at the source: the file should never contain PII.

## Tooling

- [`tools/pii_watch.sh`](tools/pii_watch.sh) — streams the PII log tag and decodes the pseudonymizer mappings.

## Vulnerable vs hardened

```kotlin
// vulnerable
Log.i("SiegePii", "track " + email + " @ " + lat + "," + lon)

// hardened
// Log.i("SiegePii", "event=screen_view id=" + opaqueId)
```
