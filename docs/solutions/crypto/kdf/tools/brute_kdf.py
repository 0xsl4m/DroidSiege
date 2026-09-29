#!/usr/bin/env python3
"""DroidSiege crypto/kdf — dictionary attacks against the weak KDF tiers.

  python brute_kdf.py md5                       # L1 (nothing to brute; key = md5(salt+pw))
  python brute_kdf.py pbkdf2 <salt-ascii> <iters> <b64-iv||ct>   # L2/L3 dictionary
"""
import base64
import hashlib
import sys
import cryptography.hazmat.primitives.ciphers.aead as aead

WORDS = [
    "siege123", "letmein", "password", "123456", "qwerty",
    "siege-master-2024", "correct-horse-battery", "player1", "siege", "wallet",
]

def try_word(word, salt, iterations, blob):
    key = hashlib.pbkdf2_hmac("sha256", word.encode(), salt, iterations)[:16]
    iv, ct = blob[:12], blob[12:]
    try:
        return aead.AESGCM(key).decrypt(iv, ct, None).decode()
    except Exception:
        return None

def main():
    mode = sys.argv[1]
    if mode == "md5":
        key = hashlib.md5(b"droidsiegesiege-master-2024").digest()
        print("L1 key (md5(salt+password)):", key.hex(), "-> AES-128-CBC, iv=0x11*16")
        return
    salt = sys.argv[2].encode()
    iterations = int(sys.argv[3])
    blob = base64.b64decode(sys.argv[4])
    for word in WORDS:
        got = try_word(word, salt, iterations, blob)
        if got:
            print(f"password={word!r} ->", got)
            return
    print("not in wordlist — extend WORDS")

if __name__ == "__main__":
    main()
