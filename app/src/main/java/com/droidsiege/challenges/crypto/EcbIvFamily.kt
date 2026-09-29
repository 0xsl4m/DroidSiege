package com.droidsiege.challenges.crypto

import androidx.compose.runtime.Composable
import com.droidsiege.challenges.common.ConsoleChallengeScreen
import com.droidsiege.challenges.common.HexCodec
import com.droidsiege.challenges.common.KeystoreVault
import com.droidsiege.challenges.common.KitAction
import com.droidsiege.challenges.common.RawAes
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

private const val FLAG_L1 = "DS{crypto_ecbiv_L1_7a2e46}"
private const val FLAG_L2 = "DS{crypto_ecbiv_L2_d5f981}"
private const val FLAG_L3 = "DS{crypto_ecbiv_L3_38c1b7}"
private const val FLAG_L4 = "DS{crypto_ecbiv_L4_82ea6f}"

private const val DEMO_KEY_HEX = "00112233445566778899aabbccddeeff"

object EcbIvVault {
    fun ecbLines(secureMode: Boolean): List<Pair<String, String>> {
        val key = SecretKeySpec(HexCodec.fromHex(DEMO_KEY_HEX), "AES")
        val plaintext =
            "premium tier unlocked::" + if (!secureMode) FLAG_L1 else "[sealed by keystore]"
        val ct =
            if (!secureMode) {
                RawAes.ecbEncrypt(key, plaintext.toByteArray())
            } else {
                val ks = KeystoreVault.loadOrCreateKey("ecbiv_l1")
                RawAes.gcmEncrypt(ks, randomNonce(), plaintext.toByteArray())
            }
        return listOf(
            "DEV key (hex)" to DEMO_KEY_HEX,
            "Ciphertext (hex)" to HexCodec.toHex(ct),
            "Plaintext sample (same key, ECB)" to
                HexCodec.toHex(RawAes.ecbEncrypt(key, "siege-siege-siege!!!!".toByteArray())),
        )
    }

    private fun randomNonce() = ByteArray(12).also { SecureRandom().nextBytes(it) }

    fun cbcLines(secureMode: Boolean): List<Pair<String, String>> {
        val key = SecretKeySpec("iv_demo_key_2024".toByteArray(), "AES")
        val iv = "static-iv-static".toByteArray()
        val plaintext = "backup code::" + if (!secureMode) FLAG_L2 else "[keystore-sealed]"
        val ct =
            if (!secureMode) {
                RawAes.cbcEncrypt(key, iv, plaintext.toByteArray())
            } else {
                val ks = KeystoreVault.loadOrCreateKey("ecbiv_l2")
                RawAes.gcmEncrypt(ks, randomNonce(), plaintext.toByteArray())
            }
        return listOf(
            "Key (ascii)" to "iv_demo_key_2024",
            "IV (ascii)" to "static-iv-static",
            "Ciphertext (hex)" to HexCodec.toHex(ct),
        )
    }

    /** L3 — the in-app padding oracle. */
    fun probeToken(
        tokenHex: String,
        secureMode: Boolean,
    ): String {
        val key = SecretKeySpec("iv_demo_key_2024".toByteArray(), "AES")
        val iv = "static-iv-static".toByteArray()
        return try {
            val pt = RawAes.cbcDecrypt(key, iv, HexCodec.fromHex(tokenHex))
            if (secureMode) {
                // hardened builds answer identically for every input
                "token rejected"
            } else {
                "token accepted: ${String(pt)}"
            }
        } catch (boom: Exception) {
            if (secureMode) {
                "token rejected"
            } else {
                // the padding failure is distinguishable from other failures: an oracle
                if (boom.message?.contains("padding", ignoreCase = true) == true) {
                    "padding error"
                } else {
                    "block size error"
                }
            }
        }
    }

    fun oracleToken(): String {
        val key = SecretKeySpec("iv_demo_key_2024".toByteArray(), "AES")
        val iv = "static-iv-static".toByteArray()
        return HexCodec.toHex(RawAes.cbcEncrypt(key, iv, "recovery::${FLAG_L3}".toByteArray()))
    }

