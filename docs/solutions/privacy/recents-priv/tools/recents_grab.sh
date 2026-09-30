#!/usr/bin/env bash
# DroidSiege — privacy/recents-priv tooling pack
# Races the unblurred first frame and dumps the task metadata the 'incognito'
# mode still fills in.
# Requires: adb, the app open on the private-notes screen.
set -euo pipefail

echo "== L2: race the first frame (FLAG_SECURE set only after first render) =="
adb shell am start -n com.droidsiege/.MainActivity
adb exec-out screencap -p > /tmp/first_frame.png 2>/dev/null \
  || adb shell screencap -p /sdcard/ff.png && adb pull /sdcard/ff.png /tmp/first_frame.png
echo "saved /tmp/first_frame.png — inspect the notes content"

echo
echo "== L4: task descriptions carry the 'private' note =="
adb shell dumpsys activity recents | grep -A2 "realActivity\|taskDescription" | head -30
