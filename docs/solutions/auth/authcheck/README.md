# auth / authcheck — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M3 Insecure Authentication/Authorization · **References:** MASVS-AUTH-2 · MASTG-TEST-0x52

## How this family works

The admin console performs its authorization checks in the wrong places: client-side role checks, claim-based entitlements, an AND-gate that splits the secret across both factors the client controls, and a server response the client 'interprets'.

**Vulnerable code:** `challenges/auth/AuthCheckFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Client Admin Gate

**Vulnerable behavior:** `isAdmin` is a client-side boolean from prefs.

**Exploit:**
1. Flip the boolean via run-as (the bundled script does it).
2. The console opens with the flag half.

**Flag:** `DS{auth_authcheck_L1_3b95d0}`

**Fix:** Roles verified server-side per request.

## L2 🟡 — Hidden Screen

**Vulnerable behavior:** The role check reads a JWT claim the client itself decoded.

**Exploit:**
1. Forge the claim (the session family's codec applies here).
2. The check passes — the claim was authored by the attacker.

**Flag:** `DS{auth_authcheck_L2_a7e418}`

**Fix:** Server issues and enforces entitlements; claims are identity, not authority.

## L3 🟠 — Local Feature Flag

**Vulnerable behavior:** Two client-controlled flags must BOTH be true — each trivially settable.

**Exploit:**
1. Set the role flag AND flip the entitlement bit (bundled script).
2. The AND-gate meant 'two client booleans', not two factors.

**Flag:** `DS{auth_authcheck_L3_c6219f}`

**Fix:** Multi-factor means independent factors — at least one server-held.

## L4 🔴 — Obfuscated Authz Chain

**Vulnerable behavior:** The final gate consults the 'server' verdict from a response cached in prefs.

**Exploit:**
1. Edit the cached verdict (bundled script), reopen the console.
2. The client trusts yesterday's server answer — full bypass.

**Flag:** `DS{auth_authcheck_L4_50d7e3}`

**Fix:** Authorization is per-request, in the moment, on the server.

## Tooling

- [`tools/forge_tokens.sh`](tools/forge_tokens.sh) — auth-family pack: flips every client-side gate (boolean, claim, AND-pair, cached verdict).

## Vulnerable vs hardened

```kotlin
// vulnerable
if (prefs.getBoolean("isAdmin", false)) openAdmin()

// hardened
// the server answers "may this session do X?" per request
```