    fun gcmLines(secureMode: Boolean): List<Pair<String, String>> {
        val key =
            if (!secureMode) {
                SecretKeySpec(HexCodec.fromHex(DEMO_KEY_HEX), "AES")
            } else {
                KeystoreVault.loadOrCreateKey("ecbiv_l4")
            }
        // The vulnerability: the SAME nonce encrypts both messages.
        val nonce = ByteArray(12).also { if (!secureMode) it.fill(7) else SecureRandom().nextBytes(it) }
        val known = "welcome to siege wallet, dear customer!"
        val secret = if (!secureMode) FLAG_L4 else "[nonce-unique sealed]"
        val c1 = RawAes.gcmEncrypt(key, nonce, known.toByteArray())
        val c2 = RawAes.gcmEncrypt(key, nonce, secret.toByteArray())
        return listOf(
            "Welcome plaintext (public)" to known,
            "Welcome ciphertext (hex)" to HexCodec.toHex(c1),
            "Recovery ciphertext (hex)" to HexCodec.toHex(c2),
            "Nonce" to HexCodec.toHex(nonce),
            "Note" to "GCM blobs end with a 16-byte auth tag — strip it before XORing.",
        )
    }
}

class EcbIvL1Challenge : TieredChallenge(
    category = "crypto",
    slug = "ecbiv",
    level = Difficulty.EASY,
    title = "Pattern Block",
    brief = "The premium license is encrypted with the development AES configuration. " +
        "The parameters are printed next to the blob — the configuration is the lesson.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-1", "MASTG-TEST-0x20"),
    hints = listOf(
        "Look at the plaintext sample line: the repeated blocks repeat in the ciphertext too.",
        "Decrypt the blob with the DEV key — AES-128-ECB, no IV.",
        "cyberchef: AES decrypt, ECB, key from hex, output raw. The flag is the plaintext.",
    ),
    flag = FLAG_L1,
    learn = LearnContent(
        theory = "ECB encrypts identical blocks to identical ciphertext: structure " +
            "survives encryption, which is why the ECB penguin is famous. It also " +
            "provides no integrity — anyone can reorder or replay blocks.\n\n" +
            "Block ciphers must be used with a random-nonce authenticated mode — AES-GCM " +
            "is the modern default.",
        mastgRefs = listOf("MASVS-CRYPTO-1", "MASTG-TEST-0x20"),
        vulnerableSnippet = "Cipher.getInstance(\"AES\") // defaults to AES/ECB/PKCS5Padding!",
        fixSnippet = "Cipher.getInstance(\"AES/GCM/NoPadding\")\n" +
            "cipher.init(ENCRYPT_MODE, key, GCMParameterSpec(128, randomNonce()))",
        takeaway = "Cipher.getInstance(\"AES\") is ECB — the penguin is watching.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ConsoleChallengeScreen(secureMode = secureMode, lines = EcbIvVault.ecbLines(secureMode))
    }
}

class EcbIvL2Challenge : TieredChallenge(
    category = "crypto",
    slug = "ecbiv",
    level = Difficulty.MEDIUM,
    title = "Fixed IV",
    brief = "The backup code moved to CBC with an explicit IV. The key and IV are " +
        "printed below for support reproducibility.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-1", "MASTG-TEST-0x20"),
    hints = listOf(
        "CBC with a static IV leaks prefix equality — and this IV never changes.",
        "The parameters are all here: decrypt AES-128-CBC with the ASCII key and IV.",
        "The first block XORs with the IV; the rest chain — a crypto tool handles it.",
    ),
    flag = FLAG_L2,
    learn = LearnContent(
        theory = "A fixed IV turns CBC into a prefix oracle: identical plaintext " +
            "prefixes produce identical ciphertext prefixes, and reused (key, IV) pairs " +
            "erase CBC's randomness entirely. IVs must be random per message (and are " +
            "not secret, so printing them is fine — reusing them is not).\n\n" +
            "The durable fix is an authenticated mode with implicit nonces.",
        mastgRefs = listOf("MASVS-CRYPTO-1", "MASTG-TEST-0x20"),
        vulnerableSnippet = "val iv = \"static-iv-static\".toByteArray() // never changes\n" +
            "cipher.init(ENCRYPT_MODE, key, IvParameterSpec(iv))",
        fixSnippet = "val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }\n" +
            "cipher.init(ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))",
        takeaway = "An IV is a per-message value — static IVs unrandomize the cipher.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ConsoleChallengeScreen(secureMode = secureMode, lines = EcbIvVault.cbcLines(secureMode))
    }
}

