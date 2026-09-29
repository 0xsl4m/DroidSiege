#!/usr/bin/env bash
# DroidSiege — storage/backup tooling pack
# Runs a real backup through bmgr, then locates and decrypts the artifacts.
set -euo pipefail
PKG="com.droidsiege"

echo "== enable local backup transport and run a backup now =="
adb shell bmgr enable true
adb shell bmgr transport com.android.localtransport/.LocalTransport || true
adb shell bmgr backupnow $PKG

echo
echo "== L1/L2: what the backup set contains =="
echo "prefs:  shared_prefs/siege_backup_prefs.xml (account_recovery)"
echo "file:   files/support/escalation_note.txt (via SiegeBackupAgent)"
adb shell run-as $PKG cat shared_prefs/siege_backup_prefs.xml 2>/dev/null || true
adb shell run-as $PKG cat files/support/escalation_note.txt 2>/dev/null || true

echo
echo "== L4: decrypt the backup bundle (recovery key is derivable) =="
python3 "$(dirname "$0")/decrypt_backup_bundle.py"
