#!/usr/bin/env python3
"""DroidSiege storage/backup L4 — decrypt files/backup_bundle.bin.

Recovery key = MD5("backup-recovery::" + device_identity) where device_identity
is the ANDROID_ID the app itself stores in siege_backup_prefs.
"""
import hashlib
import re
import subprocess
import cryptography.hazmat.primitives.ciphers.aead as aead

def sh(*cmd):
    return subprocess.run(list(cmd), capture_output=True).stdout

identity = None
raw = sh("adb", "shell", "run-as", "com.droidsiege",
         "cat", "shared_prefs/siege_backup_prefs.xml").decode(errors="replace")
m = re.search(r'<string name="device_identity">([^<]+)</string>', raw)
if m:
    identity = m.group(1)
else:
    identity = sh("adb", "shell", "settings", "get", "secure", "android_id").decode().strip()
print("device identity:", identity)

key = hashlib.md5(("backup-recovery::" + identity).encode()).digest()
blob = sh("adb", "shell", "run-as", "com.droidsiege", "cat", "files/backup_bundle.bin")
iv_len = blob[0]
iv, ct = blob[1:1 + iv_len], blob[1 + iv_len:]
print("flag:", aead.AESGCM(key).decrypt(iv, ct, None).decode())
