#!/usr/bin/env python3
"""DroidSiege storage/prefs — reconstruct the L3/L4 keys and decrypt the stored blobs.

L3: key = SHA-256("droidsiege_pref_secret_2024")[..16]  (strings.xml constant)
L4: master key = "droidsiege-mk!"[..16] and the plaintext support_cache sits next to it
"""
import base64
import hashlib
import json
import re
import subprocess
import sys
import cryptography.hazmat.primitives.ciphers.aead as aead

RAW = subprocess.run(
    ["adb", "shell", "run-as", "com.droidsiege", "cat", "shared_prefs/siege_wallet_prefs.xml"],
    capture_output=True, text=True).stdout

def entry(name):
    m = re.search(rf'<string name="{name}">([^<]+)</string>', RAW)
    return base64.b64decode(m.group(1)) if m else None

def gcm_open(key, iv, ct):
    return aead.AESGCM(key).decrypt(iv, ct, None)

def main():
    # L3 — derived key; the app stores the IV in its own prefs entry
    key3 = hashlib.sha256(b"droidsiege_pref_secret_2024").digest()[:16]
    try:
        print("L3 flag:", gcm_open(key3, entry("derived_iv"), entry("derived_flag")).decode())
    except Exception as e:
        print("L3: run 'Save release-keyed session' first (%s)" % e)

    # L4 — mirrored master key makes the vault trivially openable...
    mk = entry("master_key_hint")
    if mk:
        try:
            print("L4 flag (via mirrored master key):", gcm_open(mk, entry("master_iv"), entry("vault_flag")).decode())
        except Exception as e:
            print("L4 mirrored-key path failed: %s" % e)

    # ...but the challenge even ships the plaintext photocopy:
    m = re.search(r'<string name="support_cache">([^<]+)</string>', RAW)
    if m:
        print("L4 flag (via support cache):", m.group(1))

if __name__ == "__main__":
    main()
