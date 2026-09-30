# crypto / hardcoded — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M10 Insufficient Cryptography · **References:** MASVS-CRYPTO-1 · MASTG-TEST-0x48

## How this family works

The vault feature encrypts with keys embedded in the binary or resources — `jadx` reads them in seconds, and every install shares the same key.

**Vulnerable code:** `challenges/crypto/HardcodedKeyFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Static License

**Vulnerable behavior:** The AES key is a string constant next to the crypto call.

**Exploit:**
1. Open jadx, search for the vault key constant (the challenge hints name it).
2. Decrypt the on-screen ciphertext with the bundled tool using that key.

**Flag:** `DS{crypto_hardcoded_L1_41f7c2}`

**Fix:** Keys from AndroidKeyStore; nothing secret in code or resources.

## L2 🟡 — Split Secret

**Vulnerable behavior:** The key is assembled from resource strings so it doesn't show in one place.

**Exploit:**
1. Decompile resources (`apktool d`), reassemble the parts per the bundled tool.
2. Same key as L1 — just scattered.

**Flag:** `DS{crypto_hardcoded_L2_9d3a58}`

**Fix:** Same: Keystore-generated keys; assembly logic in code is still publication.

## L3 🟠 — Signed Derivation

**Vulnerable behavior:** The key is XOR of two constants ('split knowledge').

**Exploit:**
1. Read both constants in smali/jadx; XOR them (bundled tool).
2. Static splitting is arithmetic, not secrecy.

**Flag:** `DS{crypto_hardcoded_L3_e6b824}`

**Fix:** Keystore keys never exist as material the attacker can read.

## L4 🔴 — Bridge License

**Vulnerable behavior:** The key is derived from the signature hash via a printed formula.

**Exploit:**
1. Compute the signing-cert hash with `keytool -printcert` (debug keystore is public).
2. Feed it through the derivation in the bundled tool and decrypt.
3. Deriving from public inputs yields public keys.

**Flag:** `DS{crypto_hardcoded_L4_15c7f9}`

**Fix:** Keystore with setUserAuthenticationRequired where needed; derivations need a secret only the keystore holds.

## Tooling

- [`tools/hook-secretkeyspec.js`](tools/hook-secretkeyspec.js) — Frida hook printing every SecretKeySpec the app builds (all tier keys).
- [`tools/decrypt_license.py`](tools/decrypt_license.py) — re-derives each tier's key from the extracted constants and decrypts the vault blob.

## Vulnerable vs hardened

```kotlin
// vulnerable
val KEY = "5up3r-s3cr3t".toByteArray()

// hardened
// AndroidKeyStore: key generated in, and never leaving, the keystore
```
