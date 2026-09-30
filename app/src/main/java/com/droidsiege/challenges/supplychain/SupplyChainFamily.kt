package com.droidsiege.challenges.supplychain

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidsiege.challenges.common.ChallengeConsole
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import dalvik.system.DexClassLoader
import java.io.File
import java.io.ObjectInputStream

/** L3 — a deliberately insecure deserialization helper (readObject on caller bytes). */
object InsecureSerde {
    fun deserialize(bytes: ByteArray): Any? {
        java.io.ObjectInputStream(bytes.inputStream()).use { it.readObject() }
        return null
    }
}

object DynLoadLab {
    /** L1 — loads a DEX from app external storage (attacker-writable). */
    fun loadExternalDex(
        context: Context,
        secure: Boolean,
    ): String {
        val dir = context.getExternalFilesDir(null) ?: return "external storage unavailable"
        val dexFile = File(dir, "plugin.dex")
        if (secure) {
            return "hardened: no external dex loading — plugins ship inside the APK"
        }
        if (!dexFile.exists()) {
            dexFile.writeBytes(
                byteArrayOf(
                    0x64,
                    0x65,
                    0x78,
                    0x0A,
                    0x30,
                    0x33,
                    0x35,
                    0x00,
                ),
            )
        }
        return runCatching {
            val loader = DexClassLoader(
                dexFile.absolutePath,
                context.codeCacheDir.absolutePath,
                null,
                context.classLoader,
            )
            "loaded (placeholder dex): loader=${loader.javaClass.simpleName} — " +
                "swap plugin.dex to inject code"
        }.getOrElse { "load failed: ${it.message}" }
    }

    /** L3 — insecure deserialization of a caller-supplied payload file. */
    fun deserializePayload(
        context: Context,
        secure: Boolean,
    ): String {
        val payload = File(context.filesDir, "sync_payload.ser")
        if (secure) {
            payload.writeText("json-validated payload only")
            return "hardened: JSON-validated payload written (no native deserialization)"
        }
        if (!payload.exists()) {
            payload.writeBytes(byteArrayOf(0xAC.toByte(), 0xED.toByte(), 0x00, 0x05))
        }
        return runCatching {
            InsecureSerde.deserialize(payload.readBytes())
            "deserialized OK — native ObjectInputStream accepts anything"
        }.getOrElse { "deserialization refused: ${it.javaClass.simpleName}" }
    }
}

class VulndepL1Challenge : TieredChallenge(
    category = "supplychain",
    slug = "vulndep",
    level = Difficulty.EASY,
    title = "Vulnerable Decoder",
    brief = "The app bundles an old image-decoder dependency with a known buffer " +
        "over-read: a crafted header walks past the bitmap into the recovery record.",
    owaspRefs = listOf("M2", "MASVS-RESILIENCE-3", "MASTG-TEST-0x57"),
    hints = listOf(
        "The decoder trusts the header's declared width/height.",
        "Craft a header with a huge height — the over-read walks into adjacent memory.",
        "Secure builds upgraded the decoder.",
    ),
    flag = "DS{supplychain_vulndep_L1_48d2b6}",
    learn = LearnContent(
        theory = "Bundled dependencies age: a known CVE in an old decoder applies to " +
            "your app the day you ship it, and an over-read in decoded memory exposes " +
            "whatever sits adjacent.\n\n" +
            "Software composition analysis (SCA) catches old versions before release.",
        mastgRefs = listOf("MASVS-RESILIENCE-3", "MASTG-TEST-0x57"),
        vulnerableSnippet = "OldDecoder.decode(header) // header height trusted",
        fixSnippet = "// upgrade to the patched decoder version",
        takeaway = "Your dependency's CVE is your CVE.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                // simulated over-read: declared length walks past the bitmap buffer
                val bitmapBuffer = "RGBA-bitmap-data;adjacent=recovery:DS{supplychain_vulndep_L1_48d2b6}"
                val declaredHeight = if (!secureMode) 8192 else 16
                output = if (!secureMode) {
                    "decoded ${declaredHeight}px — over-read tail: " +
                        bitmapBuffer.substring(0, declaredHeight.coerceAtMost(bitmapBuffer.length)) +
                        "\n(recovery visible in the over-read)"
                } else {
                    "patched decoder clamps the header — decode failed safely"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Decode crafted image") }
            ChallengeConsole(output)
        }
    }
}

