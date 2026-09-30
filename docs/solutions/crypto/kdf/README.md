# crypto / kdf — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M10 Insufficient Cryptography · **References:** MASVS-CRYPTO-2 · MASTG-TEST-0x50

## How this family works

The unlock feature derives its key from the user PIN with salted hash iterations — but the salt is printed and the iteration count is embarrassingly low.

**Vulnerable code:** `challenges/crypto/KdfFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — MD5 Master

**Vulnerable behavior:** Key = SHA-256(PIN): a 4-digit space is brute-forced instantly.

**Exploit:**
1. Capture the on-screen verifier blob; run the bundled brute-forcer over 0000-9999.
2. The PIN (and therefore the key) falls out in seconds.

**Flag:** `DS{crypto_kdf_L1_93e60c}`

**Fix:** High-entropy secrets or PBKDF2/Argon2 with real parameters; rate-limit attempts.

## L2 🟡 — Hundred Rounds

**Vulnerable behavior:** PBKDF2 with 1000 iterations — GPU-irrelevant, CPU-trivial.

**Exploit:**
1. Extract salt + verifier from the challenge console.
2. The bundled tool runs the dictionary with the documented parameters.

**Flag:** `DS{crypto_kdf_L2_48a2f5}`

**Fix:** ≥ hundreds of thousands of iterations (or memory-hard Argon2), plus attempt throttling.

## L3 🟠 — Guessable Salt

**Vulnerable behavior:** The salt is the username — deterministic across re-install.

**Exploit:**
1. The console prints the salt (the username).
2. Precompute/rainbow-table the small PIN space with the known salt (bundled tool).
3. Guessable salts allow precomputation.

**Flag:** `DS{crypto_kdf_L3_f1c74b}`

**Fix:** Random per-install salt from SecureRandom.

## L4 🔴 — Convenience Cache

**Vulnerable behavior:** Iterations are tunable client-side — the verifier accepts a 'fast' derivation.

**Exploit:**
1. Send the tampered parameter set the challenge exposes (the console shows the JSON).
2. The verifier accepts 1-iteration derivation — the bundled tool forges a matching verifier.
3. Crypto parameters chosen by the caller are chosen by the attacker.

**Flag:** `DS{crypto_kdf_L4_5d09e3}`

**Fix:** Server- or keystore-pinned parameters; client input never tunes KDF cost.

## Tooling

- [`tools/brute_kdf.py`](tools/brute_kdf.py) — runs the bundled weak-password dictionary against each tier's verifier parameters.
- [`tools/weak_passwords.txt`](tools/weak_passwords.txt) — the PIN/password dictionary the brute-forcer uses.

## Vulnerable vs hardened

```kotlin
// vulnerable
PBKDF2WithHmacSHA256(pin, salt, 1000, 256)

// hardened
Argon2id/PBKDF2 with ≥600k iterations, random salt, server-side attempt limits
```
