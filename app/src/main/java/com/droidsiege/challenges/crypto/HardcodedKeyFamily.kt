package com.droidsiege.challenges.crypto

import android.content.Context
import androidx.compose.runtime.Composable
import com.droidsiege.BuildConfig
import com.droidsiege.challenges.common.ConsoleChallengeScreen
import com.droidsiege.challenges.common.KeystoreVault
import com.droidsiege.challenges.common.RawAes
import com.droidsiege.challenges.common.SealedBox
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

private const val FLAG_L1 = "DS{crypto_hardcoded_L1_41f7c2}"
private const val FLAG_L2 = "DS{crypto_hardcoded_L2_9d3a58}"
private const val FLAG_L3 = "DS{crypto_hardcoded_L3_e6b824}"
private const val FLAG_L4 = "DS{crypto_hardcoded_L4_15c7f9}"

private fun randomIv() = ByteArray(12).also { SecureRandom().nextBytes(it) }

private fun b64(bytes: ByteArray) = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

/**
 * PLACEHOLDER Phase-5 native; L4 path = runtime hook, not static. The obfuscated
 * constants are a dead end by design: half the material is per-install and only
 * exists wrapped by a non-exportable Keystore key. The full key is assembled in
 * memory when the license path runs — dump it with the Frida SecretKeySpec hook
 * (docs/solutions/crypto/hardcoded/tools/hook-secretkeyspec.js). The real `.so`
 * replaces this stand-in in Phase 5.
 */
internal object LicenseKeyBridge {
    private val a = intArrayOf(0x4c, 0x69, 0x63) // 'Lic'
    private const val SCRAMBLED = "3N\$E" // deliberately scrambled constant

    private fun installHalf(context: Context): ByteArray {
        val prefs = context.getSharedPreferences("license_bridge_prefs", Context.MODE_PRIVATE)
        val wrapKey = KeystoreVault.loadOrCreateKey("license_wrap")
        prefs.getString("install_half", null)?.let {
            return SealedBox.unseal(
                wrapKey,
                android.util.Base64.decode(it, android.util.Base64.NO_WRAP),
            )
        }
        val fresh = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(
                "install_half",
                android.util.Base64.encodeToString(SealedBox.seal(wrapKey, fresh), android.util.Base64.NO_WRAP),
            )
            .commit()
        return fresh
    }

    fun materialize(context: Context): ByteArray {
        val sb = StringBuilder()
        a.forEach { sb.appendCodePoint(it) }
        sb.append(SCRAMBLED.reversed().lowercase())
        sb.append("nse-vault")
        installHalf(context).forEach { sb.append("%02x".format(it)) }
        return MessageDigest.getInstance("SHA-256").digest(sb.toString().toByteArray())
    }
}

object HardcodedKeyVault {
    private fun licenseBlob(secureMode: Boolean): ByteArray =
        if (!secureMode) {
            val iv = randomIv()
            val key = SecretKeySpec("droidsiege-static".toByteArray().copyOf(16), "AES")
            iv + RawAes.gcmEncrypt(key, iv, FLAG_L1.toByteArray())
        } else {
            SealedBox.seal(KeystoreVault.loadOrCreateKey("license_l1"), FLAG_L1.toByteArray())
        }

    fun licenseLines(secureMode: Boolean): List<Pair<String, String>> {
        return listOf(
            "License blob (base64, iv||ct)" to
                b64(licenseBlob(secureMode)),
            "Key location" to
                if (!secureMode) {
                    "HardcodedKeyVault — hardcoded 16-byte constant in this file"
                } else {
                    "AndroidKeyStore alias license_l1 (non-exportable)"
                },
        )
    }

    fun splitKeyLines(secureMode: Boolean): List<Pair<String, String>> {
        val blob =
            if (!secureMode) {
                val iv = randomIv()
                val key = RawAes.keyFrom(
                    sha256((BuildConfig.WALLET_KEY_PART_A + "install_seed_2026").toByteArray()).copyOf(16),
                )
                iv + RawAes.gcmEncrypt(key, iv, FLAG_L2.toByteArray())
            } else {
                SealedBox.seal(KeystoreVault.loadOrCreateKey("license_l2"), FLAG_L2.toByteArray())
            }
        return listOf(
            "Install license (base64, iv||ct)" to b64(blob),
            "Part A" to "BuildConfig.WALLET_KEY_PART_A (gradle, see app/build.gradle.kts)",
            "Part B" to "Kotlin literal in HardcodedKeyVault: install_seed_2026",
            "Derivation" to "AES key = SHA-256(partA + partB)[..16]",
        )
    }

