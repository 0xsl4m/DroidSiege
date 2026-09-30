# storage / prefs — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M9 Insecure Data Storage · **References:** MASVS-STORAGE-1 · MASTG-TEST-0x50

## How this family works

The wallet feature persists session artifacts into `siege_wallet_prefs.xml`. L1 writes the recovery code as plaintext; L2 'protects' it with base64; L3/L4 escalate the key handling around an AES-GCM blob.

**Vulnerable code:** `challenges/storage/PrefsFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Remember Me

**Vulnerable behavior:** The recovery code is written verbatim into the prefs XML.

**Exploit:**
1. Open the challenge and press the persist action (insecure mode).
2. Dump the file: `adb shell run-as com.droidsiege cat shared_prefs/siege_wallet_prefs.xml`.
3. The flag is the plaintext `<string>` value.

**Flag:** `DS{crypto_ecbiv_L1_7a2e46}`

**Fix:** Never store secrets in prefs — keep server-side, or use the Keystore-backed EncryptedSharedPreferences / DataStore.

## L2 🟡 — Encoded Session

**Vulnerable behavior:** The same value is stored base64-encoded and called 'encrypted'.

**Exploit:**
1. Persist in insecure mode, dump the prefs file as above.
2. `echo <value> | base64 -d` (or the bundled decrypt_prefs.py helper decodes it).
3. Encoding is not encryption — the flag falls straight out.

**Flag:** `DS{crypto_ecbiv_L2_d5f981}`

**Fix:** Real authenticated encryption with a non-exportable Keystore key, or no secret on the device at all.

## L3 🟠 — Release Key

**Vulnerable behavior:** An AES-GCM blob is stored, but the key is a hardcoded string in the APK.

**Exploit:**
1. Pull the key with `jadx` (search for the wallet key constant).
2. Decrypt the stored blob with the bundled decrypt_prefs.py (it mirrors the app derivation).
3. GCM without confidentiality of the key is just obfuscation.

**Flag:** `DS{crypto_ecbiv_L3_38c1b7}`

**Fix:** Generate the key inside AndroidKeyStore (`setRandomizedEncryptionRequired(true)`) so it never exists in code.

## L4 🔴 — Sealed Vault

**Vulnerable behavior:** The key is split into two halves in different places — but both halves ship with the app.

**Exploit:**
1. Recover part A from BuildConfig (`WALLET_KEY_PART_A`) and part B from the prefs file.
2. Concatenate and decrypt the GCM blob with decrypt_prefs.py.
3. Splitting a secret across client locations is storage, not protection.

**Flag:** `DS{crypto_ecbiv_L4_82ea6f}`

**Fix:** Server-held escrow: the client should never possess every share.

## Tooling

- [`tools/dump_prefs.sh`](tools/dump_prefs.sh) — dumps the prefs file and decodes the L2 base64 artifact.
- [`tools/decrypt_prefs.py`](tools/decrypt_prefs.py) — rebuilds the L3/L4 keys from public parts and decrypts the AES-GCM blob.

## Vulnerable vs hardened

```kotlin
// vulnerable
prefs.edit().putString("recovery", FLAG).apply()

// hardened
// EncryptedSharedPreferences or server-side storage; no secret plaintext on disk
```
