#!/usr/bin/env bash
# DroidSiege — storage/logs tooling pack
# Harvests each tier's logcat trail and the crash-reporter payload.
set -euo pipefail

echo "== clear, trigger the challenge actions in the app, then: =="

echo "== L1/L2/L3: SiegeWallet tag (balance refresh / sync failure / verbose) =="
adb logcat -d -s SiegeWallet

echo
echo "== L4: the reporter also echoes every breadcrumb =="
adb logcat -d -s SiegeReporter

echo
echo "== L4 payload file =="
adb shell run-as com.droidsiege cat files/crash_breadcrumbs.log 2>/dev/null || true
