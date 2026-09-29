#!/usr/bin/env bash
# DroidSiege — storage/extstorage tooling pack
# Collects every external artifact the extstorage family produces.
set -euo pipefail
PKG="com.droidsiege"

echo "== L1: app-scoped external file =="
adb shell ls -la /sdcard/Android/data/$PKG/files/ 2>/dev/null || true
adb shell cat /sdcard/Android/data/$PKG/files/receipt_export.txt 2>/dev/null || true

echo
echo "== L2: cache file (run-as; cache is app-private but not protected) =="
adb shell run-as $PKG cat cache/tmp_report_1337.txt 2>/dev/null || true

echo
echo "== L3: provider-shared file (with a grant from the share sheet, or the broad path) =="
echo "content://$PKG.transfer/shared/transfer_summary.txt"
adb shell run-as $PKG cat files/shared/transfer_summary.txt 2>/dev/null || true

echo
echo "== L4: EXIF UserComment of the session photo =="
PHOTO=$(adb shell find /sdcard/Android/data/$PKG/files/Pictures -name "session_photo.jpg" 2>/dev/null | tr -d '\r' | head -1 || true)
if [ -n "$PHOTO" ]; then
  adb pull "$PHOTO" /tmp/session_photo.jpg > /dev/null
  exiftool /tmp/session_photo.jpg 2>/dev/null | grep -i comment || \
    python3 - <<'PY'
from PIL import Image
img = Image.open("/tmp/session_photo.jpg")
print("UserComment:", img.getexif().get(0x9286))
PY
else
  echo "run 'Save session photo' first"
fi