class VulndepL2Challenge : TieredChallenge(
    category = "supplychain",
    slug = "vulndep",
    level = Difficulty.MEDIUM,
    title = "Transitive Reach",
    brief = "A transitive dependency pulls an old crypto helper into the feature " +
        "path. Reach it.",
    owaspRefs = listOf("M2", "MASVS-RESILIENCE-3", "MASTG-TEST-0x57"),
    hints = listOf(
        "gradlew :app:dependencies shows the transitive chain.",
        "The helper's ECB mode is reachable from the vault render path.",
        "Secure builds exclude the transitive dep and pin the fixed version.",
    ),
    flag = "DS{supplychain_vulndep_L2_c75a19}",
    learn = LearnContent(
        theory = "Transitive dependencies import vulnerable code two hops away: " +
            "nobody chose it, but the class is on the classpath and reachable from " +
            "feature code.\n\n" +
            "Pin versions and fail the build on vulnerable transitive resolves (SCA).",
        mastgRefs = listOf("MASVS-RESILIENCE-3", "MASTG-TEST-0x57"),
        vulnerableSnippet = "// transitive: old-crypto 1.2 → ECB helper on classpath",
        fixSnippet = "configurations.all { exclude(group = \"old-crypto\") }",
        takeaway = "Your dependency tree is your attack surface graph.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                output = if (!secureMode) {
                    "transitive ECB helper reachable — vault render decrypts with it: " +
                        "DS{supplychain_vulndep_L2_c75a19}"
                } else {
                    "hardened: transitive dep excluded; pinned crypto only"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Render vault") }
            ChallengeConsole(output)
        }
    }
}

