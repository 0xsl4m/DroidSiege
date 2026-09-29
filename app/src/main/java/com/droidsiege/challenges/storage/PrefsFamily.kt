package com.droidsiege.challenges.storage

import android.content.Context
import androidx.compose.runtime.Composable
import com.droidsiege.challenges.common.ActionChallengeScreen
import com.droidsiege.challenges.common.KeystoreVault
import com.droidsiege.challenges.common.KitAction
import com.droidsiege.challenges.common.RawAes
import com.droidsiege.challenges.common.SealedBox
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

/**
 * The wallet "remember me" store. Each tier persists the session secret a little
 * differently; find where it ends up on disk.
 */
private const val PREFS_NAME = "siege_wallet_prefs"
private const val FLAG_L1 = "DS{storage_prefs_L1_3f9a2c}"
private const val FLAG_L2 = "DS{storage_prefs_L2_71c4e8}"
private const val FLAG_L3 = "DS{storage_prefs_L3_b6d21a}"
private const val FLAG_L4 = "DS{storage_prefs_L4_8e0f43}"

internal object PrefsVault {
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Realistic "encrypt" helper: base64 is not encryption, but apps ship it daily. */
    private fun pseudoEncrypt(value: String): String =
        android.util.Base64.encodeToString(value.toByteArray(), android.util.Base64.NO_WRAP)

    // L3: the "key material" ships as a plain Kotlin constant, as it does in plenty of shipped apps.
    private fun derivedKey(): ByteArray {
        val secret = "droidsiege_pref_secret_2024"
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(secret.toByteArray())
        return digest.copyOf(16)
    }

    // L4: EncryptedSharedPreferences-style wrapper, but the master key material is also
    // mirrored into plain prefs so support tooling can recover it. That mirror is the bug.

    fun saveSessionPlain(
        context: Context,
        secureMode: Boolean,
    ): String =
        if (!secureMode) {
            prefs(context).edit().putString("session_token", FLAG_L1).commit()
            "Session saved. Sync finished — everything stored."
        } else {
            prefs(context).edit().remove("session_token").commit()
            "Session kept in memory only. Nothing sensitive hits the disk."
        }

    fun saveSessionEncoded(
        context: Context,
        secureMode: Boolean,
    ): String =
        if (!secureMode) {
            prefs(context).edit().putString("encrypted_flag", pseudoEncrypt(FLAG_L2)).commit()
            "Encrypted session saved."
        } else {
            val sealed =
                SealedBox.seal(KeystoreVault.loadOrCreateKey("prefs_l2"), FLAG_L2.toByteArray())
            prefs(context).edit()
                .putString(
                    "encrypted_flag_iv",
                    android.util.Base64.encodeToString(sealed.copyOf(12), android.util.Base64.NO_WRAP),
                )
                .putString(
                    "encrypted_flag",
                    android.util.Base64.encodeToString(
                        sealed.copyOfRange(12, sealed.size),
                        android.util.Base64.NO_WRAP,
                    ),
                )
                .commit()
            "Session sealed with a Keystore key."
        }

    fun saveSessionDerived(
        context: Context,
        secureMode: Boolean,
    ): String =
        if (!secureMode) {
            val key = SecretKeySpec(derivedKey(), "AES")
            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
            val ct = RawAes.gcmEncrypt(key, iv, FLAG_L3.toByteArray())
            prefs(context).edit()
                .putString("derived_iv", android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP))
                .putString("derived_flag", android.util.Base64.encodeToString(ct, android.util.Base64.NO_WRAP))
                .commit()
            "Encrypted session saved with the release key."
        } else {
            val sealed =
                SealedBox.seal(KeystoreVault.loadOrCreateKey("prefs_l3"), FLAG_L3.toByteArray())
            prefs(context).edit()
                .putString(
                    "derived_iv",
                    android.util.Base64.encodeToString(sealed.copyOf(12), android.util.Base64.NO_WRAP),
                )
                .putString(
                    "derived_flag",
                    android.util.Base64.encodeToString(
                        sealed.copyOfRange(12, sealed.size),
                        android.util.Base64.NO_WRAP,
                    ),
                )
                .commit()
            "Session sealed with a non-exportable Keystore key."
        }

    fun saveSessionMasterKeyed(
        context: Context,
        secureMode: Boolean,
    ): String =
        if (!secureMode) {
            // Weak master key spec: 128-bit, mirrored to prefs for "support recovery".
            val masterBytes = "droidsiege-mk!".toByteArray().copyOf(16)
            val key = SecretKeySpec(masterBytes, "AES")
            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
            val ct = RawAes.gcmEncrypt(key, iv, FLAG_L4.toByteArray())
            prefs(context).edit()
                .putString(
                    "master_key_hint",
                    android.util.Base64.encodeToString(masterBytes, android.util.Base64.NO_WRAP),
                )
                .putString("master_iv", android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP))
                .putString("vault_flag", android.util.Base64.encodeToString(ct, android.util.Base64.NO_WRAP))
                // The side cache the table warns about:
                .putString("support_cache", FLAG_L4)
                .commit()
            "Vault secured with EncryptedSharedPreferences. Support cache refreshed."
        } else {
            val sealed =
                SealedBox.seal(KeystoreVault.loadOrCreateKey("prefs_l4_master"), FLAG_L4.toByteArray())
            prefs(context).edit()
                .putString(
                    "master_iv",
                    android.util.Base64.encodeToString(sealed.copyOf(12), android.util.Base64.NO_WRAP),
                )
                .putString(
                    "vault_flag",
                    android.util.Base64.encodeToString(
                        sealed.copyOfRange(12, sealed.size),
                        android.util.Base64.NO_WRAP,
                    ),
                )
                .remove("support_cache")
                .commit()
            "Vault sealed with StrongBox-class key. No support cache kept."
        }
}

