#!/usr/bin/env bash
# DroidSiege — storage/clipboard tooling pack
set -euo pipefail
PKG="com.droidsiege"

echo "== L1: the clipboard itself (read from another context while focused) =="
adb shell run-as $PKG log -t ClipboardPeek "poking clipboard" 2>/dev/null || true
echo "paste anywhere focused, or use the inspect button on the challenge screen"

echo
echo "== L3: the app's own persisted clipboard history =="
adb shell run-as $PKG cat shared_prefs/siege_clip_history.xml 2>/dev/null || \
  echo "(press 'Copy store coupon' first)"

echo
echo "== L4: the simulated keyboard/autofill cache =="
adb shell run-as $PKG cat files/keyboard_suggestions.cache 2>/dev/null || \
  echo "(press 'Save safe-deposit note' first)"
