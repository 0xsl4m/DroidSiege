#!/usr/bin/env python3
"""DroidSiege storage/sqlite — decrypt the L3/L4 sealed records.

L3 passphrase: SHA-256("droidsiege::" + ANDROID_ID)[..16]
L4 bridge:     SHA-256("warp" + "-" + reversed("k3y-2026"))[..16]  (decompiled WarpKeyBridge)
"""
import hashlib
import sqlite3
import sys
import cryptography.hazmat.primitives.ciphers.aead as aead

def android_id():
    out = subprocess_run(["adb", "shell", "settings", "get", "secure", "android_id"])
    return out.strip()

def subprocess_run(cmd):
    import subprocess
    return subprocess.run(cmd, capture_output=True, text=True).stdout

def open_record(path, record_id, key):
    con = sqlite3.connect(path)
    row = con.execute(
        "select payload, nonce from sealed_records where recordId = ?", (record_id,)
    ).fetchone()
    if not row:
        return None
    payload, nonce = row
    return aead.AESGCM(key).decrypt(nonce, payload, None)

def main(tmpdir):
    aid = android_id()
    print("android_id:", aid)

    key3 = hashlib.sha256(("droidsiege::" + aid).encode()).digest()[:16]
    try:
        print("L3 flag:", open_record(f"{tmpdir}/secure_store.db", "recovery", key3).decode())
    except Exception as e:
        print("L3: seed + pull first (%s)" % e)

    key4 = hashlib.sha256(b"warp-" + b"6202-yek"[::-1]).digest()[:16]
    try:
        print("L4 flag:", open_record(f"{tmpdir}/warp_vault.db", "warp-core", key4).decode())
    except Exception as e:
        print("L4: seed + pull first (%s)" % e)

if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else ".")
