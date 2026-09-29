package com.droidsiege.challenges.crypto

import android.content.Context
import androidx.compose.runtime.Composable
import com.droidsiege.challenges.common.ConsoleChallengeScreen
import com.droidsiege.challenges.common.HexCodec
import com.droidsiege.challenges.common.KeystoreVault
import com.droidsiege.challenges.common.KitAction
import com.droidsiege.challenges.common.RawAes
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.challenges.common.WeakKdf
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

private const val FLAG_L1 = "DS{crypto_kdf_L1_93e60c}"
private const val FLAG_L2 = "DS{crypto_kdf_L2_48a2f5}"
private const val FLAG_L3 = "DS{crypto_kdf_L3_f1c74b}"
private const val FLAG_L4 = "DS{crypto_kdf_L4_5d09e3}"

object KdfVault {
    private fun pref(context: Context) = context.getSharedPreferences("siege_kdf_prefs", Context.MODE_PRIVATE)

    fun staticSaltLines(secureMode: Boolean): List<Pair<String, String>> {
        val password = "siege-master-2024"
        val salt = "droidsiege"
        val key = SecretKeySpec(WeakKdf.md5((salt + password).toByteArray()), "AES")
        val iv = ByteArray(16).also { it.fill(0x11) }
        val plaintext = "master code: " + if (!secureMode) FLAG_L1 else "[kdf-strong-sealed]"
        val ct =
            if (!secureMode) {
                RawAes.cbcEncrypt(key, iv, plaintext.toByteArray())
            } else {
                val ks = KeystoreVault.loadOrCreateKey("kdf_l1")
                val iv12 = ByteArray(12).also { SecureRandom().nextBytes(it) }
                return listOf(
                    "Parameters" to "Argon2-class KDF (Keystore-backed key), random nonce",
                    "Sealed blob (b64)" to android.util.Base64.encodeToString(
                        iv12 + RawAes.gcmEncrypt(ks, iv12, plaintext.toByteArray()),
                        android.util.Base64.NO_WRAP,
                    ),
                )
            }
        return listOf(
            "KDF" to "MD5(salt + password)",
            "Password" to password,
            "Salt" to salt,
            "Ciphertext (hex, AES-CBC iv=1111...)" to HexCodec.toHex(ct),
        )
    }

    fun weakPbkdf2Lines(secureMode: Boolean): List<Pair<String, String>> {
        val salt = "s1ege-salt-2026".toByteArray()
        val iterations = 100
        val password = "siege123"
        val key =
            if (!secureMode) {
                SecretKeySpec(WeakKdf.pbkdf2(password.toCharArray(), salt, iterations).copyOf(16), "AES")
            } else {
                KeystoreVault.loadOrCreateKey("kdf_l2")
            }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val plaintext = "safe code: " + if (!secureMode) FLAG_L2 else "[PBKDF2-600k-sealed]"
        val ct = RawAes.gcmEncrypt(key, iv, plaintext.toByteArray())
        return listOf(
            "KDF" to "PBKDF2WithHmacSHA256",
            "Iterations" to iterations.toString(),
            "Salt (ascii)" to String(salt),
            "Ciphertext (b64, iv||ct)" to android.util.Base64.encodeToString(iv + ct, android.util.Base64.NO_WRAP),
            "Password policy" to if (!secureMode) "user-chosen, 8 chars" else "vault-generated, 24 chars",
        )
    }

    fun usernameSaltLines(secureMode: Boolean): List<Pair<String, String>> {
        val username = "player1"
        val salt = MessageDigest.getInstance("SHA-256").digest(username.toByteArray()).copyOf(16)
        val password = "letmein"
        val key =
            if (!secureMode) {
                SecretKeySpec(WeakKdf.pbkdf2(password.toCharArray(), salt, 1_000).copyOf(16), "AES")
            } else {
                KeystoreVault.loadOrCreateKey("kdf_l3")
            }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val plaintext = "deposit code: " + if (!secureMode) FLAG_L3 else "[keystore-sealed]"
        val ct = RawAes.gcmEncrypt(key, iv, plaintext.toByteArray())
        return listOf(
            "Username" to username,
            "KDF" to "PBKDF2-SHA256, 1,000 iterations",
            "Salt derivation" to "SHA-256(username)[..16]",
            "Ciphertext (b64, iv||ct)" to android.util.Base64.encodeToString(iv + ct, android.util.Base64.NO_WRAP),
        )
    }

