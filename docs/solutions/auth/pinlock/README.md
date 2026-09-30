# auth / pinlock — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M1 Improper Credential Usage · **References:** MASVS-AUTH-1 · MASTG-TEST-0x40

## How this family works

The PIN lock protects a vault screen with escalating client-side weaknesses: comparison leaks, offline-verifiable digests, and a lock that unlocks itself.

**Vulnerable code:** `challenges/auth/PinLockFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Four Digits

**Vulnerable behavior:** The PIN is stored in plaintext prefs and compared client-side.

**Exploit:**
1. Read `shared_prefs/pin_prefs.xml` via run-as (or jadx the constant).
2. Enter the PIN — the vault opens with the flag.

**Flag:** `DS{auth_pinlock_L1_8f24b6}`

**Fix:** Server-side verification, or biometrics + Keystore-bound gate.

## L2 🟡 — Unsalted MD5

**Vulnerable behavior:** The PIN is SHA-256'd — 10k candidates brute-force instantly.

**Exploit:**
1. Pull the digest; run the bundled brute-forcer over 0000-9999.
2. The PIN (and flag) is recovered offline.

**Flag:** `DS{auth_pinlock_L2_1c79e0}`

**Fix:** Rate-limit + lockout; high-entropy factors; Keystore-gated gates.

## L3 🟠 — Boolean Biometrics

**Vulnerable behavior:** The unlock attempt count is stored client-side and resettable.

**Exploit:**
1. Hit the lockout, then `run-as` clear the counter key (bundled tool).
2. Unlimited attempts — brute-force with no consequence.

**Flag:** `DS{auth_pinlock_L3_9a53d1}`

**Fix:** Throttling server-side or in the keystore; never in clearable prefs.

## L4 🔴 — Recovery Fallback

**Vulnerable behavior:** The 'vault' checks a Possession-derived token the app itself can mint.

**Exploit:**
1. Replay the mint action the console exposes (insecure mode).
2. The client mints its own proof-of-possession — self-unlocking vault, flag included.

**Flag:** `DS{auth_pinlock_L4_be0847}`

**Fix:** Possession proofs need a server challenge; client-minted proofs are decoration.

## Tooling

- [`tools/forge_tokens.sh`](tools/forge_tokens.sh) — auth-family pack: brute-forces the PIN digest and resets the attempt counter.

## Vulnerable vs hardened

```kotlin
// vulnerable
if (sha256(input) == storedDigest) unlock()

// hardened
// server-side verification with rate limiting; or BiometricPrompt + Keystore gate
```