class EcbIvL3Challenge : TieredChallenge(
    category = "crypto",
    slug = "ecbiv",
    level = Difficulty.HARD,
    title = "Token Oracle",
    brief = "The local token validator answers with different errors for bad padding " +
        "and bad content. Helpfully specific, that validator.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-1", "MASTG-TEST-0x20"),
    hints = listOf(
        "Collect the oracle token (ciphertext of a known recovery token) first.",
        "Modify the last block(s): a padding error vs an accepted/rejected answer tells you the last plaintext byte.",
        "The console in docs/solutions/crypto/ecbiv/tools/padding_oracle.py drives the attack — 2 bytes per round.",
    ),
    flag = FLAG_L3,
    learn = LearnContent(
        theory = "CBC without authentication plus error messages that distinguish " +
            "padding failures is the padding oracle: enough information to decrypt the " +
            "entire message byte by byte, no key required. The attack needs only the " +
            "distinct answers and a way to submit crafted ciphertexts.\n\n" +
            "The fix is not better messages — it is authenticated encryption (GCM), " +
            "where any modification fails the tag check identically.",
        mastgRefs = listOf("MASVS-CRYPTO-1", "MASTG-TEST-0x20"),
        vulnerableSnippet = "try { decrypt(ct) } catch (e: BadPaddingException) {\n" +
            "    return \"padding error\" // vs \"token rejected\" elsewhere",
        fixSnippet = "// AES-GCM: every tampered input fails identically:\n" +
            "catch (AEADBadTagException) { return \"token rejected\" }",
        takeaway = "Distinct failure messages are a decryption side channel.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ConsoleChallengeScreen(
            secureMode = secureMode,
            note = "Submit modified ciphertext hex to the validator and study the answers.",
            lines = listOf("Oracle token (hex)" to EcbIvVault.oracleToken()),
            probes = listOf(
                KitAction("Validate oracle token (sanity)") { _, secure ->
                    EcbIvVault.probeToken(EcbIvVault.oracleToken(), secure)
                },
            ),
        )
    }
}

class EcbIvL4Challenge : TieredChallenge(
    category = "crypto",
    slug = "ecbiv",
    level = Difficulty.INSANE,
    title = "Nonce Reuse",
    brief = "The recovery service reuses its message nonce for efficiency. You receive " +
        "a welcome message whose plaintext is public, and a recovery message. The " +
        "keystream is shared.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-1", "MASTG-TEST-0x20"),
    hints = listOf(
        "GCM is CTR mode: ciphertext = plaintext XOR keystream.",
        "keystream = welcomeCiphertext XOR welcomePlaintext (the blobs here are ct-only).",
        "recoveryPlaintext = recoveryCiphertext XOR keystream. The tooling pack computes it.",
    ),
    flag = FLAG_L4,
    learn = LearnContent(
        theory = "Reusing a nonce in GCM (or any CTR mode) is catastrophic: the same " +
            "keystream encrypts both messages, so XORing the ciphertexts cancels the " +
            "keystream — and a known plaintext reveals it outright. Nonce reuse also " +
            "destroys GCM's authentication, enabling forgeries.\n\n" +
            "Nonces must be unique per key, full stop — or generated by the mode itself " +
            "(random 96-bit per message is standard).",
        mastgRefs = listOf("MASVS-CRYPTO-1", "MASTG-TEST-0x20"),
        vulnerableSnippet = "val nonce = ByteArray(12).apply { fill(7) } // constant\n" +
            "val c1 = gcmEncrypt(key, nonce, welcome)\n" +
            "val c2 = gcmEncrypt(key, nonce, secret)",
        fixSnippet = "val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) } // per message",
        takeaway = "Nonce reuse converts AES-GCM into a one-time pad without the once.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ConsoleChallengeScreen(secureMode = secureMode, lines = EcbIvVault.gcmLines(secureMode))
    }
}
