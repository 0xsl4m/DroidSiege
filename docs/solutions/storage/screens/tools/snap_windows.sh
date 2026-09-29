#!/usr/bin/env bash
# DroidSiege — storage/screens tooling pack
# Captures the leak windows: recents thumbnail, snapshots and the transient states.
set -euo pipefail

echo "== L1: recents thumbnail =="
echo "1) press render on the challenge   2) adb shell input keyevent KEYCODE_HOME"
echo "3) open recents (KEYCODE_APP_SWITCH) — the thumbnail shows the secret"
echo "4) screenshot it: adb exec-out screencap -p > recents.png"

echo
echo "== L2: saved snapshot =="
adb shell ls /sdcard/Android/data/com.droidsiege/files/Pictures/snapshots/ 2>/dev/null || true
SNAP=$(adb shell ls /sdcard/Android/data/com.droidsiege/files/Pictures/snapshots/ 2>/dev/null | tr -d '\r' | tail -1 || true)
if [ -n "$SNAP" ]; then
  adb pull "/sdcard/Android/data/com.droidsiege/files/Pictures/snapshots/$SNAP" /tmp/snap.jpg > /dev/null
  echo "pulled /tmp/snap.jpg"
fi

echo
echo "== L3: beat the 4s mask =="
echo "adb exec-out screencap -p > fast.png   # immediately after pressing Reveal"

echo
echo "== L4: loop captures while the partner preview runs =="
echo "while true; do adb exec-out screencap -p > loop_\$(date +%s%N).png; sleep 0.3; done"