    fun runtimeKeyLines(
        context: Context,
        secureMode: Boolean,
    ): List<Pair<String, String>> {
        val blob =
            if (!secureMode) {
                val iv = randomIv()
                RawAes
                    .gcmEncrypt(SecretKeySpec(runtimeKey(context), "AES"), iv, FLAG_L3.toByteArray())
                    .let { iv + it }
            } else {
                SealedBox.seal(KeystoreVault.loadOrCreateKey("license_l3"), FLAG_L3.toByteArray())
            }
        return listOf(
            "Enterprise license (base64, iv||ct)" to b64(blob),
            "Derivation" to "SHA-256(res_partner_tag + sha256Hex(apk signing cert))",
            "res_partner_tag" to context.getString(com.droidsiege.R.string.partner_key_tag),
        )
    }

    private fun runtimeKey(context: Context): ByteArray {
        val tag = context.getString(com.droidsiege.R.string.partner_key_tag)
        val signatures = checkNotNull(
            context.packageManager
                .getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNATURES)
                .signatures,
        )
        val certHash = MessageDigest.getInstance("SHA-256")
            .digest(signatures.first().toByteArray())
            .joinToString("") { "%02x".format(it) }
        return MessageDigest.getInstance("SHA-256")
            .digest((tag + certHash).toByteArray())
    }

    fun bridgeLines(
        context: Context,
        secureMode: Boolean,
    ): List<Pair<String, String>> {
        val blob =
            if (!secureMode) {
                val iv = randomIv()
                RawAes
                    .gcmEncrypt(
                        SecretKeySpec(LicenseKeyBridge.materialize(context), "AES"),
                        iv,
                        FLAG_L4.toByteArray(),
                    )
                    .let { iv + it }
            } else {
                SealedBox.seal(KeystoreVault.loadOrCreateKey("license_l4"), FLAG_L4.toByteArray())
            }
        return listOf(
            "Legacy license (base64, iv||ct)" to b64(blob),
            "Key location" to "LicenseKeyBridge.materialize(context) — half is per-install, Keystore-wrapped",
        )
    }
}

private fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

class HardcodedL1Challenge : TieredChallenge(
    category = "crypto",
    slug = "hardcoded",
    level = Difficulty.EASY,
    title = "Static License",
    brief = "The license blob below unlocks the premium wallet tier. The app knows how " +
        "to decrypt it — so can you.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-1", "MASTG-TEST-0x24"),
    hints = listOf(
        "The AES key is a hardcoded 16-byte string in HardcodedKeyVault — decompile and read.",
        "jadx: the 16-byte key is a literal inside HardcodedKeyVault — decrypt the blob with any AES-GCM tool.",
        "The blob is base64(iv||ciphertext) — 12-byte nonce first.",
    ),
    flag = FLAG_L1,
    learn = LearnContent(
        theory = "A hardcoded key is not a key — it is a constant shipped to every " +
            "device and readable in the APK with a decompiler. Whatever it protects, " +
            "the attacker decrypts offline in one sitting.\n\n" +
            "Symmetric keys must be generated per-install inside Android Keystore, " +
            "where they can be used but never exported.",
        mastgRefs = listOf("MASVS-CRYPTO-1", "MASTG-TEST-0x24"),
        vulnerableSnippet = "// the literal is 17 chars; the code truncates to a 16-byte key:\n" +
            "SecretKeySpec(\"droidsiege-static\".toByteArray().copyOf(16), \"AES\")\n" +
            "val cipher = aesGcm(key, blob) // AES/GCM",
        fixSnippet = "val kg = KeyGenerator.getInstance(\"AES\", \"AndroidKeyStore\")\n" +
            "kg.init(KeyGenParameterSpec.Builder(\"license\", PURPOSE_ENCRYPT or PURPOSE_DECRYPT).build())",
        takeaway = "Keys in source code are keys in the attacker's hands.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ConsoleChallengeScreen(secureMode = secureMode, lines = HardcodedKeyVault.licenseLines(secureMode))
    }
}

