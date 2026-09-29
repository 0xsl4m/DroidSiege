#!/usr/bin/env python3
"""DroidSiege crypto/ecbiv — all four tier attacks in one console.

  python mode_tools.py ecb   <key-hex> <ct-hex>              # L1 AES-ECB decrypt
  python mode_tools.py cbc   <key-ascii> <iv-ascii> <ct-hex> # L2 AES-CBC decrypt
  python mode_tools.py oracle <ct-hex>                       # L3 padding-oracle driver
  python mode_tools.py nonce <known-pt> <c1-hex> <c2-hex>    # L4 GCM keystream reuse
"""
import binascii
import sys
from Crypto.Cipher import AES

def unhex(s):
    return binascii.unhexlify(s.strip())

def ecb(key_hex, ct_hex):
    pt = AES.new(unhex(key_hex), AES.MODE_ECB).decrypt(unhex(ct_hex))
    print(pt[:-pt[-1]].decode(errors="replace"))

def cbc(key_ascii, iv_ascii, ct_hex):
    pt = AES.new(key_ascii.encode(), AES.MODE_CBC, iv_ascii.encode()).decrypt(unhex(ct_hex))
    print(pt[:-pt[-1]].decode(errors="replace"))

def oracle(ct_hex):
    """Padding oracle driver.

    The oracle lives on the Token Oracle challenge screen (in-app console): submit a
    crafted hex blob and read back 'padding error' vs anything else. Wire
    `ask_oracle()` to your relay method (manual copy/paste works, or an adb input
    automation). The math below is the standard Vaudenay attack.
    """
    ct = unhex(ct_hex)
    block, recovered = ct[-16:], b""
    prev = ct[-32:-16]
    intermediate = bytearray(16)
    for idx in range(15, -1, -1):
        pad = 16 - idx
        crafted = bytearray(ct)
        for j in range(idx + 1, 16):
            crafted[-32 + j] = prev[j] ^ intermediate[j] ^ pad
        found = None
        for guess in range(256):
            crafted[-32 + idx] = guess
            if ask_oracle(crafted.hex()) == "padding error":
                continue
            found = guess ^ pad ^ prev[idx]
            break
        if found is None:
            raise SystemExit("oracle gave no answer — check the relay")
        intermediate[idx] = found
        recovered = bytes([found]) + recovered
    print("plaintext byte-block:", recovered)

def ask_oracle(_hex):
    raise SystemExit("wire ask_oracle() to the in-app console (paste the blob, read the reply)")

def nonce(known_pt, c1_hex, c2_hex):
    c1, c2 = unhex(c1_hex)[:-16], unhex(c2_hex)[:-16]  # strip GCM tags
    ks = bytes(a ^ b for a, b in zip(c1, known_pt.encode()))
    pt = bytes(a ^ b for a, b in zip(c2[: len(ks)], ks))
    print(pt.decode(errors="replace"))

if __name__ == "__main__":
    cmd = sys.argv[1]
    args = sys.argv[2:]
    {"ecb": lambda: ecb(*args), "cbc": lambda: cbc(*args),
     "oracle": lambda: oracle(*args), "nonce": lambda: nonce(*args)}[cmd]()
