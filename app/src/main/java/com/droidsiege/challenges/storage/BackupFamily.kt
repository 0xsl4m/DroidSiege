package com.droidsiege.challenges.storage

import android.app.backup.BackupAgentHelper
import android.app.backup.FileBackupHelper
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import com.droidsiege.challenges.common.ActionChallengeScreen
import com.droidsiege.challenges.common.KeystoreVault
import com.droidsiege.challenges.common.KitAction
import com.droidsiege.challenges.common.RawAes
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

private const val FLAG_L1 = "DS{storage_backup_L1_8c45f2}"
private const val FLAG_L2 = "DS{storage_backup_L2_10e7b6}"
private const val FLAG_L3 = "DS{storage_backup_L3_d29a3f}"
private const val FLAG_L4 = "DS{storage_backup_L4_47c610}"

/**
 * L2 — the custom agent the team wrote to make sure "support files" survive restores.
 * It backs up exactly the file the challenge plants.
 */
class SiegeBackupAgent : BackupAgentHelper() {
    override fun onCreate() {
        addHelper(
            "support_files",
            FileBackupHelper(this, "support/escalation_note.txt"),
        )
    }
}

object BackupVault {
    private fun prefs(context: Context) = context.getSharedPreferences("siege_backup_prefs", Context.MODE_PRIVATE)

    private fun androidId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "emulator"

    fun stageAccountRecovery(
        context: Context,
        secureMode: Boolean,
    ): String {
        if (!secureMode) {
            prefs(context).edit().putString("account_recovery", FLAG_L1).commit()
        } else {
            prefs(context).edit().remove("account_recovery").commit()
        }
        return if (!secureMode) {
            "Account recovery stored."
        } else {
            "Account recovery stays in memory; nothing backup-sensitive stored."
        }
    }

    fun writeSupportNote(
        context: Context,
        secureMode: Boolean,
    ): String {
        val dir = File(context.filesDir, "support").apply { mkdirs() }
        val note = File(dir, "escalation_note.txt")
        note.writeText(
            if (!secureMode) {
                "escalation pin: $FLAG_L2\ncontact: support-internal\n"
            } else {
                "escalation pin: [stored in the credential vault]\ncontact: support-internal\n"
            },
        )
        return if (!secureMode) {
            "Support note written. The backup agent will pick it up."
        } else {
            "Support note written without the pin."
        }
    }

    fun seedVaultDb(
        context: Context,
        secureMode: Boolean,
    ): String {
        val dao = VaultDatabases.secureStore(context).records()
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val payload =
            if (!secureMode) {
                RawAes.gcmEncrypt(SecretKeySpec(shaKey(context), "AES"), iv, FLAG_L3.toByteArray())
            } else {
                val key = KeystoreVault.loadOrCreateKey("backup_l3")
                RawAes.gcmEncrypt(key, iv, FLAG_L3.toByteArray())
            }
        var stored = false
        kotlinx.coroutines.runBlocking {
            dao.clear()
            if (!secureMode) {
                dao.insert(
                    SealedRecord(
                        recordId = "backup_recovery",
                        payload = payload,
                        nonce = iv,
                    ),
                )
                stored = true
            }
            // the secure path keeps nothing that the backup rules would sweep up
        }
        return if (stored) {
            "Vault record written — included in the configured backup set."
        } else {
            "No backup-sensitive record kept."
        }
    }

    private fun shaKey(context: Context): ByteArray =
        MessageDigest.getInstance("SHA-256")
            .digest("siege-cloud::${androidId(context)}".toByteArray())
            .copyOf(16)

    fun writeBackupBundle(
        context: Context,
        secureMode: Boolean,
    ): String {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val payload: ByteArray
        if (!secureMode) {
            // "recovery key" derived from data the app itself keeps in prefs
            val recoveryKey = MessageDigest.getInstance("MD5")
                .digest("backup-recovery::${androidId(context)}".toByteArray())
            payload = RawAes.gcmEncrypt(SecretKeySpec(recoveryKey, "AES"), iv, FLAG_L4.toByteArray())
            prefs(context).edit().putString("device_identity", androidId(context)).commit()
        } else {
            val key = KeystoreVault.loadOrCreateKey("backup_l4")
            payload = RawAes.gcmEncrypt(key, iv, FLAG_L4.toByteArray())
            prefs(context).edit().remove("device_identity").commit()
        }
        val bundle = File(context.filesDir, "backup_bundle.bin")
        bundle.outputStream().use { out ->
            out.write(iv.size)
            out.write(iv)
            out.write(payload)
        }
        return if (!secureMode) {
            "Encrypted backup bundle written. The recovery key is derivable from the device identity on file."
        } else {
            "Backup bundle sealed with a non-exportable Keystore key."
        }
    }
}

class BackupL1Challenge : TieredChallenge(
    category = "storage",
    slug = "backup",
    level = Difficulty.EASY,
    title = "Backed Up Account",
    brief = "The app allows Android to back up its data, and the account recovery code is " +
        "part of that data. Take the backup.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "MASTG-TEST-0x57"),
    hints = listOf(
        "Check the manifest: android:allowBackup is true.",
        "adb shell bmgr backupnow com.droidsiege runs a local backup on the emulator.",
        "The backup set lands in the local transport — or read the prefs it copies:",
        "shared_prefs/siege_backup_prefs.xml",
    ),
    flag = FLAG_L1,
    learn = LearnContent(
        theory = "allowBackup=true hands app data to the platform backup transport: adb " +
            "backups, bmgr, and cloud restore on new devices. On debuggable/rooted " +
            "devices the backup set is extractable, and restore-to-attacker-device is a " +
            "known exfiltration path.\n\n" +
            "Sensitive data must either not exist in backupable storage or be excluded " +
            "by rules.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASTG-TEST-0x57"),
        vulnerableSnippet = "<application android:allowBackup=\"true\">",
        fixSnippet = "<application android:allowBackup=\"false\">\n" +
            "// or a dataExtractionRules <exclude> for sensitive domains",
        takeaway = "Backups are copies you do not control — exclude secrets from them.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Store account recovery") { ctx, secure -> BackupVault.stageAccountRecovery(ctx, secure) },
            ),
        )
    }
}

