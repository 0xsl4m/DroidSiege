#!/usr/bin/env bash
# DroidSiege — privacy/perms tooling pack
# Grants the blanket permission, shows the manifest flags, and flips the
# client-side 'granted' prefs bit the L4 tier trusts.
# Requires: adb, a debug build (run-as).
set -euo pipefail
PKG="com.droidsiege"

echo "== manifest storage flags (L2: requestLegacyExternalStorage) =="
adb shell dumpsys package "$PKG" | grep -E "READ_EXTERNAL|LEGACY|WRITE_EXTERNAL" || true

echo
echo "== L1/L3: grant blanket media access and watch the app enumerate =="
adb shell pm grant "$PKG" android.permission.READ_EXTERNAL_STORAGE 2>/dev/null \
  || echo "(grant failed — scoped-storage device; the app asks via the dialog)"

echo
echo "== L4: the app trusts its own prefs 'granted' bit — flip it with no system grant =="
adb shell run-as "$PKG" sh -c 'cat shared_prefs/perms_prefs.xml' 2>/dev/null || true
adb shell run-as "$PKG" sh -c \
  'sed -i "s/name=\"granted\" value=\"false\"/name=\"granted\" value=\"true\"/" shared_prefs/perms_prefs.xml' \
  || echo "(flip via the challenge action if sed is unavailable)"
echo "re-open the perms challenge — the feature proceeds with no real grant"
