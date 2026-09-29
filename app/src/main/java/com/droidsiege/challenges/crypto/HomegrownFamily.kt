package com.droidsiege.challenges.crypto

import androidx.compose.runtime.Composable
import com.droidsiege.challenges.common.ConsoleChallengeScreen
import com.droidsiege.challenges.common.HexCodec
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.challenges.common.XorCipher
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import java.util.Random

private const val FLAG_L1 = "DS{crypto_homegrown_L1_b74d02}"
private const val FLAG_L2 = "DS{crypto_homegrown_L2_61f3a8}"
private const val FLAG_L3 = "DS{crypto_homegrown_L3_ce5914}"
private const val FLAG_L4 = "DS{crypto_homegrown_L4_27d8b6}"

/** L3 — the "proprietary multi-round transform". Deterministic and invertible. */
object ProprietaryVault {
    fun transform(
        byte: Int,
        round: Int,
    ): Int {
        var x = byte and 0xFF
        repeat(round + 1) { r ->
            x = ((x shl 3) or (x ushr 5)) and 0xFF
            x = x xor (0x5F + r)
            x = (x + 0x21 + r) and 0xFF
        }
        return x
    }

    fun invert(
        byte: Int,
        round: Int,
    ): Int {
        var x = byte and 0xFF
        for (r in round downTo 0) {
            x = (x - 0x21 - r) and 0xFF
            x = x xor (0x5F + r)
            x = ((x ushr 3) or (x shl 5)) and 0xFF
        }
        return x
    }

    fun encrypt(plaintext: ByteArray): ByteArray =
        ByteArray(plaintext.size) { i ->
            ProprietaryVault.transform(plaintext[i].toInt(), i % 4).toByte()
        }

    fun decrypt(ciphertext: ByteArray): ByteArray =
        ByteArray(ciphertext.size) { i ->
            ProprietaryVault.invert(ciphertext[i].toInt(), i % 4).toByte()
        }
}

object HomegrownVault {
    fun singleByteXor(secureMode: Boolean): String {
        val plaintext = "recovery: " + if (!secureMode) FLAG_L1 else "[AEAD-sealed]"
        return HexCodec.toHex(XorCipher.xorSingle(plaintext.toByteArray(), 0x5A))
    }

    fun repeatingKeyXor(secureMode: Boolean): String {
        val plaintext = "vault note: " + if (!secureMode) FLAG_L2 else "[AEAD-sealed]"
        return HexCodec.toHex(XorCipher.xorRepeating(plaintext.toByteArray(), "SIEGE".toByteArray()))
    }

    fun proprietary(secureMode: Boolean): String {
        val plaintext = "transfer pin: " + if (!secureMode) FLAG_L3 else "[AEAD-sealed]"
        return HexCodec.toHex(ProprietaryVault.encrypt(plaintext.toByteArray()))
    }

    fun prngStream(
        secureMode: Boolean,
        issuedAtMinutes: Long,
    ): Pair<Long, String> {
        val plaintext = "session: " + if (!secureMode) FLAG_L4 else "[CSPRNG-sealed]"
        val data = plaintext.toByteArray()
        val keystream = ByteArray(data.size)
        if (!secureMode) {
            // seed = minutes since epoch, truncated to the hour — a "sortable session id"
            val prng = Random(issuedAtMinutes / 60)
            prng.nextBytes(keystream)
        } else {
            java.security.SecureRandom().nextBytes(keystream)
        }
        val cipherText = ByteArray(data.size) { i -> (data[i].toInt() xor keystream[i].toInt()).toByte() }
        return issuedAtMinutes to HexCodec.toHex(cipherText)
    }
}

private fun Random.nextBytes(bytes: ByteArray) {
    var i = 0
    while (i < bytes.size) {
        val value = nextInt()
        val chunk = java.nio.ByteBuffer.allocate(4).putInt(value).array()
        chunk.forEachIndexed { offset, b ->
            if (i < bytes.size) bytes[i++] = b
        }
    }
}

class HomegrownL1Challenge : TieredChallenge(
    category = "crypto",
    slug = "homegrown",
    level = Difficulty.EASY,
    title = "Single Byte",
    brief = "The transfer note below is 'encrypted' with the house cipher. The cipher " +
        "has exactly 256 possible keys.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-1", "MASTG-TEST-0x26"),
    hints = listOf(
        "256 keys is not key space, it is a short list.",
        "Brute every byte 0x00–0xFF; the correct one yields printable text starting with 'recovery:'.",
        "docs/solutions/crypto/homegrown/tools/brute_xor.py does it in milliseconds.",
    ),
    flag = FLAG_L1,
    learn = LearnContent(
        theory = "XOR with a single byte has 256 possible keys — brute force takes " +
            "microseconds, and the plaintext is recognized by its printability. It is " +
            "obfuscation, not encryption.\n\n" +
            "Custom symmetric designs fail the first time someone writes a loop. Use " +
            "vetted AEAD (AES-GCM / ChaCha20-Poly1305).",
        mastgRefs = listOf("MASVS-CRYPTO-1", "MASTG-TEST-0x26"),
        vulnerableSnippet = "fun obfuscate(data: ByteArray) = data.map { it.toInt() xor 0x5A }.toByteArray()",
        fixSnippet = "// vetted, one-liner, actually secure:\n" +
            "Cipher.getInstance(\"AES/GCM/NoPadding\")",
        takeaway = "A cipher you can brute-force by hand is a costume, not a cipher.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ConsoleChallengeScreen(
            secureMode = secureMode,
            lines = listOf("Encrypted note (hex)" to HomegrownVault.singleByteXor(secureMode)),
        )
    }
}

