#!/usr/bin/env bash
# DroidSiege — privacy/pii-logs tooling pack
# Streams the PII log trail and re-identifies the md5 pseudonymizer.
# Requires: adb, the app running with the track actions exercised.
set -euo pipefail

echo "== live trail: exercise the track/flush actions now =="
adb logcat -s SiegePii -v time

echo
echo "== L3: the pseudonymizer is md5(email) — confirm a mapping =="
python3 - <<'PY'
import hashlib
for email in ("alice@siegeapp.dev", "bob@siegeapp.dev", "carol@siegeapp.dev"):
    print(email, "=>", hashlib.md5(email.encode()).hexdigest())
PY

echo
echo "== L4: pull the flushed payload file (in-app console echoes it too) =="
adb shell run-as com.droidsiege ls -la files/ 2>/dev/null || true
