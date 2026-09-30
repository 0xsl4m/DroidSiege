# auth / creds — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M1 Improper Credential Usage · **References:** MASVS-AUTH-1 · MASTG-TEST-0x52

## How this family works

Login credentials handled badly: plaintext storage, Base64 'encryption', a static transport 'hash', and a hard-coded backdoor account.

**Vulnerable code:** `challenges/auth/CredsFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Default Credentials

**Vulnerable behavior:** Username and password persist in plaintext prefs.

**Exploit:**
1. Log in once (insecure mode), dump `shared_prefs/creds_prefs.xml` via run-as.
2. Both values — and the flag as the stored secret — are readable.

**Flag:** `DS{auth_creds_L1_c4e8a2}`

**Fix:** Never store passwords; use tokens, and store those encrypted.

## L2 🟡 — Premium Key

**Vulnerable behavior:** The password is base64'd before storage.

**Exploit:**
1. Dump the prefs; `base64 -d` the field (bundled tool).
2. Encoding isn't hashing — the password and flag fall out.

**Flag:** `DS{auth_creds_L2_9b1f76}`

**Fix:** Store nothing reversible; salted password hashing is a server concern anyway.

## L3 🟠 — Obfuscated Creds

**Vulnerable behavior:** The 'hashed' password uses an unkeyed hash with a constant salt printed in the console.

**Exploit:**
1. Read salt + digest from prefs; run the dictionary attack (bundled tool).
2. The password (and flag) is recovered from the digest.

**Flag:** `DS{auth_creds_L3_e5d340}`

**Fix:** Argon2id/bcrypt with per-user random salts — on the server.

## L4 🔴 — Native Gate

**Vulnerable behavior:** A hard-coded support account accepts a magic password in release builds.

**Exploit:**
1. Find the constant with jadx (hint names the account).
2. Log in with it — the backdoor opens and yields the flag.

**Flag:** `DS{auth_creds_L4_27a8c9}`

**Fix:** No universal credentials; backdoors are findings, not features.

## Tooling

- [`tools/forge_tokens.sh`](tools/forge_tokens.sh) — auth-family pack: dumps the creds prefs and cracks the digests for every tier.

## Vulnerable vs hardened

```kotlin
// vulnerable
prefs.edit().putString("password", base64(pw)).apply()

// hardened
// no password storage; token-based auth with secure token handling
```