class BackupL2Challenge : TieredChallenge(
    category = "storage",
    slug = "backup",
    level = Difficulty.MEDIUM,
    title = "Support Escalation",
    brief = "A custom backup agent makes sure support files survive device migrations. " +
        "One of those files is more support-sensitive than intended.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "MASTG-TEST-0x57"),
    hints = listOf(
        "The agent is declared in the manifest — read which files it backs up.",
        "SiegeBackupAgent wraps support/escalation_note.txt.",
        "Trigger a backup with bmgr and the note rides along with it.",
    ),
    flag = FLAG_L2,
    learn = LearnContent(
        theory = "Custom backup agents override the defaults: whatever they include " +
            "travels, whatever they forget is lost. An agent written to protect support " +
            "files happily backs up the escalation pin inside one.\n\n" +
            "Agents need the same data classification as the rest of the app — and " +
            "sensitive files must be excluded from their file lists.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASTG-TEST-0x57"),
        vulnerableSnippet = "class SiegeBackupAgent : BackupAgentHelper() {\n" +
            "    override fun onCreate() {\n" +
            "        addHelper(\"support_files\", FileBackupHelper(this, \"support/escalation_note.txt\"))\n" +
            "    }\n" +
            "}",
        fixSnippet = "// The pin lives in the credential vault, not in a backed-up file:\n" +
            "addHelper(\"support_files\", FileBackupHelper(this, \"support/contact.txt\"))",
        takeaway = "Review backup agents like network endpoints — they move data off-device.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Write support note") { ctx, secure -> BackupVault.writeSupportNote(ctx, secure) },
            ),
        )
    }
}

class BackupL3Challenge : TieredChallenge(
    category = "storage",
    slug = "backup",
    level = Difficulty.HARD,
    title = "Cloud Rules",
    brief = "The team migrated to the modern auto-backup and wrote extraction rules. " +
        "The vault database should be covered. 'Covered' can mean two things.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "MASTG-TEST-0x57"),
    hints = listOf(
        "Read res/xml/backup_rules.xml — what do the rules actually include?",
        "The rules sweep every database into the cloud backup set, secure_store.db included.",
        "The record is AES-GCM sealed — but the passphrase is the device-derived one from Sealed Recovery.",
    ),
    flag = FLAG_L3,
    learn = LearnContent(
        theory = "dataExtractionRules and fullBackupContent decide what leaves the " +
            "device. An <include> that is too wide (all databases, all prefs) silently " +
            "uploads everything the app persists — encryption at rest does not help when " +
            "the key derivation is equally portable.\n\n" +
            "Rules must be allowlists of safe domains, and anything sensitive must be " +
            "either excluded or sealed with non-portable keys.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASTG-TEST-0x57"),
        vulnerableSnippet = "<include domain=\"database\" path=\".\"/>\n" +
            "<include domain=\"sharedpref\" path=\".\"/>",
        fixSnippet = "<exclude domain=\"database\" path=\"secure_store.db\"/>\n" +
            "// allowlist only non-sensitive domains",
        takeaway = "Backup rules are an exfiltration config — review them like one.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Write vault record") { ctx, secure -> BackupVault.seedVaultDb(ctx, secure) },
            ),
        )
    }
}

class BackupL4Challenge : TieredChallenge(
    category = "storage",
    slug = "backup",
    level = Difficulty.INSANE,
    title = "Recovery Key",
    brief = "The backup bundle is encrypted before it ever reaches the transport — the " +
        "team is proud of that. The recovery key was designed so support can always " +
        "rebuild it. So can you.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "M10", "MASTG-TEST-0x57"),
    hints = listOf(
        "The app stores a device_identity in its backup prefs — that is the recovery key's input.",
        "recovery key = MD5(\"backup-recovery::\" + device_identity)",
        "Decrypt files/backup_bundle.bin: first byte is the IV length, then IV, then AES-GCM ciphertext.",
    ),
    flag = FLAG_L4,
    learn = LearnContent(
        theory = "'We encrypt the backup' only helps if the key is not a puzzle the app " +
            "publishes the pieces of. A key derived from data stored next to the " +
            "ciphertext is a shruggie emoji away from plaintext: read prefs, run MD5, " +
            "done.\n\n" +
            "Backup encryption keys must live in hardware (Keystore), making the bundle " +
            "useless on any other device — which is exactly the property a backup thief " +
            "hates.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASVS-CRYPTO-2", "MASTG-TEST-0x57"),
        vulnerableSnippet = "val identity = prefs.getString(\"device_identity\", null)\n" +
            "val key = md5(\"backup-recovery::${'$'}identity\") // pieces stored beside the blob",
        fixSnippet = "val key = keystoreKey(\"backup_bundle\") // TEE-bound, non-exportable\n" +
            "// bundle is unreadable outside this hardware",
        takeaway = "A backup key that can be recomputed from the backup's neighborhood is not a key.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Write backup bundle") { ctx, secure -> BackupVault.writeBackupBundle(ctx, secure) },
            ),
        )
    }
}
