# crypto / random — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M10 Insufficient Cryptography · **References:** MASVS-CRYPTO-2 · MASTG-TEST-0x51

## How this family works

The token minter seeds java.util.Random (and friends) with time — the token space collapses to the seed space.

**Vulnerable code:** `challenges/crypto/RandomFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Fixed Seed

**Vulnerable behavior:** Tokens come from `Random(timestamp)`.

**Exploit:**
1. Read the timestamp the console prints for the mint action.
2. The bundled tool re-seeds Random over a ±window and reproduces the token.

**Flag:** `DS{crypto_ecbiv_L1_7a2e46}`

**Fix:** java.security.SecureRandom, unseeded by the developer.

## L2 🟡 — Hour Window

**Vulnerable behavior:** Math.random()-derived strings with second-resolution entropy.

**Exploit:**
1. Capture two tokens and their (visible) mint seconds.
2. Brute the small seed space per the bundled tool.

**Flag:** `DS{crypto_ecbiv_L2_d5f981}`

**Fix:** SecureRandom for every security decision.

## L3 🟠 — Counter Sessions

**Vulnerable behavior:** SecureRandom — but seeded with a constant 'for determinism'.

**Exploit:**
1. The fixed seed is in the code (jadx).
2. The bundled tool regenerates the whole token stream offline.
3. Seeding SecureRandom replaces its entropy, it doesn't add.

**Flag:** `DS{crypto_ecbiv_L3_38c1b7}`

**Fix:** Never call setSeed on SecureRandom; trust the platform entropy.

## L4 🔴 — Daily OTP

**Vulnerable behavior:** The reset codes use a global Random shared with UI animation jitter.

**Exploit:**
1. Collect the public jitter values the console displays.
2. State-recover the shared generator (bundled tool) and predict the next security output.
3. One generator for cosmetics and crypto leaks state across the boundary.

**Flag:** `DS{crypto_ecbiv_L4_82ea6f}`

**Fix:** Separate, dedicated SecureRandom instances for security outputs.

## Tooling

- [`tools/predict_tokens.sh`](tools/predict_tokens.sh) — re-seeds / state-recovers the generators and predicts each tier's token.

## Vulnerable vs hardened

```kotlin
// vulnerable
val rnd = java.util.Random(System.currentTimeMillis())

// hardened
val rnd = java.security.SecureRandom() // never manually seeded
```
