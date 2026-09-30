#!/usr/bin/env bash
# DroidSiege — binary/noobfusc tooling pack
# Unpacks the (unobfuscated) APK and recovers every tier's artifact.
# Requires: apktool, jadx, python3.
set -euo pipefail
APK="${1:-app-debug.apk}"

echo "== L1: grep the flag constants straight out of the tree =="
apktool d -f -o /tmp/ds_unpacked "$APK" >/dev/null
grep -rn "DS{" /tmp/ds_unpacked/res /tmp/ds_unpacked/smali | head -10

echo
echo "== L2: the base64 'hidden' resource decodes to the L2 flag =="
echo "RFN7YmluYXJ5X25vb2JmdXNjX0wyX2I4NDFkMH0=" | base64 -d; echo

echo
echo "== L3: readable smali — the unlock logic compares a magic constant =="
grep -rn "const-string" /tmp/ds_unpacked/smali/com/droidsiege/challenges/binary/ | grep -i "magic\|unlock" | head -5
echo "(enter the printed constant in the challenge's unlock input)"

echo
echo "== L4: the app is debuggable and trusts GET_SIGNATURES — see resign_check.py =="
grep -n "debuggable" /tmp/ds_unpacked/AndroidManifest.xml || true
