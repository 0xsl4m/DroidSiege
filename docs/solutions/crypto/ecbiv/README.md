# crypto / ecbiv — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M10 Insufficient Cryptography · **References:** MASVS-CRYPTO-2 · MASTG-TEST-0x49

## How this family works

The payroll renderer uses AES in ECB mode with zero IV discipline. Identical blocks map to identical ciphertext, and predictable IVs collapse CBC to ECB's patterns.

**Vulnerable code:** `challenges/crypto/EcbIvFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Pattern Block

**Vulnerable behavior:** AES/ECB encrypts the secret — block patterns leak content.

**Exploit:**
1. Copy the challenge ciphertext; feed the ECB detector (bundled tool).
2. The repeated-block analysis reveals the secret phrase without the key.

**Flag:** `DS{crypto_ecbiv_L1_7a2e46}`

**Fix:** AES/GCM (or CBC with random IV) — never ECB.

## L2 🟡 — Fixed IV

**Vulnerable behavior:** CBC with a fixed zero IV: equal prefixes produce equal ciphertext prefixes.

**Exploit:**
1. Encrypt two chosen plaintexts with the app's action; diff the ciphertexts.
2. The constant prefix confirms the zero IV; the bundled tool decrypts using it.

**Flag:** `DS{crypto_ecbiv_L2_d5f981}`

**Fix:** Random per-message IV, transmitted alongside (that's its purpose).

## L3 🟠 — Token Oracle

**Vulnerable behavior:** The IV is prepended but reused across messages (nonce reuse).

**Exploit:**
1. Collect two ciphertexts from the repeated action; XOR the first blocks (bundled tool).
2. IV reuse leaks the XOR of the plaintexts — the flag is one of them.

**Flag:** `DS{crypto_ecbiv_L3_38c1b7}`

**Fix:** Fresh random IV per encryption; GCM nonces must never repeat under one key.

## L4 🔴 — Nonce Reuse

**Vulnerable behavior:** ECB with block-aligned formatting: the secret sits on a known block boundary.

**Exploit:**
1. Craft the plaintext the challenge offers so the secret block aligns.
2. Cut-and-paste the aligned block into a decrypt-oracle request (the bundled script automates the byte-at-a-time recovery).
3. Deterministic padding + ECB = byte-at-a-time decryption without the key.

**Flag:** `DS{crypto_ecbiv_L4_82ea6f}`

**Fix:** Authenticated encryption (GCM) — and never let ciphertext blocks be individually meaningful.

## Tooling

- [`tools/mode_tools.py`](tools/mode_tools.py) — ECB block-pattern detection, fixed-IV diffing and byte-at-a-time recovery for all four tiers.

## Vulnerable vs hardened

```kotlin
// vulnerable
Cipher.getInstance("AES/ECB/PKCS5Padding")

// hardened
Cipher.getInstance("AES/GCM/NoPadding") with a fresh random nonce
```
