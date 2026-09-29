#!/usr/bin/env bash
# DroidSiege — crypto/random tooling pack
# The deterministic-token attacks live in the homegrown console (same Java LCG engine).
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"

echo "== L1: fixed seed token =="
python3 "$HERE/../../homegrown/tools/break_them.py" token-fixed

echo
echo "== L2: hour brute around now (pass minutes since epoch) =="
python3 "$HERE/../../homegrown/tools/break_them.py" token-hour "$(($(date +%s) / 60))"

echo
echo "== L3: counter session id (session number from siege_session_prefs.xml) =="
adb shell run-as com.droidsiege cat shared_prefs/siege_session_prefs.xml 2>/dev/null || true
N=$(adb shell run-as com.droidsiege cat shared_prefs/siege_session_prefs.xml 2>/dev/null | grep -oE 'session_counter[^<]*' | grep -oE '[0-9]+' | tail -1 || echo 1)
python3 "$HERE/../../homegrown/tools/break_them.py" token-counter "$N"

echo
echo "== L4: daily OTP =="
python3 "$HERE/../../homegrown/tools/break_them.py" otp "$(date +%-d)"
