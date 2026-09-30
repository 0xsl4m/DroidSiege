#!/usr/bin/env python3
'''DroidSiege — binary/noobfusc L4: re-sign with the public debug key and walk
the signature check. The GET_SIGNATURES comparison trusts a constant the
attacker can reproduce because the app is debuggable.

Usage: python3 resign_check.py app-debug.apk
Requires: apksigner (build-tools), keytool, unzip.
'''
import subprocess, sys, zipfile, hashlib, base64

apk = sys.argv[1] if len(sys.argv) > 1 else "app-debug.apk"

# 1) the app's own (debug) signature digest — the constant the check compares
with zipfile.ZipFile(apk) as z:
    cert = next((n for n in z.namelist() if n.startswith("META-INF/") and n.endswith((".RSA", ".DSA", ".EC"))), None)
    print("signing block:", cert)

# 2) debug keystore digest (public on every dev machine)
out = subprocess.run(
    ["keytool", "-list", "-v", "-keystore",
     subprocess.run(["bash", "-lc", "echo $HOME/.android/debug.keystore"], capture_output=True, text=True).stdout.strip(),
     "-alias", "androiddebugkey", "-storepass", "android"],
    capture_output=True, text=True).stdout
for line in out.splitlines():
    if "SHA1:" in line:
        print("debug cert SHA1:", line.split("SHA1:")[1].strip())
        print("-> the check compares this public value; any re-signed build matches")

# 3) re-sign (zipalign first in practice)
print(subprocess.run(["apksigner", "sign", "--ks", "debug.keystore", apk],
                     capture_output=True, text=True).stderr or "signed")
print("L4: install the re-signed build — the signature check passes and reveals the flag")
