#!/usr/bin/env bash
# DroidSiege — storage/prefs tooling pack
# Dumps the wallet prefs file and decodes the artifacts of each tier.
# Requires: adb, a debug build (run-as) or a rooted device.
set -euo pipefail

PKG="com.droidsiege"
PREFS="shared_prefs/siege_wallet_prefs.xml"

echo "== raw prefs (L1 reads plaintext here; L2 looks 'encrypted') =="
adb shell run-as "$PKG" cat "$PREFS" 2>/dev/null || adb shell su -c "cat /data/data/$PKG/$PREFS"

echo
echo "== L2: decode the base64 'encrypted_flag' (encoding != encryption) =="
python3 - <<'PY'
import base64, re, subprocess, sys
raw = subprocess.run(
    ["adb", "shell", "run-as", "com.droidsiege", "cat", "shared_prefs/siege_wallet_prefs.xml"],
    capture_output=True, text=True).stdout
for name in ("encrypted_flag",):
    m = re.search(rf'<string name="{name}">([^<]+)</string>', raw)
    if m:
        print(name, "=>", base64.b64decode(m.group(1)).decode(errors="replace"))
PY

echo
echo "== L3/L4: rebuild the keys and decrypt the AES-GCM blobs =="
python3 "$(dirname "$0")/decrypt_prefs.py"
