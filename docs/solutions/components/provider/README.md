# components / provider — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M8 Security Misconfiguration · **References:** MASVS-PLATFORM-1 · MASTG-TEST-0x9

## How this family works

The notes ContentProvider is exported without a read permission and builds its SQL from string concatenation; its openFile also accepts traversal-ish projections.

**Vulnerable code:** `challenges/components/ProviderLab.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Open Vault

**Vulnerable behavior:** Any app can query the notes table.

**Exploit:**
1. `adb shell content query --uri content://com.droidsiege.notes/notes`.
2. Rows include the secret column.

**Flag:** `DS{components_provider_L1_3f86d1}`

**Fix:** exported=false, or read permission + per-row authorization.

## L2 🟡 — Realm Gate

**Vulnerable behavior:** The selection string is concatenated — classic SQL injection.

**Exploit:**
1. `content query --uri … --where "title=' OR 1=1 --"` (bundled tool automates).
2. The injection dumps every row including secrets.

**Flag:** `DS{components_provider_L2_82c4a9}`

**Fix:** Parameterized selection args only.

## L3 🟠 — Injectable Lookup

**Vulnerable behavior:** A projection hack unions attacker-chosen columns.

**Exploit:**
1. Pass the union projection from the bundled tool.
2. The query result includes the secret column under an alias.

**Flag:** `DS{components_provider_L3_e07b52}`

**Fix:** Whitelist projections; never reflect caller-supplied column lists.

## L4 🔴 — File Traversal

**Vulnerable behavior:** openFile serves files by path segment — traverse it.

**Exploit:**
1. `content read --uri content://com.droidsiege.notes/file/…/shared_prefs/siege_wallet_prefs.xml` (bundled tool).
2. Path traversal in the uri segment escapes the notes dir and reads app-private files.

**Flag:** `DS{components_provider_L4_46d9f8}`

**Fix:** Canonicalize + restrict served paths to a dedicated subdir; reject `..` and absolute escapes.

## Tooling

- [`tools/query_provider.sh`](tools/query_provider.sh) — queries/injects/traverses the provider for all four tiers.

## Vulnerable vs hardened

```kotlin
// vulnerable
"SELECT * FROM notes WHERE title = '" + selection + "'"

// hardened
"SELECT * FROM notes WHERE title = ?" with selectionArgs
```
