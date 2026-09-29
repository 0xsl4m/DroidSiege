package com.droidsiege.challenges.common

import com.droidsiege.challenges.crypto.ProprietaryVault
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

class ChallengeCryptoTest {
    @Test
    fun hexRoundTrips() {
        val bytes = byteArrayOf(0x00, 0x7f, (0x80).toByte(), (0xff).toByte())
        assertThat(HexCodec.fromHex(HexCodec.toHex(bytes))).isEqualTo(bytes)
    }

    @Test
    fun javaRandomMatchesTheSpecLcg() {
        // java.util.Random(0).nextInt() is specified behavior: -1155484576.
        val value = java.util.Random(0L).nextInt()
        assertThat(value).isEqualTo(-1155484576)
        val hex = HexCodec.toHex(java.nio.ByteBuffer.allocate(4).putInt(value).array())
        assertThat(hex).isEqualTo("bb20b460") // pins the tooling-pack parity
    }

    @Test
    fun xorSingleIsReversibleAndBruteable() {
        val secret = "recovery: DS{crypto_homegrown_L1_b74d02}"
        val cipher = XorCipher.xorSingle(secret.toByteArray(), 0x5A)
        assertThat(XorCipher.xorSingle(cipher, 0x5A).toString(Charsets.UTF_8)).isEqualTo(secret)
        val brute = (0..255).firstNotNullOfOrNull { k ->
            val candidate = XorCipher.xorSingle(cipher, k.toByte())
            candidate.toString(Charsets.UTF_8).takeIf { it.startsWith("recovery: DS{") }
        }
        assertThat(brute).isEqualTo(secret)
    }

    @Test
    fun xorRepeatingDiesToKnownPlaintext() {
        val key = "SIEGE".toByteArray()
        val known = "vault note: "
        val cipher = XorCipher.xorRepeating(known.toByteArray(), key)
        val recovered = XorCipher.xorRepeating(cipher, known.toByteArray())
        assertThat(recovered.copyOf(key.size)).isEqualTo(key)
    }

    @Test
    fun proprietaryTransformInvertsItself() {
        val secret = "transfer pin: DS{crypto_homegrown_L3_ce5914}"
        val data = secret.toByteArray()
        val cipher = ProprietaryVault.encrypt(data)
        assertThat(ProprietaryVault.decrypt(cipher)).isEqualTo(data)
    }

    @Test
    fun nonceReuseLetsKeystreamRecoveryWork() {
        val key = SecretKeySpec(HexCodec.fromHex("00112233445566778899aabbccddeeff"), "AES")
        val nonce = ByteArray(12).also { it.fill(7) }
        val known = "welcome to siege wallet, dear customer!"
        val secret = "DS{crypto_ecbiv_L4_82ea6f}"
        val c1 = RawAes.gcmEncrypt(key, nonce, known.toByteArray())
        val c2 = RawAes.gcmEncrypt(key, nonce, secret.toByteArray())
        // GCM ciphertext includes the trailing 16-byte tag; strip it.
        val ct1 = c1.copyOfRange(0, c1.size - 16)
        val ct2 = c2.copyOfRange(0, c2.size - 16)
        val keystream = ct1.mapIndexed { i, b -> (b.toInt() xor known[i].code).toByte() }.toByteArray()
        val recovered = ct2.mapIndexed { i, b -> (b.toInt() xor keystream[i].toInt()).toByte() }
            .toByteArray()
            .toString(Charsets.UTF_8)
        assertThat(recovered).isEqualTo(secret)
    }

    @Test
    fun randomNextIntBoundFollowsTheJavaSpec() {
        // java.util.Random.nextInt(bound) draws next(31) with rejection for non-powers
        // of two; pin the spec algorithm here so the python tooling stays in parity.
        fun nextIntBound(
            seed: Long,
            bound: Int,
        ): Int {
            var s = (seed xor 0x5DEECE66D) and ((1L shl 48) - 1)

            fun next(bits: Int): Int {
                s = (s * 0x5DEECE66D + 0xB) and ((1L shl 48) - 1)
                return (s ushr (48 - bits)).toInt()
            }
            return if (bound and -bound == bound) {
                (bound * next(31)) ushr 31
            } else {
                while (true) {
                    val bits = next(31)
                    val value = bits % bound
                    // java re-draws when bits - val + (bound - 1) overflows int
                    val overflowCheck = (bits.toLong() - value + (bound - 1L)) and 0xFFFFFFFFL
                    if (overflowCheck < 0x80000000L) {
                        return value
                    }
                }
                @Suppress("UNREACHABLE_CODE")
                error("unreachable")
            }
        }
        val fromApi = java.util.Random(7L).nextInt(1_000_000)
        assertThat(nextIntBound(7L, 1_000_000)).isEqualTo(fromApi)
        assertThat(fromApi).isIn(0 until 1_000_000)
    }

    @Test
    fun keystreamFillIsBigEndianIntChunks() {
        // The L4 tooling pack packs nextInt() big-endian; pin the byte order.
        val keystream = ByteArray(4)
        val prng = java.util.Random(0L)
        var i = 0
        while (i < keystream.size) {
            java.nio.ByteBuffer
                .allocate(4)
                .putInt(prng.nextInt())
                .array()
                .forEach { b ->
                    if (i < keystream.size) keystream[i++] = b
                }
        }
        assertThat(HexCodec.toHex(keystream)).isEqualTo("bb20b460")
    }

    @Test
    fun sealedBoxRoundTrips() {
        val key = javax.crypto.spec.SecretKeySpec(
            HexCodec.fromHex("00112233445566778899aabbccddeeff"),
            "AES",
        )
        val secret = "DS{crypto_hardcoded_L1_41f7c2}"
        val sealed = SealedBox.seal(key, secret.toByteArray())
        // blob layout: 12-byte nonce ++ ciphertext(++ tag)
        assertThat(sealed.size).isAtLeast(12 + 16 + secret.length)
        assertThat(SealedBox.unseal(key, sealed).toString(Charsets.UTF_8)).isEqualTo(secret)
    }

    @Test
    fun weakKdfIsDeterministicAndWeak() {
        val a = WeakKdf.pbkdf2("siege123".toCharArray(), "s1ege-salt-2026".toByteArray(), 100)
        val b = WeakKdf.pbkdf2("siege123".toCharArray(), "s1ege-salt-2026".toByteArray(), 100)
        assertThat(a).isEqualTo(b)
        assertThat(WeakKdf.md5("droidsiegesiege-master-2024".toByteArray()))
            .isEqualTo(WeakKdf.md5("droidsiegesiege-master-2024".toByteArray()))
    }
}
