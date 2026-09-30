# backend / idor — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M3 (server-side BOLA — API #1) · **References:** OWASP API Top 1 · MASVS-AUTH-2

## How this family works

The `:backend` lab hosts the server-side BOLA family. Start it with `docker compose up --build` and point the app at it (or hit it with curl/Burp). All flags live on objects owned by the `system` account (role-gated out of login), so the only path is the missing authorization itself.

**Vulnerable code:** `backend/src/main/kotlin/com/droidsiege/backend/IdorRoutes.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Secret for sale (BOLA on /secret)

**Vulnerable behavior:** GET /api/users/{id}/secret returns any user's secret — no ownership check.

**Exploit:**
1. Login as alice (seed creds in the Handoff/README): `POST /api/auth/login {"username":"alice","password":"hunter2"}`.
2. `GET /api/users/5/secret` — user 5 is the system 'vaultkeeper'. The flag is in the response.
3. SECURE_MODE=on: the same request 403s; the owner read returns benign data.

**Flag:** `DS{backend_idor_L1_c39a17}`

**Fix:** Authorize every object access against the authenticated principal.

## L2 🟡 — Hashid vault (enumerable ids)

**Vulnerable behavior:** GET /api/vaults/{hid} uses salted-digest ids — but the space is tiny and the server never authorizes.

**Exploit:**
1. Enumerate: vault 7's hid = first 8 base32 chars of sha256("siege7") — the bundled tool computes it (or brute ids 0-64).
2. `GET /api/vaults/<hid>` returns the flagged vault contents.
3. Hardened: the vault belongs to the system account — no loginable principal can own it.

**Flag:** `DS{backend_idor_L2_5f8e02}`

**Fix:** Obfuscation is not authorization; check ownership server-side.

## L3 🟠 — Read-only guard (write verbs unguarded)

**Vulnerable behavior:** GET /api/notes/{id} checks ownership — PUT/DELETE do not.

**Exploit:**
1. `PUT /api/notes/2 {"note":"hacked"}` with alice's token: bob's note is modified and the response reveals the flag.
2. Hardened: the write verbs enforce the same check as the read.

**Flag:** `DS{backend_idor_L3_e1d4c8}`

**Fix:** Authorize all verbs, not just reads.

## L4 🔴 — Nested escape (order/item gap)

**Vulnerable behavior:** Nested resources: the order is authorized but the item is fetched globally.

**Exploit:**
1. `GET /api/orders/101/items/503` with alice's token — item 503 belongs to the system order 103, not order 101.
2. The full-path gap serves the item with the flag.
3. Hardened: item.orderId must equal the path order — 403.

**Flag:** `DS{backend_idor_L4_a7b93f}`

**Fix:** Authorize the full path: every nested object must belong to its parent and to the caller.

## Tooling

- [`tools/idor_attacks.py`](tools/idor_attacks.py) — curl-driven exploit script for all four tiers (vuln + hardened verification).

## Vulnerable vs hardened

```kotlin
// vulnerable
// order ownership checked, item fetched by id

// hardened
// item.orderId == order.id && order.ownerId == caller
```
