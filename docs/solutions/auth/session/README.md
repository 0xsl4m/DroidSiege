# auth / session — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M3 Insecure Authentication/Authorization · **References:** MASVS-AUTH-1 · MASTG-TEST-0x52

## How this family works

Session handling done wrong on the client: plaintext prefs tokens, client-side JWT verification with alg:none / a shipped HMAC key, and authorization decided by decoded token claims. The lab backend (`:backend`, SECURE_MODE families) is the real-server counterpart — session L2's probe sends its forge to the backend verifier too.

**Vulnerable code:** `challenges/auth/SessionFamily.kt + challenges/auth/JwtCodec.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Plaintext Session

**Vulnerable behavior:** The session token (the flag) sits in plaintext prefs and 'logged in' is a local boolean.

**Exploit:**
1. Persist the session (insecure mode), then `adb shell run-as com.droidsiege cat shared_prefs/session_prefs.xml`.
2. The token value is the flag — and flipping the boolean logs anyone in.

**Flag:** `DS{auth_session_L1_f06b21}`

**Fix:** Server-validated sessions; the client holds only an opaque, revocable handle.

## L2 🟡 — alg:none

**Vulnerable behavior:** The client verifies session JWTs itself and accepts alg:none.

**Exploit:**
1. Forge header {"alg":"none"} + payload {"role":"admin"} (the Forge action builds it).
2. The client verifier accepts the unsigned token and opens the session; the backend probe shows a real verifier accepting the same forge when the lab backend runs in vuln mode.

**Flag:** `DS{auth_session_L2_8a3ce7}`

**Fix:** Verify signatures server-side; pin the alg list — 'none' is never on it.

## L3 🟠 — Weak HMAC

**Vulnerable behavior:** The verifier checks HS256 — with the shipped key `siege-jwt-secret-2026`.

**Exploit:**
1. Pull the key with jadx (the hint names it).
2. Sign {"role":"admin"} with it (JwtCodec / openssl / hashcat) and replay.
3. A shipped HMAC key is a signing oracle for everyone.

**Flag:** `DS{auth_session_L3_d19f45}`

**Fix:** Asymmetric signatures; the client verifies with a public key, the server signs.

## L4 🔴 — Client Role

**Vulnerable behavior:** The app decides entitlements by decoding the role claim from a token it just verified.

**Exploit:**
1. Chain L3: sign role=admin with the known key.
2. The client unlocks 'admin functions' from the claim — authorization authored by the attacker.
3. The lab backend's hardened massassign family shows the server-side alternative.

**Flag:** `DS{auth_session_L4_62c8ba}`

**Fix:** The token identifies; the server decides. Re-check entitlements per action.

## Tooling

- [`tools/forge_tokens.sh`](tools/forge_tokens.sh) — auth-family pack: alg:none and weak-HMAC forgeries, verified against the app codec + the lab backend.

## Vulnerable vs hardened

```kotlin
// vulnerable
if (header.alg == "none") accept(claims)

// hardened
// server verifies HS256/RS256 with a pinned alg list and a server-held key
```