class HomegrownL2Challenge : TieredChallenge(
    category = "crypto",
    slug = "homegrown",
    level = Difficulty.MEDIUM,
    title = "Repeating Key",
    brief = "The note cipher upgraded to a multi-byte key. The flag format is publicly " +
        "documented — and that is the cipher's undoing.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-1", "MASTG-TEST-0x26"),
    hints = listOf(
        "Repeating-key XOR with known plaintext: the note starts with 'vault note: ' and the flag starts with 'DS{'.",
        "Six known plaintext bytes against a 5-byte key crack most of it; the rest falls to printability.",
        "tools/known_plaintext_xor.py reconstructs the key from the prefix alone.",
    ),
    flag = FLAG_L2,
    learn = LearnContent(
        theory = "Repeating-key XOR dies to known plaintext: every known byte reveals " +
            "one key byte, and structured formats (like a fixed flag prefix) provide " +
            "plenty. Without known plaintext, frequency analysis of long messages " +
            "achieves the same.\n\n" +
            "XOR 'ciphers' have no business protecting data in 2026 — AEAD or nothing.",
        mastgRefs = listOf("MASVS-CRYPTO-1", "MASTG-TEST-0x26"),
        vulnerableSnippet = "fun xor(data: ByteArray, key: ByteArray) =\n" +
            "    ByteArray(data.size) { i -> (data[i] xor key[i % key.size]).toByte() }",
        fixSnippet = "// the standard library, used correctly:\n" +
            "Cipher.getInstance(\"AES/GCM/NoPadding\")",
        takeaway = "Known plaintext plus XOR equals the key.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ConsoleChallengeScreen(
            secureMode = secureMode,
            lines = listOf("Encrypted note (hex)" to HomegrownVault.repeatingKeyXor(secureMode)),
        )
    }
}

class HomegrownL3Challenge : TieredChallenge(
    category = "crypto",
    slug = "homegrown",
    level = Difficulty.HARD,
    title = "Proprietary Rounds",
    brief = "This transform survived a decade in production: rotation, masks and " +
        "position-dependent rounds. Marketing calls it proprietary encryption.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-1", "MASTG-TEST-0x26"),
    hints = listOf(
        "Decompile ProprietaryVault — the transform and its parameters are fully visible.",
        "It is position-dependent: byte i uses round i % 4; each round is rotl(3), xor mask, add bias.",
        "Invert the operations right-to-left, or run tools/invert_proprietary.py.",
    ),
    flag = FLAG_L3,
    learn = LearnContent(
        theory = "Custom designs are broken by reading them: with the algorithm in " +
            "hand, inverting a deterministic byte transform is homework, not " +
            "cryptanalysis. Kerckhoffs's principle is 140 years old — security must " +
            "survive full knowledge of the algorithm.\n\n" +
            "'Proprietary' is a property of the marketing department, not of the cipher.",
        mastgRefs = listOf("MASVS-CRYPTO-1", "MASTG-TEST-0x26"),
        vulnerableSnippet = "repeat(round + 1) { r ->\n" +
            "    x = rotl(x, 3); x = x xor (0x5F + r); x = (x + 0x21 + r) and 0xFF\n" +
            "}",
        fixSnippet = "// vetted AEAD; the algorithm being public is the point:\n" +
            "Cipher.getInstance(\"AES/GCM/NoPadding\")",
        takeaway = "Proprietary means unreviewed — and unreviewed means broken.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ConsoleChallengeScreen(
            secureMode = secureMode,
            lines = listOf(
                "Encrypted transfer pin (hex)" to HomegrownVault.proprietary(secureMode),
                "Transform" to "ProprietaryVault.transform() — 4 position-dependent rounds",
            ),
        )
    }
}

class HomegrownL4Challenge : TieredChallenge(
    category = "crypto",
    slug = "homegrown",
    level = Difficulty.INSANE,
    title = "Predictable Stream",
    brief = "The session cipher generates its keystream from java.util.Random, seeded " +
        "with the session hour for sortability. The ciphertext and its issue time are " +
        "below.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-1", "MASTG-TEST-0x26"),
    hints = listOf(
        "java.util.Random is a 48-bit LCG — with a known seed the entire stream is deterministic.",
        "The seed is issuedAtMinutes / 60. Brute the hours around the shown issue time.",
        "tools/predict_stream.py tries every hour in a window and matches on the 'session: ' prefix.",
    ),
    flag = FLAG_L4,
    learn = LearnContent(
        theory = "java.util.Random is not a random number generator in any security " +
            "sense: it is a linear congruential generator whose entire future output is " +
            "determined by its seed. Predictable seeds (time, counters) make the " +
            "keystream a lookup table.\n\n" +
            "Keystreams and tokens demand SecureRandom — the OS entropy pool, never a " +
            "sortable clock.",
        mastgRefs = listOf("MASVS-CRYPTO-1", "MASTG-TEST-0x26"),
        vulnerableSnippet = "val prng = Random(issuedAtMinutes / 60)\n" +
            "prng.nextBytes(keystream) // deterministic forever",
        fixSnippet = "SecureRandom().nextBytes(keystream) // OS entropy",
        takeaway = "Time-seeded randomness is just a slower lookup table.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val issuedAt = System.currentTimeMillis() / 60_000
        val (minutes, ciphertext) = HomegrownVault.prngStream(secureMode, issuedAt)
        ConsoleChallengeScreen(
            secureMode = secureMode,
            lines = listOf(
                "Ciphertext (hex)" to ciphertext,
                "Issued at (minutes since epoch)" to minutes.toString(),
                "Cipher" to "XOR keystream from java.util.Random(seed = hour)",
            ),
        )
    }
}
