# storage / sqlite — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M9 Insecure Data Storage · **References:** MASVS-STORAGE-1 · MASTG-TEST-0x50

## How this family works

The sessions feature keeps a Room/SQLite database in the app's private dir. L1 stores tokens in plaintext columns; L2/L3 add encryption whose key is retrievable; L4 shows WAL sidecars leaking deleted rows.

**Vulnerable code:** `challenges/storage/SqliteFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Notes Vault

**Vulnerable behavior:** Session tokens sit in a plaintext table.

**Exploit:**
1. Trigger a session save in insecure mode.
2. `adb shell run-as com.droidsiege sqlite3 databases/siege.db 'select * from sessions;'`.
3. The token column holds the flag.

**Flag:** `DS{crypto_ecbiv_L1_7a2e46}`

**Fix:** Encrypt sensitive columns with a Keystore key, or keep the data server-side.

## L2 🟡 — Ops Cache

**Vulnerable behavior:** The 'secret' column is XOR-obfuscated with a static key.

**Exploit:**
1. Dump the row as above; the payload is base64 of an XOR blob.
2. The bundled sqlite tool re-derives the XOR key from the app constant and decodes.
3. Static-key XOR is decodable by anyone with the APK.

**Flag:** `DS{crypto_ecbiv_L2_d5f981}`

**Fix:** Authenticated encryption (AES/GCM) with a non-exportable key.

## L3 🟠 — Sealed Recovery

**Vulnerable behavior:** The DB key lives in a SharedPreferences 'vault' next to the database.

**Exploit:**
1. Dump both the prefs file and the DB.
2. Read the key from prefs, decrypt the column with the bundled tool.
3. Co-located key + ciphertext is one `run-as` away from plaintext.

**Flag:** `DS{crypto_ecbiv_L3_38c1b7}`

**Fix:** AndroidKeyStore-generated key; never persist the key beside the data it protects.

## L4 🔴 — Warp Vault

**Vulnerable behavior:** Deleted rows survive in the `-wal` file.

**Exploit:**
1. Save a secret, delete it in the app.
2. Pull `databases/siege.db-wal` and strings/grep it (or use the bundled tool).
3. The 'deleted' flag value is still in the write-ahead log.

**Flag:** `DS{crypto_ecbiv_L4_82ea6f}`

**Fix:** Use `PRAGMA wal_checkpoint(TRUNCATE)` / secure deletion, or never write secrets at all.

## Tooling

- [`tools/pull_dbs.sh`](tools/pull_dbs.sh) — pulls both databases and the WAL sidecar; greps deleted rows.
- [`tools/decrypt_sealed.py`](tools/decrypt_sealed.py) — re-derives the L2/L3 column keys and decrypts the stored blobs.

## Vulnerable vs hardened

```kotlin
// vulnerable
db.insert(Session(token = FLAG))

// hardened
// encrypted columns with a Keystore key — or store only a server-opaque handle
```