private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

class PrefsL1Challenge : TieredChallenge(
    category = "storage",
    slug = "prefs",
    level = Difficulty.EASY,
    title = "Remember Me",
    brief = "Wallet saves your session so you stay signed in. The sync already ran on this " +
        "device — find where the session token was persisted and read it back.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "MASTG-TEST-0x50"),
    hints = listOf(
        "SharedPreferences live in /data/data/com.droidsiege/shared_prefs/ as plain XML.",
        "On a debug build: run-as com.droidsiege cat shared_prefs/siege_wallet_prefs.xml",
        "Look for the session_token entry.",
    ),
    flag = FLAG_L1,
    learn = LearnContent(
        theory = "SharedPreferences are XML files inside the app's private directory. The " +
            "sandbox protects them from other apps, but not from the device owner: on a " +
            "debuggable build `run-as` reads them, on a rooted device they are one `cat` " +
            "away, and backups can carry them off-device.\n\n" +
            "Writing long-lived secrets there means anyone with the device (or a backup) " +
            "owns the session.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASTG-TEST-0x50", "MASTG-TEST-0x55"),
        vulnerableSnippet = "prefs.edit()\n" +
            "    .putString(\"session_token\", sessionToken)\n" +
            "    .apply()",
        fixSnippet = "// Keep the token in memory, or seal it first:\n" +
            "val key = keystoreKey(\"session\")\n" +
            "prefs.edit()\n" +
            "    .putString(\"session_token\", seal(key, sessionToken))\n" +
            "    .apply()",
        takeaway = "The app sandbox is not a secret store — anything on disk is readable by the device owner.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            note = "Runs the wallet's remember-me sync against this device's real storage.",
            actions = listOf(
                KitAction("Run remember-me sync") { ctx, secure -> PrefsVault.saveSessionPlain(ctx, secure) },
                KitAction("Reset stored session") { ctx, _ ->
                    prefs(ctx).edit().clear().commit()
                    "Stored session cleared."
                },
            ),
        )
    }
}

class PrefsL2Challenge : TieredChallenge(
    category = "storage",
    slug = "prefs",
    level = Difficulty.MEDIUM,
    title = "Encoded Session",
    brief = "Engineering heard about plaintext tokens, so the session is now stored " +
        "encrypted. Verify the new protection holds.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "MASTG-TEST-0x51"),
    hints = listOf(
        "The prefs entry is called encrypted_flag. What algorithm is actually used?",
        "Encoding is not encryption. Base64(round-trips) in seconds.",
        "Decode the value of encrypted_flag.",
    ),
    flag = FLAG_L2,
    learn = LearnContent(
        theory = "Base64, hex and URL-encoding are transport encodings — they are fully " +
            "reversible with no secret. Wrapping a secret in one of them adds zero " +
            "protection, yet 'encrypted_flag' is one of the most common lies in prefs " +
            "files.\n\n" +
            "Whenever you see an opaque string in storage, check its alphabet first: " +
            "base64/hex decoders are one command away.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASTG-TEST-0x51"),
        vulnerableSnippet = "fun \"encrypt\"(value: String) =\n" +
            "    Base64.encodeToString(value.toByteArray(), NO_WRAP)",
        fixSnippet = "val key = keystoreKey(\"session\") // AndroidKeyStore, non-exportable\n" +
            "val sealed = aesGcmSeal(key, value.toByteArray())",
        takeaway = "If a 'decryption' needs no key, it was never encryption.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            note = "Stores the session with the wallet's current 'encryption'.",
            actions = listOf(
                KitAction("Save encrypted session") { ctx, secure -> PrefsVault.saveSessionEncoded(ctx, secure) },
            ),
        )
    }
}

