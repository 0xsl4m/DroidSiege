#!/usr/bin/env python3
"""DroidSiege crypto/hardcoded — decrypt every license tier.

L1: key = "droidsiege-static"[..16]                     (jadx constant)
L2: key = SHA-256(BuildConfig.WALLET_KEY_PART_A + "install_seed_2026")[..16]
L3: key = SHA-256(partner_tag + sha256hex(signing cert))  (apksigner --print-certs)
L4: key = SHA-256("Lic" + reversed("3N$E").lower() + "nse-vault")   (LicenseKeyBridge)
All blobs: base64(iv[12] || AES-GCM ciphertext).
"""
import base64
import hashlib
import subprocess
import sys
import cryptography.hazmat.primitives.ciphers.aead as aead

def gcm_open(key, blob_b64):
    raw = base64.b64decode(blob_b64)
    iv, ct = raw[:12], raw[12:]
    return aead.AESGCM(key).decrypt(iv, ct, None).decode()

def cert_sha256_hex(apk):
    out = subprocess.run(
        ["apksigner", "verify", "--print-certs", apk],
        capture_output=True, text=True).stdout
    for line in out.splitlines():
        if "SHA-256 digest" in line:
            return line.split(":")[-1].strip().replace(" ", "").lower()
    raise SystemExit("apksigner not found or no cert digest in output")

def main():
    blob = input("paste the license blob (base64): ").strip()

    print("L1:", gcm_open(b"droidsiege-static", blob))

    part_a = "w4ll3t-p4rt-"
    key2 = hashlib.sha256((part_a + "install_seed_2026").encode()).digest()[:16]
    print("L2:", gcm_open(key2, blob))

    if len(sys.argv) > 1:
        cert_hex = cert_sha256_hex(sys.argv[1])
        tag = "partn3r-tag-2026"
        key3 = hashlib.sha256((tag + cert_hex).encode()).digest()
        print("L3:", gcm_open(key3, blob))

    key4 = hashlib.sha256(b"Lic" + "3N$E"[::-1].lower().encode() + b"nse-vault").digest()
    print("L4:", gcm_open(key4, blob))

if __name__ == "__main__":
    main()