class HardcodedL2Challenge : TieredChallenge(
    category = "crypto",
    slug = "hardcoded",
    level = Difficulty.MEDIUM,
    title = "Split Secret",
    brief = "Security review rejected one hardcoded key, so the key is now split across " +
        "the build config and resources. Recombining is left as an exercise.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-1", "MASTG-TEST-0x24"),
    hints = listOf(
        "Both halves ship inside the APK — the split is between files, not parties.",
        "BuildConfig.WALLET_KEY_PART_A is in app/build.gradle.kts; part B is in strings.xml.",
        "SHA-256(partA + \"install_seed_2026\")[..16] is the AES key.",
    ),
    flag = FLAG_L2,
    learn = LearnContent(
        theory = "Splitting a key across gradle files, resources and code feels like " +
            "dispersing trust, but the APK assembles all of it on every device. The " +
            "attacker diffs one build against another (or simply reads the assembled " +
            "key in memory) and rejoins the halves.\n\n" +
            "Secret-splitting only helps when the shares live with different parties " +
            "— not in the same archive.",
        mastgRefs = listOf("MASVS-CRYPTO-1", "MASTG-TEST-0x24"),
        vulnerableSnippet = "val key = sha256(BuildConfig.WALLET_KEY_PART_A + partB).copyOf(16)",
        fixSnippet = "// per-install, non-exportable:\n" +
            "KeyGenerator.getInstance(\"AES\", \"AndroidKeyStore\")",
        takeaway = "Two halves in the same APK make one whole secret.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ConsoleChallengeScreen(secureMode = secureMode, lines = HardcodedKeyVault.splitKeyLines(secureMode))
    }
}

class HardcodedL3Challenge : TieredChallenge(
    category = "crypto",
    slug = "hardcoded",
    level = Difficulty.HARD,
    title = "Signed Derivation",
    brief = "The enterprise license key is derived at runtime from a partner tag and " +
        "this build's signing certificate. It never appears in the source.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-1", "MASTG-TEST-0x24"),
    hints = listOf(
        "The signing cert is public metadata of the APK — not a secret.",
        "apksigner verify --print-certs app-debug.apk prints the certificate you need.",
        "key = SHA-256(<partner_key_tag> + sha256Hex(cert)), then AES-GCM the blob.",
    ),
    flag = FLAG_L3,
    learn = LearnContent(
        theory = "Deriving keys from the signing certificate is a popular 'device " +
            "binding' pattern that survives source reading — but certificates are " +
            "public, printed by apksigner in one command. Anyone holding the APK holds " +
            "the derivation inputs.\n\n" +
            "Signature-based binding proves who built the app; it says nothing about " +
            "who is running it. Keystore + attestation is the actual binding mechanism.",
        mastgRefs = listOf("MASVS-CRYPTO-1", "MASTG-TEST-0x24"),
        vulnerableSnippet = "val cert = signatures.first().toByteArray()\n" +
            "val key = sha256(tag + sha256Hex(cert))",
        fixSnippet = "// hardware attestation ties keys to the verified boot state:\n" +
            "KeyGenParameterSpec.Builder(\"license\", …).setAttestationChallenge(challenge)",
        takeaway = "Public metadata is not key material.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ConsoleChallengeScreen(
            secureMode = secureMode,
            lines = HardcodedKeyVault.runtimeKeyLines(context, secureMode),
        )
    }
}

class HardcodedL4Challenge : TieredChallenge(
    category = "crypto",
    slug = "hardcoded",
    level = Difficulty.INSANE,
    title = "Bridge License",
    brief = "The legacy license path builds its key inside the bridge at runtime — it " +
        "never exists as a literal. The ciphertext is in front of you.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-1", "M7", "MASTG-TEST-0x24"),
    hints = listOf(
        "Static reading gives half the recipe — the other half is per-install and Keystore-wrapped.",
        "Run the license path under the Frida SecretKeySpec.<init> hook and read the assembled key.",
        "docs/solutions/crypto/hardcoded/tools/hook-secretkeyspec.js does exactly that.",
    ),
    flag = FLAG_L4,
    learn = LearnContent(
        theory = "Runtime assembly and light obfuscation raise the cost of static " +
            "reading, not of observation. The moment the key reaches javax.crypto it " +
            "exists as plain bytes in memory — precisely where dynamic tooling lives.\n\n" +
            "Obfuscation protects against nothing more than grep; keys belong in " +
            "hardware-backed storage that never yields bytes.",
        mastgRefs = listOf("MASVS-CRYPTO-1", "MASVS-RESILIENCE-2", "MASTG-TEST-0x24"),
        vulnerableSnippet = "// static half + per-install half (Keystore-wrapped):\n" +
            "fun materialize(ctx: Context) =\n" +
            "    sha256(staticHalf() + installHalf(ctx)) // in memory only while sealing",
        fixSnippet = "// hardware-backed keys are used, never observed:\n" +
            "val key = keystoreKey(\"license_legacy\")",
        takeaway = "Obfuscation reshuffles the reading order for the attacker's benefit.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ConsoleChallengeScreen(
            secureMode = secureMode,
            lines = HardcodedKeyVault.bridgeLines(context, secureMode),
        )
    }
}