    fun strongKdfWithCache(
        context: Context,
        secureMode: Boolean,
    ): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val key =
            if (!secureMode) {
                SecretKeySpec(WeakKdf.pbkdf2("correct-horse-battery".toCharArray(), salt, 210_000).copyOf(16), "AES")
            } else {
                KeystoreVault.loadOrCreateKey("kdf_l4")
            }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val plaintext = "vault code: $FLAG_L4"
        val sealed = RawAes.gcmEncrypt(key, iv, plaintext.toByteArray())
        val sealedNote = "sealed blob: ${sealed.size} bytes"
        if (!secureMode) {
            // "session convenience": the derived key is cached for quick re-unlock
            pref(context).edit()
                .putString("kdf_cache_salt", HexCodec.toHex(salt))
                .putString("kdf_cache_key", HexCodec.toHex(key.encoded))
                .commit()
        } else {
            pref(context).edit().clear().commit()
        }
        return if (!secureMode) {
            "Vault unlocked ($sealedNote). Key cached: shared_prefs/siege_kdf_prefs.xml"
        } else {
            "Vault unlocked ($sealedNote). Keys stay inside the Keystore — nothing cached."
        }
    }
}

class KdfL1Challenge : TieredChallenge(
    category = "crypto",
    slug = "kdf",
    level = Difficulty.EASY,
    title = "MD5 Master",
    brief = "The master code is protected by a key derived from the recovery phrase — " +
        "which support reads to users over the phone every day.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-2", "MASTG-TEST-0x27"),
    hints = listOf(
        "MD5 is instant — precomputation is trivial once the inputs are known.",
        "key = MD5(\"droidsiege\" + \"siege-master-2024\"); the cipher is AES-128-CBC with IV 0x11 repeated.",
        "Any cyber hash tool gives the key; then AES-CBC decrypt the hex blob.",
    ),
    flag = FLAG_L1,
    learn = LearnContent(
        theory = "MD5 is not a key derivation function: it is instant, unkeyed and " +
            "unbroken only in the sense that nobody bothers. When the password is " +
            "human-chosen and printed on the same screen, MD5(password) is just a " +
            "slightly rotated password.\n\n" +
            "Passwords need memory-hard KDFs (Argon2, or PBKDF2 at high iterations) " +
            "with random salts — and the password itself must not be support-readable.",
        mastgRefs = listOf("MASVS-CRYPTO-2", "MASTG-TEST-0x27"),
        vulnerableSnippet = "val key = md5((salt + password).toByteArray())",
        fixSnippet = "Argon2id(password, randomSalt, iterations = 3, memory = 64.MiB)\n" +
            "// or PBKDF2WithHmacSHA256 at 600k iterations",
        takeaway = "MD5 hides nothing, from anyone, for any amount of time.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ConsoleChallengeScreen(secureMode = secureMode, lines = KdfVault.staticSaltLines(secureMode))
    }
}

