#!/usr/bin/env bash
# DroidSiege — advanced/native tooling pack
# Recon + one-liners against libdroidsiege-vault.so for all four tiers.
# Requires: adb (debug build), readelf/strings (binutils), frida (optional).
set -euo pipefail
LIB="libdroidsiege-vault.so"

echo "== pull the library from the installed app =="
adb shell run-as com.droidsiege ls -la .. 2>/dev/null || true
adb pull "/data/app/*/com.droidsiege*/lib/arm64/$LIB" /tmp/ 2>/dev/null \
  || adb shell "find /data/app -name '$LIB' 2>/dev/null" \
  || echo "(or grab it from the APK: unzip -j app-debug.apk 'lib/arm64-v8a/$LIB')"
unzip -j -o app-debug.apk "lib/arm64-v8a/$LIB" -d /tmp/ 2>/dev/null || true

echo
echo "== L1: the magic value nativeCheck compares — strings tell you =="
strings /tmp/$LIB | grep -i "siege\|magic\|clean" | head -5

echo
echo "== L4: hiddenPrintFlag is exported for anyone to call =="
readelf -sW /tmp/$LIB 2>/dev/null | grep -i hidden || nm -D /tmp/$LIB | grep -i hidden

echo
echo "== L2/L3: in-app — declare a huge length (over-read) / pass %s as the format =="
echo "nativeParse(input, 999) walks past the record; nativeLog('%s') prints the secret argument."

echo
echo "== optional: call the hidden export directly =="
cat <<'JS'
// frida -U -f com.droidsiege -l - --no-pause, then:
var addr = Module.findExportByName("libdroidsiege-vault.so",
    "Java_com_droidsiege_challenges_advanced_NativeVault_hiddenPrintFlag");
var NativeVault = Java.use("com.droidsiege.challenges.advanced.NativeVault");
console.log(NativeVault.hiddenPrintFlag());
JS
