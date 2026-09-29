package com.droidsiege.challenges.storage

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import com.droidsiege.challenges.common.ActionChallengeScreen
import com.droidsiege.challenges.common.KeystoreVault
import com.droidsiege.challenges.common.KitAction
import com.droidsiege.challenges.common.RawAes
import com.droidsiege.challenges.common.SealedBox
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

private const val FLAG_L1 = "DS{storage_sqlite_L1_4d7b90}"
private const val FLAG_L2 = "DS{storage_sqlite_L2_2a5e17}"
private const val FLAG_L3 = "DS{storage_sqlite_L3_c8f346}"
private const val FLAG_L4 = "DS{storage_sqlite_L4_09b7d2}"

/** The passphrase seed for L3 — half hardcoded, half the device's ANDROID_ID. */
private const val STORE_PASSPHRASE_SEED = "droidsiege::"

private fun l3Key(context: Context): SecretKeySpec {
    val androidId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID,
    ) ?: "emulator"
    val digest = MessageDigest.getInstance("SHA-256")
        .digest((STORE_PASSPHRASE_SEED + androidId).toByteArray())
    return SecretKeySpec(digest.copyOf(16), "AES")
}

/**
 * PLACEHOLDER Phase-5 native; L4 path = runtime hook, not static. The obfuscated
 * constants alone are a dead end by design: half of the key material is per-install
 * and only ever exists wrapped by a non-exportable Keystore key. The final key is
 * assembled in memory when the gate opens — the intended solve is a Frida hook on
 * SecretKeySpec/Cipher at that moment, and the real `.so` replaces this in Phase 5.
 */
internal object WarpKeyBridge {
    // What a lightweight obfuscator produces from a native constant.
    private val p1 = intArrayOf(0x77, 0x61, 0x72, 0x70) // 'warp'
    private const val P2 = "k3y-2026"

    private fun installHalf(context: Context): ByteArray {
        val prefs = context.getSharedPreferences("warp_bridge_prefs", Context.MODE_PRIVATE)
        val wrapKey = com.droidsiege.challenges.common.KeystoreVault.loadOrCreateKey("warp_wrap")
        prefs.getString("install_half", null)?.let {
            return com.droidsiege.challenges.common.SealedBox.unseal(
                wrapKey,
                android.util.Base64.decode(it, android.util.Base64.NO_WRAP),
            )
        }
        val fresh = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(
                "install_half",
                android.util.Base64.encodeToString(
                    com.droidsiege.challenges.common.SealedBox.seal(wrapKey, fresh),
                    android.util.Base64.NO_WRAP,
                ),
            )
            .commit()
        return fresh
    }

    fun materialize(context: Context): ByteArray {
        val sb = StringBuilder()
        p1.forEach { sb.appendCodePoint(it) }
        sb.append('-')
        sb.append(P2.reversed())
        installHalf(context).forEach { sb.append("%02x".format(it)) }
        return MessageDigest.getInstance("SHA-256").digest(sb.toString().toByteArray()).copyOf(16)
    }
}

object SqliteSeeder {
    fun seedPlainNotes(
        context: Context,
        secureMode: Boolean,
    ): String {
        val dao = VaultDatabases.vault(context).notes()
        runBlocking {
            dao.clear()
            if (!secureMode) {
                dao.insert(VaultNote(title = "Wallet recovery", body = FLAG_L1))
                dao.insert(VaultNote(title = "Wifi guest", body = "guest-2026-temporary"))
            }
        }
        return if (!secureMode) {
            "Notes synced: 2 records stored."
        } else {
            "Notes synced. Sensitive notes are no longer persisted."
        }
    }

    fun seedOpsCache(
        context: Context,
        secureMode: Boolean,
    ): String {
        val dao = VaultDatabases.cache(context).entries()
        runBlocking {
            dao.clear()
            if (!secureMode) {
                dao.insert(CacheEntry(c1 = "cfg.sync.flag", cfg_blob = FLAG_L2))
                dao.insert(CacheEntry(c1 = "cfg.sync.hint", cfg_blob = "last-sync=1727900000"))
            }
        }
        return if (!secureMode) {
            "Ops cache refreshed: 2 config entries."
        } else {
            "Ops cache refreshed. Secret config entries skipped."
        }
    }

    fun seedSealedStore(
        context: Context,
        secureMode: Boolean,
    ): String {
        val dao = VaultDatabases.secureStore(context).records()
        val sealed =
            if (!secureMode) {
                val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
                RawAes.gcmEncrypt(l3Key(context), iv, FLAG_L3.toByteArray()).let { iv to it }
            } else {
                SealedBox
                    .seal(KeystoreVault.loadOrCreateKey("sqlite_l3"), FLAG_L3.toByteArray())
                    .let { it.copyOf(12) to it.copyOfRange(12, it.size) }
            }
        runBlocking {
            dao.clear()
            dao.insert(SealedRecord(recordId = "recovery", payload = sealed.second, nonce = sealed.first))
        }
        return if (!secureMode) {
            "Recovery record sealed with the device passphrase."
        } else {
            "Recovery record sealed with a non-exportable Keystore key."
        }
    }

