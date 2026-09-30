# backend / massassign — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M3 (server-side mass assignment — API #3) · **References:** OWASP API Top 3 · MASVS-AUTH-2

## How this family works

The massassign family: client-controlled role binding, hidden premium fields, nested-object binding, and over-exposed list responses. Every account starts unprivileged — flags are reachable only through the assignment bug.

**Vulnerable code:** `backend/src/main/kotlin/com/droidsiege/backend/MassAssignRoutes.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Role by request (mass-assign)

**Vulnerable behavior:** PUT /api/profile binds every JSON field — including `role`.

**Exploit:**
1. `PUT /api/profile {"fullname":"Alice Lab","role":"admin"}` with alice's token.
2. `GET /api/admin/metrics` — the elevated caller reads the flag.
3. Hardened: fullname is the only client-writable field; metrics stays 403.

**Flag:** `DS{backend_massassign_L1_7c2e58}`

**Fix:** Whitelist bindable fields; explicit DTOs per endpoint.

## L2 🟡 — Hidden premium fields

**Vulnerable behavior:** Hidden `flagAccess`/`isPremium` fields are settable and open the premium vault.

**Exploit:**
1. `PUT /api/profile {"flagAccess":true}` then `GET /api/profile`.
2. The premiumVault field carries the flag (and NOT the L3 secret — different field).
3. Hardened: the fields are server-controlled; the vault stays closed.

**Flag:** `DS{backend_massassign_L2_b94d70}`

**Fix:** Server-controlled state; clients propose, the server decides.

## L3 🟠 — Nested object binding

**Vulnerable behavior:** POST /api/account decodes a nested `prefs` object and binds its `vaultSync` flag.

**Exploit:**
1. `POST /api/account {"fullname":"Alice Lab","prefs":{"vaultSync":true}}`.
2. `GET /api/account` returns the secureNote — the L3 flag (and NOT the L2 one).
3. Hardened: the flat DTO never decodes nested prefs.

**Flag:** `DS{backend_massassign_L3_3e6fa4}`

**Fix:** Explicit nested DTOs; unknown/hidden structures never reach the domain.

## L4 🔴 — Over-exposed listing

**Vulnerable behavior:** GET /api/users returns every user's internal fields to ANY authenticated caller.

**Exploit:**
1. `GET /api/users` with alice's token — the listing includes role and internalFlag per user.
2. The internalFlag column carries the flag.
3. Hardened: admin-only, minimal summary DTO.

**Flag:** `DS{backend_massassign_L4_08d1c6}`

**Fix:** Response shaping per caller; expose the minimum, per role.

## Tooling

- [`tools/massassign_attacks.py`](tools/massassign_attacks.py) — role elevation, hidden-field flips, nested binding and exposure read for all tiers.

## Vulnerable vs hardened

```kotlin
// vulnerable
user.role = body.role ?: user.role

// hardened
// copy only whitelisted fields: user.fullname = body.fullname ?: user.fullname
```
