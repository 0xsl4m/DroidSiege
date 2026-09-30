#!/usr/bin/env bash
# DroidSiege — supplychain/vulndep tooling pack
# Prints the dependency chain (L2) and drives the decoder/plugin flows.
# Requires: gradlew, adb.
set -euo pipefail

echo "== L2: the transitive chain putting the ECB helper on the classpath =="
./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -B2 -A2 "ecb\|old-crypto" || true

echo
echo "== L1: drive the crafted-header decode (the app action simulates the over-read) =="
adb shell am start -n com.droidsiege/.MainActivity
echo "open Supply Chain -> Vulnerable Decoder -> Decode crafted image (insecure mode)"

echo
echo "== L3: plant and inspect the deserialization payload =="
adb shell run-as com.droidsiege sh -c 'ls -la files/sync_payload.ser' 2>/dev/null || \
  echo "(payload appears after the first sync in the challenge)"
adb shell run-as com.droidsiege sh -c 'xxd files/sync_payload.ser | head -3' 2>/dev/null || true

echo
echo "== L4: write a plugin.dex the loader will eat (real class compiled with dx) =="
echo "javac Plugin.java && d8 Plugin.class --output . && adb push classes.dex /sdcard/Android/data/com.droidsiege/files/plugin.dex"