    fun seedWarpVault(
        context: Context,
        secureMode: Boolean,
    ): String {
        val dao = VaultDatabases.warpVault(context).records()
        val sealed =
            if (!secureMode) {
                val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
                RawAes
                    .gcmEncrypt(
                        SecretKeySpec(WarpKeyBridge.materialize(context), "AES"),
                        iv,
                        FLAG_L4.toByteArray(),
                    )
                    .let { iv to it }
            } else {
                SealedBox
                    .seal(KeystoreVault.loadOrCreateKey("sqlite_l4"), FLAG_L4.toByteArray())
                    .let { it.copyOf(12) to it.copyOfRange(12, it.size) }
            }
        runBlocking {
            dao.clear()
            dao.insert(SealedRecord(recordId = "warp-core", payload = sealed.second, nonce = sealed.first))
        }
        return if (!secureMode) {
            "Warp vault initialized. The key lives in the bridge, not in prefs."
        } else {
            "Warp vault initialized with a hardware-backed key."
        }
    }
}

class SqliteL1Challenge : TieredChallenge(
    category = "storage",
    slug = "sqlite",
    level = Difficulty.EASY,
    title = "Notes Vault",
    brief = "The wallet keeps recovery notes in its local database. One of those notes is " +
        "worth reading.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-2", "MASTG-TEST-0x54"),
    hints = listOf(
        "Databases are files under /data/data/com.droidsiege/databases/.",
        "vault.db is a plain SQLite file — pull it and open it with sqlite3.",
        "The notes table holds the answer in a body column.",
    ),
    flag = FLAG_L1,
    learn = LearnContent(
        theory = "A local database inherits the same trust model as prefs: sandboxed from " +
            "other apps, wide open to the device owner. A pulled .db file opens with any " +
            "SQLite client, and Room is only a thin layer on top of plain tables.\n\n" +
            "Secrets stored 'in the database' are stored in a file with no protection at all.",
        mastgRefs = listOf("MASVS-STORAGE-2", "MASTG-TEST-0x54"),
        vulnerableSnippet = "@Insert\nsuspend fun insert(note: VaultNote) // body = recovery code",
        fixSnippet = "// Do not persist the secret, or seal it first:\n" +
            "dao.insert(note.copy(body = seal(keystoreKey(\"notes\"), secret)))",
        takeaway = "Pulling a .db file is a one-liner — anything plaintext inside is captured.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            note = "Syncs the wallet's notes into its local database.",
            actions = listOf(
                KitAction("Sync notes") { ctx, secure -> SqliteSeeder.seedPlainNotes(ctx, secure) },
            ),
        )
    }
}

class SqliteL2Challenge : TieredChallenge(
    category = "storage",
    slug = "sqlite",
    level = Difficulty.MEDIUM,
    title = "Ops Cache",
    brief = "There is no notes database in this build — the team moved operational config " +
        "into an internal cache. Nothing to see there.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-2", "MASTG-TEST-0x54"),
    hints = listOf(
        "List every file under databases/, not just the obvious ones.",
        "sys_cache_v3.db is still a SQLite file; .schema reveals its structure.",
        "Table sys_kv, column cfg_blob — config values live there.",
    ),
    flag = FLAG_L2,
    learn = LearnContent(
        theory = "Renaming files and columns is security by obscurity. An attacker who " +
            "enumerates the databases directory finds every .db regardless of its name, " +
            "and .schema prints the layout. Obfuscation only slows down a screen-shot " +
            "walkthrough, not an audit.\n\n" +
            "If a value must be secret, its storage name has to be the least interesting " +
            "thing about it.",
        mastgRefs = listOf("MASVS-STORAGE-2", "MASTG-TEST-0x54"),
        vulnerableSnippet = "@Entity(tableName = \"sys_kv\")\ndata class CacheEntry(\n" +
            "    @PrimaryKey val c1: String,\n" +
            "    val cfg_blob: String,\n)",
        fixSnippet = "// Secrets do not belong in the cache at all:\n" +
            "// keep them in memory or behind a Keystore-sealed record.",
        takeaway = "Obscure names change the search order, not the outcome.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            note = "Refreshes the internal ops cache.",
            actions = listOf(
                KitAction("Refresh ops cache") { ctx, secure -> SqliteSeeder.seedOpsCache(ctx, secure) },
            ),
        )
    }
}