class KdfL2Challenge : TieredChallenge(
    category = "crypto",
    slug = "kdf",
    level = Difficulty.MEDIUM,
    title = "Hundred Rounds",
    brief = "The safe code now uses PBKDF2 — real KDF this time. The parameters are " +
        "printed below. The dictionary is four lines long.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-2", "MASTG-TEST-0x27"),
    hints = listOf(
        "PBKDF2 cost is linear in iterations — 100 rounds is 6000x cheaper than the 600k standard.",
        "The password is user-chosen and weak: try the wordlist in tools/weak_passwords.txt.",
        "tools/brute_pbkdf2.py rebuilds the key for each candidate and AES-GCM-decrypts.",
    ),
    flag = FLAG_L2,
    learn = LearnContent(
        theory = "PBKDF2 with a low iteration count is a speed bump at driving speed: " +
            "modern GPUs measure 100 rounds in microseconds, so a small dictionary " +
            "against a weak password completes before the coffee cools.\n\n" +
            "KDF parameters have a shelf life — follow current guidance (OWASP: 600k " +
            "PBKDF2-SHA256 iterations today) and pair with generated passwords.",
        mastgRefs = listOf("MASVS-CRYPTO-2", "MASTG-TEST-0x27"),
        vulnerableSnippet = "PBEKeySpec(password, salt, 100, 256)",
        fixSnippet = "PBEKeySpec(generatedPassword, randomSalt, 600_000, 256)\n" +
            "// password generated by the vault, never typed",
        takeaway = "A KDF is only as strong as its iteration count and the password it feeds on.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ConsoleChallengeScreen(secureMode = secureMode, lines = KdfVault.weakPbkdf2Lines(secureMode))
    }
}

class KdfL3Challenge : TieredChallenge(
    category = "crypto",
    slug = "kdf",
    level = Difficulty.HARD,
    title = "Guessable Salt",
    brief = "Salts are now per-user — derived from the username, so support can " +
        "reconstruct them. You get the username too.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-2", "MASTG-TEST-0x27"),
    hints = listOf(
        "The salt must be random, not derivable — a username is not entropy.",
        "salt = SHA-256(\"player1\")[..16]; the password is in the weak wordlist.",
        "tools/brute_pbkdf2.py supports --username mode.",
    ),
    flag = FLAG_L3,
    learn = LearnContent(
        theory = "A salt's only job is uniqueness — and a username-derived salt fails " +
            "the part that matters for targeted attacks: anyone attacking player1 " +
            "computes the same salt and runs the same dictionary as everyone else. " +
            "Salts don't need to be secret, but they must be unpredictable and unique.\n\n" +
            "Generate salts with SecureRandom and store them next to the record.",
        mastgRefs = listOf("MASVS-CRYPTO-2", "MASTG-TEST-0x27"),
        vulnerableSnippet = "val salt = sha256(username).copyOf(16)",
        fixSnippet = "val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }",
        takeaway = "Derivable salts make every user's dictionary attack a local attack.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ConsoleChallengeScreen(secureMode = secureMode, lines = KdfVault.usernameSaltLines(secureMode))
    }
}

class KdfL4Challenge : TieredChallenge(
    category = "crypto",
    slug = "kdf",
    level = Difficulty.INSANE,
    title = "Convenience Cache",
    brief = "This vault uses PBKDF2 at 210,000 iterations with a random salt — " +
        "textbook. Unlock it and see where the convenience layer puts things.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-2", "M9", "MASTG-TEST-0x27"),
    hints = listOf(
        "The KDF is unbreakable; that was never the weak link.",
        "Unlock the vault, then read shared_prefs/siege_kdf_prefs.xml.",
        "The derived key is cached in hex — the ciphertext is useless without it, and useless with it.",
    ),
    flag = FLAG_L4,
    learn = LearnContent(
        theory = "Strong cryptography dies at the convenience layer: a derived key " +
            "cached in prefs for 'fast re-unlock' hands the attacker the result of " +
            "210,000 iterations for free. Every hardening step before the cache is " +
            "decorative.\n\n" +
            "Derived keys live in memory for the session or inside the Keystore — " +
            "never in storage, logs or caches.",
        mastgRefs = listOf("MASVS-CRYPTO-2", "MASTG-TEST-0x27"),
        vulnerableSnippet = "prefs.putString(\"kdf_cache_key\", hex(derivedKey)) // 'convenience'",
        fixSnippet = "// the key stays in the Keystore or dies with the session",
        takeaway = "The weakest link is wherever the key rests longest.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ConsoleChallengeScreen(
            secureMode = secureMode,
            note = "Unlock the vault and audit the convenience layer.",
            probes = listOf(
                KitAction("Unlock vault (strong KDF)") { ctx, secure -> KdfVault.strongKdfWithCache(ctx, secure) },
            ),
        )
    }
}