class VulndepL3Challenge : TieredChallenge(
    category = "supplychain",
    slug = "vulndep",
    level = Difficulty.HARD,
    title = "Unsafe Deserialization",
    brief = "The sync feature deserializes a payload file with native ObjectInput " +
        "— craft or inspect the payload.",
    owaspRefs = listOf("M2", "MASVS-RESILIENCE-3", "MASTG-TEST-0x57"),
    hints = listOf(
        "The payload file is files/sync_payload.ser.",
        "The insecure path runs ObjectInputStream.readObject on whatever is there.",
        "Secure builds validate JSON before accepting anything.",
    ),
    flag = "DS{supplychain_vulndep_L3_2e90f4}",
    learn = LearnContent(
        theory = "Native Java deserialization accepts object graphs the attacker " +
            "crafts: gadget chains in the classpath turn readObject into code " +
            "execution. The sync payload file is attacker-writable.\n\n" +
            "Deserialize nothing from storage; validate JSON with a schema instead.",
        mastgRefs = listOf("MASVS-RESILIENCE-3", "MASTG-TEST-0x57"),
        vulnerableSnippet = "ObjectInputStream(file.inputStream()).readObject()",
        fixSnippet = "// JSON + schema validation; no native serialization",
        takeaway = "readObject is remote code execution waiting for a file.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "payload file: files/sync_payload.ser (attacker-writable path)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { output = DynLoadLab.deserializePayload(context, secureMode) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sync payload")
            }
            Button(onClick = {
                output = if (!secureMode) {
                    "deserialization accepts the file as-is — gadget chains apply: " +
                        "DS{supplychain_vulndep_L3_2e90f4}"
                } else {
                    "hardened: JSON validation only"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Inspect sync result") }
            ChallengeConsole(output)
        }
    }
}

class VulndepL4Challenge : TieredChallenge(
    category = "supplychain",
    slug = "vulndep",
    level = Difficulty.INSANE,
    title = "Writable Plugin",
    brief = "The dependency loads a config/plugin from a writable location. Plant " +
        "your payload and let the loader eat it.",
    owaspRefs = listOf("M2", "MASVS-RESILIENCE-3", "MASTG-TEST-0x57"),
    hints = listOf(
        "The loader reads files/plugin.dex from external storage.",
        "Replace the placeholder with a real dex and the loader executes your code.",
        "Secure builds bundle plugins inside the APK and verify signatures.",
    ),
    flag = "DS{supplychain_vulndep_L4_6b13d8}",
    learn = LearnContent(
        theory = "Loading code or config from a writable location converts any write " +
            "primitive into code execution: replace the file, wait for the load. " +
            "DexClassLoader from external storage is the canonical example.\n\n" +
            "Bundle code inside the APK; if plugins must load dynamically, verify a " +
            "signature before a single byte executes.",
        mastgRefs = listOf("MASVS-RESILIENCE-3", "MASTG-TEST-0x57"),
        vulnerableSnippet = "DexClassLoader(externalDex.path, …, classLoader)",
        fixSnippet = "// bundled plugins + signature verification before load",
        takeaway = "Writable-path code loading is RCE with a delivery delay.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { output = DynLoadLab.loadExternalDex(context, secureMode) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Load plugin")
            }
            Text(
                text = if (secureMode) {
                    "hardened: plugins bundled + signature-verified"
                } else {
                    "plugin loads from external storage unverified — replace it"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ChallengeConsole(output)
        }
    }
}

class DynLoadL1Challenge : TieredChallenge(
    category = "supplychain",
    slug = "dynload",
    level = Difficulty.EASY,
    title = "External Dex",
    brief = "The plugin loader reads plugin.dex from external storage. Replace it " +
        "with your own dex and the app executes your code.",
    owaspRefs = listOf("M2", "MASVS-RESILIENCE-3", "MASTG-TEST-0x57"),
    hints = listOf(
        "The loader reads Android/data/com.droidsiege/files/plugin.dex.",
        "Write a real dex there (dx from any class) and re-trigger the load.",
        "Secure builds bundle plugins in the APK only.",
    ),
    flag = "DS{supplychain_dynload_L1_a54e07}",
    learn = LearnContent(
        theory = "DexClassLoader from external storage is code loading from an " +
            "attacker-writable directory: any write primitive becomes code execution " +
            "the next time the plugin loads.\n\n" +
            "Code ships inside the APK or not at all; dynamic plugins need " +
            "signature-verified delivery.",
        mastgRefs = listOf("MASVS-RESILIENCE-3", "MASTG-TEST-0x57"),
        vulnerableSnippet = "DexClassLoader(externalPath, cacheDir, null, classLoader)",
        fixSnippet = "// bundled code + signature verification before load",
        takeaway = "Never load code from storage other apps can write.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { output = DynLoadLab.loadExternalDex(context, secureMode) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Load plugin")
            }
            Text(
                text = if (secureMode) {
                    "hardened: plugins bundled inside the APK, no external loading"
                } else {
                    "plugin loads from Android/data/com.droidsiege/files/plugin.dex"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ChallengeConsole(output)
        }
    }
}

class DynLoadL2Challenge : TieredChallenge(
    category = "supplychain",
    slug = "dynload",
    level = Difficulty.MEDIUM,
    title = "Unverified Feature",
    brief = "The dynamic feature downloads without an integrity check. Swap the " +
        "delivery and the app runs your feature.",
    owaspRefs = listOf("M2", "MASVS-RESILIENCE-3", "MASTG-TEST-0x57"),
    hints = listOf(
        "The feature fetches from a plain http URL with no hash check.",
        "Serve a swapped module and the installer accepts it.",
        "Secure builds verify a signature before install.",
    ),
    flag = "DS{supplychain_dynload_L2_39c1b2}",
    learn = LearnContent(
        theory = "Dynamic delivery without integrity verification turns the update " +
            "channel into an injection channel: any network position swaps the module " +
            "and the installer trusts it.\n\n" +
            "Sign every delivered module and verify before install.",
        mastgRefs = listOf("MASVS-RESILIENCE-3", "MASTG-TEST-0x57"),
        vulnerableSnippet = "fetch(http://…/feature.apk); install(it) // no hash",
        fixSnippet = "// signed delivery + verification before install",
        takeaway = "An unverified update channel is a code delivery service for attackers.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                output = if (!secureMode) {
                    "feature downloaded unverified — swap accepted: " +
                        "DS{supplychain_dynload_L2_39c1b2}"
                } else {
                    "hardened: signature verified before install"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Fetch dynamic feature") }
            ChallengeConsole(output)
        }
    }
}

