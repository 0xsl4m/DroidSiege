# backend / brokenauth — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M3 (server-side broken auth — API #2) · **References:** OWASP API Top 2 · MASVS-AUTH-1

## How this family works

The brokenauth family: predictable session tokens, weak/absent JWT signature checks, no login rate limiting, and guessable non-expiring reset tokens.

**Vulnerable code:** `backend/src/main/kotlin/com/droidsiege/backend/BrokenAuthRoutes.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Predictable tokens

**Vulnerable behavior:** Tokens are sess-1000+userId — and the verifier accepts derivable, never-issued tokens.

**Exploit:**
1. Predict bob's token: `sess-1002` (bob is user 2).
2. `GET /api/auth/whoami` with `Authorization: Bearer sess-1002` — authenticated without ever logging in; the response carries the flag.
3. Hardened: SecureRandom tokens; only the issued-session map authenticates.

**Flag:** `DS{backend_brokenauth_L1_d20f6b}`

**Fix:** High-entropy tokens; stateful or signed verification; never derivable.

## L2 🟡 — alg:none & weak HMAC

**Vulnerable behavior:** The verifier accepts alg:none and checks HS256 against the secret "secret".

**Exploit:**
1. Forge: `header.{"alg":"none"}.payload.{"sub":9,"role":"admin"}` (bundled script builds it).
2. `GET /api/auth/jwt/verify?token=<forged>` — accepted, flag in the response.
3. Hardened: strong secret, alg pinned to HS256, forgeries rejected 401.

**Flag:** `DS{backend_brokenauth_L2_94c5e7}`

**Fix:** Pin the algorithm; keys from a vault, never a dictionary word.

## L3 🟠 — No rate limit, no lockout

**Vulnerable behavior:** Login has no rate limit: 6 wrong attempts, then the correct one.

**Exploit:**
1. `POST /api/auth/login` with svcadmin + 6 wrong passwords, then `123456`.
2. The success response carries the flag — the account survived the brute force.
3. Hardened: 5-fail lockout returns 429; the brute never completes.

**Flag:** `DS{backend_brokenauth_L3_6a81d3}`

**Fix:** Rate limiting + lockout + alerting.

## L4 🔴 — Guessable reset tokens

**Vulnerable behavior:** Reset tokens are md5("droidsiege"+username)[:8] and never expire.

**Exploit:**
1. `POST /api/auth/forgot {"username":"bob"}`, then compute the token (bundled script).
2. `POST /api/auth/reset` with the guessed token — password reset, flag revealed.
3. Hardened: SecureRandom token, 15-minute expiry, single-use.

**Flag:** `DS{backend_brokenauth_L4_f53b19}`

**Fix:** Unguessable, expiring, single-use tokens — delivered out of band.

## Tooling

- [`tools/brokenauth_attacks.py`](tools/brokenauth_attacks.py) — token prediction, JWT forgery, brute force and reset hijack for all tiers.

## Vulnerable vs hardened

```kotlin
// vulnerable
val token = "sess-" + (1000 + user.id)

// hardened
SecureRandom 128-bit tokens + server-side verification
```