class SqliteL3Challenge : TieredChallenge(
    category = "storage",
    slug = "sqlite",
    level = Difficulty.HARD,
    title = "Sealed Recovery",
    brief = "Recovery data now lives encrypted in secure_store.db, keyed by a passphrase " +
        "made for this device. The ciphertext in the database is the only copy.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-2", "M10", "MASTG-TEST-0x54"),
    hints = listOf(
        "The passphrase combines a fixed prefix with something this device provides.",
        "settings get secure android_id gives you the device part; the prefix ships in the APK.",
        "payload = AES-GCM(SHA-256(\"droidsiege::\" + androidId)[..16]); the nonce column is next to it.",
    ),
    flag = FLAG_L3,
    learn = LearnContent(
        theory = "'Device-specific' keys derived from ANDROID_ID are a classic middle " +
            "ground that fails both ways: the derivation input is one adb command away, " +
            "and the constant half ships in the app. The attacker reproduces the exact " +
            "passphrase offline.\n\n" +
            "Device binding must be done by hardware — Android Keystore ties keys to the " +
            "TEE without ever exposing the material.",
        mastgRefs = listOf("MASVS-STORAGE-2", "MASVS-CRYPTO-2", "MASTG-TEST-0x54"),
        vulnerableSnippet = "val pass = \"droidsiege::\" + Settings.Secure.ANDROID_ID\n" +
            "val key = sha256(pass).copyOf(16) // fully reproducible off-device",
        fixSnippet = "val kg = KeyGenerator.getInstance(\"AES\", \"AndroidKeyStore\")\n" +
            "kg.init(KeyGenParameterSpec.Builder(\"store\", PURPOSE_ENCRYPT or PURPOSE_DECRYPT).build())\n" +
            "// hardware-bound; ANDROID_ID never enters key material",
        takeaway = "Reproducible derivations are reproducible by the attacker — bind keys to hardware, not IDs.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            note = "Seals the recovery record into secure_store.db.",
            actions = listOf(
                KitAction("Seal recovery record") { ctx, secure -> SqliteSeeder.seedSealedStore(ctx, secure) },
            ),
        )
    }
}

class SqliteL4Challenge : TieredChallenge(
    category = "storage",
    slug = "sqlite",
    level = Difficulty.INSANE,
    title = "Warp Vault",
    brief = "warp_vault.db is sealed by the warp bridge — the key never exists as a plain " +
        "string, and the vault only unseals at runtime. The database file alone is dead " +
        "weight.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-2", "M7", "MASTG-TEST-0x54"),
    hints = listOf(
        "The key is assembled from obfuscated pieces at runtime — decompile the bridge.",
        "Static halves are a dead end: half the material is per-install and Keystore-wrapped.",
        "Open the gate under a Frida SecretKeySpec.<init> hook — the assembled key prints itself.",
    ),
    flag = FLAG_L4,
    learn = LearnContent(
        theory = "Hiding key material behind obfuscation or a 'bridge' delays static " +
            "reading, but the key must exist in memory as bytes the moment crypto runs — " +
            "and that is exactly what dynamic tooling observes. Frida hooks on " +
            "SecretKeySpec or Cipher.init dump every key, regardless of how it was built.\n\n" +
            "True protection keeps the material inside the TEE (Keystore/StrongBox) where " +
            "no hook can read it.",
        mastgRefs = listOf("MASVS-STORAGE-2", "MASVS-CRYPTO-2", "MASTG-TEST-0x54"),
        vulnerableSnippet = "fun materialize(ctx: Context) =\n" +
            "    sha256(staticHalf() + installHalf(ctx)) // install half: Keystore-wrapped\n" +
            "        .copyOf(16) // bytes exist in memory only when the gate opens",
        fixSnippet = "// Hardware-bound keys never materialize in app memory:\n" +
            "KeyGenerator.getInstance(\"AES\", \"AndroidKeyStore\")\n" +
            "    .apply { init(KeyGenParameterSpec.Builder(\"warp\", …).setIsStrongBoxBacked(true).build()) }",
        takeaway = "If your code can reconstruct the key, so can a hook — hardware or nothing.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            note = "Initializes the warp vault on this device.",
            actions = listOf(
                KitAction("Initialize warp vault") { ctx, secure -> SqliteSeeder.seedWarpVault(ctx, secure) },
                KitAction("Show bridge diagnostics") { ctx, _ ->
                    "bridge = WarpKeyBridge@${System.identityHashCode(WarpKeyBridge).toString(16)} " +
                        "(install half wrapped: " +
                        "${ctx.getSharedPreferences("warp_bridge_prefs", Context.MODE_PRIVATE)
                            .getString("install_half", null) != null})"
                },
            ),
        )
    }
}