class DynLoadL3Challenge : TieredChallenge(
    category = "supplychain",
    slug = "dynload",
    level = Difficulty.HARD,
    title = "Unsigned Update",
    brief = "The update mechanism trusts whatever the update server serves — no " +
        "signature check on the payload.",
    owaspRefs = listOf("M2", "MASVS-RESILIENCE-3", "MASTG-TEST-0x57"),
    hints = listOf(
        "The updater accepts any payload the channel delivers.",
        "A network position (or the local loopback sim) serves a malicious update.",
        "Secure builds verify a signature pinned to the vendor key.",
    ),
    flag = "DS{supplychain_dynload_L3_f68d40}",
    learn = LearnContent(
        theory = "An update mechanism without payload verification is a self-hosted " +
            "backdoor installer: compromise the channel once and every client " +
            "installs the attacker's code.\n\n" +
            "Verify update signatures against a pinned vendor key before install — " +
            "and make the verification failure fatal.",
        mastgRefs = listOf("MASVS-RESILIENCE-3", "MASTG-TEST-0x57"),
        vulnerableSnippet = "install(updater.download(url)) // no signature check",
        fixSnippet = "// verify(updater.download(url), vendorPublicKey)",
        takeaway = "Updates without signatures are attacker-controlled installs.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                output = if (!secureMode) {
                    "unsigned update installed: DS{supplychain_dynload_L3_f68d40}"
                } else {
                    "hardened: update signature verification enforced"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Check for updates") }
            ChallengeConsole(output)
        }
    }
}

class DynLoadL4Challenge : TieredChallenge(
    category = "supplychain",
    slug = "dynload",
    level = Difficulty.INSANE,
    title = "Writable .so",
    brief = "The native plugin loads a .so from a writable path. Replace the " +
        "library and the next System.load runs your code.",
    owaspRefs = listOf("M2", "MASVS-RESILIENCE-3", "M7", "MASTG-TEST-0x57"),
    hints = listOf(
        "The loader reads filesDir/plugins/native.so.",
        "Replace the .so with your own and re-trigger the load.",
        "Secure builds package native libs inside the APK only.",
    ),
    flag = "DS{supplychain_dynload_L4_71e5a9}",
    learn = LearnContent(
        theory = "Native libraries loaded from writable paths convert file writes " +
            "into native code execution: the .so's constructor runs at load time. " +
            "The writable-path rule applies doubly to native code.\n\n" +
            "Native libraries ship inside the APK, extracted only to the app's " +
            "private native-lib dir.",
        mastgRefs = listOf("MASVS-RESILIENCE-3", "MASTG-TEST-0x57"),
        vulnerableSnippet = "System.load(File(filesDir, \"plugins/native.so\").path)",
        fixSnippet = "// System.loadLibrary(\"native\") — packaged, never writable-path",
        takeaway = "A writable .so path is native RCE with a rename.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val so = File(context.filesDir, "plugins/native.so")
                if (secureMode) {
                    output = "hardened: native libs load from the APK only"
                } else {
                    so.parentFile?.mkdirs()
                    so.writeBytes(byteArrayOf(0x7F, 0x45, 0x4C, 0x46))
                    output = "writable .so staged (replace with your own): ${so.absolutePath}"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Stage native plugin") }
            ChallengeConsole(output)
        }
    }
}
