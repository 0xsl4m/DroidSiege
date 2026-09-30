# crypto / homegrown — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M10 Insufficient Cryptography · **References:** MASVS-CRYPTO-1 · MASTG-TEST-0x48

## How this family works

The 'cipher' feature rolls its own stream cipher and checksum. Home constructions fail in ways textbook attacks exploit — here, a repeating keystream and a forgeable checksum.

**Vulnerable code:** `challenges/crypto/HomegrownFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Single Byte

**Vulnerable behavior:** The stream cipher XORs with a short repeating key.

**Exploit:**
1. Capture the ciphertext (the console shows the bytes).
2. Repeating-key XOR analysis (bundled tool) recovers the key from known plaintext structure.

**Flag:** `DS{crypto_ecbiv_L1_7a2e46}`

**Fix:** Use AES/GCM; never hand-roll ciphers.

## L2 🟡 — Repeating Key

**Vulnerable behavior:** The 'MAC' is a CRC32 of plaintext — linear and forgeable.

**Exploit:**
1. Flip ciphertext bytes; the CRC delta is computable without the key (bundled tool).
2. The verifier accepts the tampered message: malleability proven.

**Flag:** `DS{crypto_ecbiv_L2_d5f981}`

**Fix:** HMAC-SHA256 or GCM's built-in tag.

## L3 🟠 — Proprietary Rounds

**Vulnerable behavior:** Keystream restarts per message under the same key.

**Exploit:**
1. XOR two ciphertexts captured from the repeated action (bundled tool).
2. Keystream reuse leaks the plaintext XOR — both messages fall.

**Flag:** `DS{crypto_ecbiv_L3_38c1b7}`

**Fix:** Unique nonce per message (GCM handles this correctly).

## L4 🔴 — Predictable Stream

**Vulnerable behavior:** The custom checksum doubles as the 'authenticity' check for an encrypted command.

**Exploit:**
1. Forge the command format the console documents, compute its CRC (bundled tool).
2. The app accepts the forged command and prints the flag.
3. Unauthenticated encryption is a forgery kit.

**Flag:** `DS{crypto_ecbiv_L4_82ea6f}`

**Fix:** AEAD (GCM) — encryption and authenticity in one primitive.

## Tooling

- [`tools/break_them.py`](tools/break_them.py) — repeating-XOR recovery, CRC forgery and keystream-reuse attacks for all tiers.

## Vulnerable vs hardened

```kotlin
// vulnerable
fun encrypt(b: ByteArray) = b.mapIndexed { i, v -> (v xor key[i % key.size]).toByte() }

// hardened
Cipher AES/GCM/NoPadding + SecretKeySpec from AndroidKeyStore
```