class PrefsL3Challenge : TieredChallenge(
    category = "storage",
    slug = "prefs",
    level = Difficulty.HARD,
    title = "Release Key",
    brief = "This build encrypts the session with AES before storing it. The key is " +
        "derived per release. The ciphertext is right there in prefs — it should be " +
        "useless to you.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "M10", "MASTG-TEST-0x51"),
    hints = listOf(
        "The ciphertext alone is not enough — you need the key material.",
        "The app ships all of its key material inside the APK. Where do apps keep constants?",
        "The seed ships as a plain constant in PrefsVault — jadx prints it.",
        "SHA-256 of it, cut to 16 bytes, is the AES key.",
    ),
    flag = FLAG_L3,
    learn = LearnContent(
        theory = "Encrypting with a key that ships inside the APK only moves the problem: " +
            "the APK is readable. Constants in strings.xml, BuildConfig or companion " +
            "objects are extracted with jadx in minutes, and the 'secret' derivation is " +
            "replayed offline.\n\n" +
            "A key the app can compute by itself, an attacker can compute too. Keys must " +
            "be generated and held by Android Keystore, which never releases the bytes.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASVS-CRYPTO-1", "MASTG-TEST-0x51"),
        vulnerableSnippet = "// ships as a plain Kotlin constant in PrefsVault:\n" +
            "private val secret = \"droidsiege_pref_secret_2024\"\n" +
            "val key = SecretKeySpec(sha256(secret).copyOf(16), \"AES\")",
        fixSnippet = "val generator = KeyGenerator.getInstance(\"AES\", \"AndroidKeyStore\")\n" +
            "generator.init(KeyGenParameterSpec.Builder(\"session\", PURPOSE_ENCRYPT or PURPOSE_DECRYPT)…)\n" +
            "val key = generator.generateKey() // bytes never leave the TEE",
        takeaway = "A key written anywhere in the APK is a key handed to the attacker.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            note = "Stores the session with the release key derivation.",
            actions = listOf(
                KitAction("Save release-keyed session") { ctx, secure -> PrefsVault.saveSessionDerived(ctx, secure) },
            ),
        )
    }
}

class PrefsL4Challenge : TieredChallenge(
    category = "storage",
    slug = "prefs",
    level = Difficulty.INSANE,
    title = "Sealed Vault",
    brief = "The vault now uses EncryptedSharedPreferences with a master key — industry " +
        "standard. The ciphertext is untouchable. Or is the key, and is the vault the " +
        "only copy?",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "MASTG-TEST-0x51"),
    hints = listOf(
        "Two questions: is the master key really protected, and does the secret exist anywhere else?",
        "Compare the stored master_key_hint with what a Keystore master key should look like.",
        "support_cache is refreshed on every save. The strongest crypto cannot protect a plaintext photocopy.",
    ),
    flag = FLAG_L4,
    learn = LearnContent(
        theory = "Using EncryptedSharedPreferences is not a checklist item — the master key " +
            "spec matters. A master key that is short, static, or mirrored into plain " +
            "storage reduces the scheme to a fixed padlock with the key taped to the " +
            "door.\n\n" +
            "The second half is equally common: a 'support cache', a debug export, a " +
            "breadcrumb — a plaintext copy of the same secret somewhere else. Defense in " +
            "depth fails at the weakest copy.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASVS-CRYPTO-2", "MASTG-TEST-0x51"),
        vulnerableSnippet = "val master = \"droidsiege-mk!\".toByteArray().copyOf(16)\n" +
            "prefs.edit().putString(\"master_key_hint\", b64(master)) // 'support recovery'\n" +
            "prefs.edit().putString(\"support_cache\", secret)      // plaintext photocopy",
        fixSnippet = "val spec = KeyGenParameterSpec.Builder(\"vault_master\",\n" +
            "    PURPOSE_ENCRYPT or PURPOSE_DECRYPT)\n" +
            "    .setKeySize(256)\n" +
            "    .setIsStrongBoxBacked(true)   // when available\n" +
            "    .build()\n" +
            "// no plaintext copies anywhere — one sealed source of truth",
        takeaway = "Audit the key spec and hunt for the second copy — 'we use encrypted prefs' is not a control.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            note = "Runs the vault save path, including the support tooling.",
            actions = listOf(
                KitAction("Save vault secret") { ctx, secure -> PrefsVault.saveSessionMasterKeyed(ctx, secure) },
            ),
        )
    }
}
