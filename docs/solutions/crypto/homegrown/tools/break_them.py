#!/usr/bin/env python3
"""DroidSiege crypto/homegrown + crypto/random — reversible attacks in one console.

  python break_them.py xor-single <ct-hex>                 # L1: brute 256 keys
  python break_them.py xor-repeating <ct-hex> <known>      # L2: known-plaintext key recovery
  python break_them.py proprietary <ct-hex>                # L3: invert the 4-round transform
  python break_them.py stream <issued-minutes> <ct-hex>    # L4: brute java Random hour seeds
  python break_them.py token-fixed                         # random L1: Random(42) token
  python break_them.py token-hour <epoch-minutes>          # random L2: hour brute
  python break_them.py token-counter <n>                   # random L3: md5 counter token
  python break_them.py otp <day-of-month>                  # random L4: daily OTP
"""
import binascii
import hashlib
import struct
import sys

MASK48 = (1 << 48) - 1
MULT = 0x5DEECE66D
ADD = 0xB

class JavaRandom:
    """java.util.Random, exactly."""

    def __init__(self, seed):
        self.seed = (seed ^ MULT) & MASK48

    def next(self, bits):
        self.seed = (self.seed * MULT + ADD) & MASK48
        return self.seed >> (48 - bits)

    def next_int(self):
        v = self.next(32)
        return v - (1 << 32) if v >= (1 << 31) else v

    def next_int_bound(self, bound):
        # java.util.Random.nextInt(bound): next(31), power-of-two fast path,
        # otherwise rejection to kill modulo bias.
        if bound & (-bound) == bound:
            return (bound * self.next(31)) >> 31
        while True:
            bits = self.next(31)
            val = bits % bound
            # java re-draws when bits - val + (bound - 1) overflows int
            if ((bits - val + (bound - 1)) & 0xFFFFFFFF) < 0x80000000:
                return val

def rotl(x, n):
    return ((x << n) | (x >> (8 - n))) & 0xFF

def rotr(x, n):
    return ((x >> n) | (x << (8 - n))) & 0xFF

def transform(b, rnd):
    x = b
    for r in range(rnd + 1):
        x = rotl(x, 3)
        x ^= (0x5F + r) & 0xFF
        x = (x + 0x21 + r) & 0xFF
    return x

def invert(b, rnd):
    x = b
    for r in range(rnd, -1, -1):
        x = (x - 0x21 - r) & 0xFF
        x ^= (0x5F + r) & 0xFF
        x = rotr(x, 3)
    return x

def xor_single(ct):
    for k in range(256):
        pt = bytes(b ^ k for b in ct)
        if b"recovery" in pt or b"DS{" in pt:
            print(f"key=0x{k:02x}:", pt.decode(errors="replace"))

def xor_repeating(ct, known):
    key = bytes(a ^ b for a, b in zip(ct, known.encode()))
    print("key prefix:", key.decode(errors="replace"))
    pt = bytes(b ^ key[i % len(key)] for i, b in enumerate(ct))
    print(pt.decode(errors="replace"))

def proprietary(ct):
    pt = bytes(invert(b, i % 4) for i, b in enumerate(ct))
    print(pt.decode(errors="replace"))

def prng_bytes(seed, n):
    rng = JavaRandom(seed)
    out = bytearray()
    while len(out) < n:
        out += struct.pack(">i", rng.next_int())
    return bytes(out[:n])

def stream(minutes, ct_hex):
    ct = binascii.unhexlify(ct_hex)
    for hour in range(minutes // 60 - 48, minutes // 60 + 2):
        ks = prng_bytes(hour, len(ct))
        pt = bytes(a ^ b for a, b in zip(ct, ks))
        if pt.startswith(b"session:"):
            print(f"seed(hour)={hour}:", pt.decode(errors="replace"))
            return
    print("no seed matched — widen the window")

def token_fixed():
    print((binascii.hexlify(struct.pack(">i", JavaRandom(42).next_int()))).decode()[:6])

def token_hour(minutes):
    for hour in range(minutes // 60 - 48, minutes // 60 + 2):
        t = binascii.hexlify(struct.pack(">i", JavaRandom(hour).next_int())).decode()[:6]
        print(hour, t)

def token_counter(n):
    print(binascii.hexlify(hashlib.md5(f"siege-session-{n}".encode()).digest()).decode()[:6])

def otp(day):
    print(f"{JavaRandom(day).next_int_bound(1_000_000):06d}")

if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "xor-single":
        xor_single(binascii.unhexlify(sys.argv[2]))
    elif cmd == "xor-repeating":
        xor_repeating(binascii.unhexlify(sys.argv[2]), sys.argv[3])
    elif cmd == "proprietary":
        proprietary(binascii.unhexlify(sys.argv[2]))
    elif cmd == "stream":
        stream(int(sys.argv[2]), sys.argv[3])
    elif cmd == "token-fixed":
        token_fixed()
    elif cmd == "token-hour":
        token_hour(int(sys.argv[2]))
    elif cmd == "token-counter":
        token_counter(int(sys.argv[2]))
    elif cmd == "otp":
        otp(int(sys.argv[2]))
