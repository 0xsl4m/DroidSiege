#!/usr/bin/env bash
# DroidSiege — storage/sqlite tooling pack
# Pulls every challenge database and decrypts the sealed stores.
set -euo pipefail

PKG="com.droidsiege"
DIR="$(mktemp -d)"

for db in vault.db sys_cache_v3.db secure_store.db warp_vault.db; do
  adb shell run-as "$PKG" cat "databases/$db" > "$DIR/$db" 2>/dev/null \
    && echo "pulled $db" || echo "$db absent (run its seed action first)"
done

echo
echo "== L1: notes table =="
sqlite3 "$DIR/vault.db" "select title, body from notes;" 2>/dev/null || true

echo
echo "== L2: ops cache =="
sqlite3 "$DIR/sys_cache_v3.db" "select c1, cfg_blob from sys_kv;" 2>/dev/null || true

echo
echo "== L3/L4: reconstruct keys and decrypt the sealed records =="
python3 "$(dirname "$0")/decrypt_sealed.py" "$DIR"
