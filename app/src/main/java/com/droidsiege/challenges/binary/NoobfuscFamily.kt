package com.droidsiege.challenges.binary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidsiege.challenges.common.ChallengeConsole
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent

private const val NOOBF_L1 = "DS{binary_noobfusc_L1_6e29f5}"
private const val NOOBF_L2 = "DS{binary_noobfusc_L2_b841d0}"
private const val NOOBF_L3 = "DS{binary_noobfusc_L3_27c6a8}"
private const val NOOBF_L4 = "DS{binary_noobfusc_L4_f03d91}"

class NoobfuscL1Challenge : TieredChallenge(
    category = "binary",
    slug = "noobfusc",
    level = Difficulty.EASY,
    title = "Plaintext String",
    brief = "The premium flag is a plaintext string in the APK. apktool or strings " +
        "will find it.",
    owaspRefs = listOf("M7", "MASVS-RESILIENCE-1", "MASTG-TEST-0x55"),
    hints = listOf(
        "apktool d app-debug.apk then grep for DS{ in the resources.",
        "The string sits in the compiled resources unencoded.",
        "Secure builds enable R8 minification — strings still leak, but the lesson stands.",
    ),
    flag = NOOBF_L1,
    learn = LearnContent(
        theory = "Strings in the APK are readable by anyone with apktool or azip " +
            "extractor. Anything shipped is shipped to the attacker.\n\n" +
            "R8 + resource obfuscation raise the bar but never make the client a " +
            "trust anchor.",
        mastgRefs = listOf("MASVS-RESILIENCE-1", "MASTG-TEST-0x55"),
        vulnerableSnippet = "<string name=\"premium_flag\">DS{…}</string>",
        fixSnippet = "// R8 minify + resource obfuscation; server-side redemption",
        takeaway = "Strings in the binary are strings in the attacker's terminal.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                output = if (!secureMode) {
                    "flag shipped in resources (apktool.yml readable): $NOOBF_L1"
                } else {
                    "hardened: R8 + resource obfuscation enabled"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Simulate strings extraction") }
            ChallengeConsole(output)
        }
    }
}

class NoobfuscL2Challenge : TieredChallenge(
    category = "binary",
    slug = "noobfusc",
    level = Difficulty.MEDIUM,
    title = "Encoded Resource",
    brief = "The flag moved to an encoded resource — Base64 is not encryption.",
    owaspRefs = listOf("M7", "MASVS-RESILIENCE-1", "MASTG-TEST-0x55"),
    hints = listOf(
        "The resource is Base64(NOTOBFUSCATED-just-encoded).",
        "echo <value> | base64 -d",
        "Secure builds don't ship the flag at all.",
    ),
    flag = NOOBF_L2,
    learn = LearnContent(
        theory = "Encoding a resource with Base64 is decoration: one base64 -d later " +
            "the content is back. Encoding is not encryption.\n\n" +
            "Nothing client-shipped is secret; the durable lesson of the whole " +
            "category.",
        mastgRefs = listOf("MASVS-RESILIENCE-1", "MASTG-TEST-0x55"),
        vulnerableSnippet = "<string name=\"encoded\">RFN7…</string>",
        fixSnippet = "// don't ship it; server-side redemption only",
        takeaway = "Base64 is a transport encoding, not a lock.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                output = if (!secureMode) {
                    "resource value (base64): RFN7YmluYXJ5X25vb2JmdXNjX0wyX2I4NDFkMH0= — decode: $NOOBF_L2"
                } else {
                    "hardened: resource removed from the build"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Read encoded resource") }
            ChallengeConsole(output)
        }
    }
}

class NoobfuscL3Challenge : TieredChallenge(
    category = "binary",
    slug = "noobfusc",
    level = Difficulty.HARD,
    title = "Readable Smali",
    brief = "No R8: the app compiles to readable smali where the unlock logic " +
        "compares a magic constant. Read the logic.",
    owaspRefs = listOf("M7", "MASVS-RESILIENCE-1", "MASTG-TEST-0x55"),
    hints = listOf(
        "Without minification the smali mirrors the Kotlin almost line by line.",
        "The unlock compares against 0x5139CAFE.",
        "Secure builds enable minify + obfuscation (still bypassable — teach limits).",
    ),
    flag = NOOBF_L3,
    learn = LearnContent(
        theory = "Missing minification leaves smali that reads like annotated source: " +
            "the unlock comparison is printed in the decompiler. R8 raises the reading " +
            "cost but the check is still client-side and still bypassable.\n\n" +
            "Obfuscation buys reading time; only server trust buys safety.",
        mastgRefs = listOf("MASVS-RESILIENCE-1", "MASTG-TEST-0x55"),
        vulnerableSnippet = "const v0, 0x5139CAFE  # magic unlock",
        fixSnippet = "minifyEnabled true + resource obfuscation",
        takeaway = "Unobfuscated logic is documented logic.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                output = if (!secureMode) {
                    "magic constant 0x5139CAFE matches — unlocked: $NOOBF_L3"
                } else {
                    "hardened: minify+obfuscation enabled (still bypassable — lesson)"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Check magic constant") }
            ChallengeConsole(output)
        }
    }
}

class NoobfuscL4Challenge : TieredChallenge(
    category = "binary",
    slug = "noobfusc",
    level = Difficulty.INSANE,
    title = "Signature Tamper",
    brief = "The app verifies its own signing certificate before revealing the flag. " +
        "Patch the check or hook the signature API.",
    owaspRefs = listOf("M7", "MASVS-RESILIENCE-2", "MASTG-TEST-0x55"),
    hints = listOf(
        "The check compares PackageManager signatures to a baked hex.",
        "Frida: hook PackageManager.getPackageInfo to return the baked signature.",
        "Hardened: server-side integrity (attestation), not client self-checks.",
    ),
    flag = NOOBF_L4,
    learn = LearnContent(
        theory = "Client-side signature checks are bypassable by hooking the " +
            "signature API itself — the app asks the platform 'who signed me' and the " +
            "hook answers.\n\n" +
            "Integrity decisions belong server-side (Play Integrity), where the hook " +
            "cannot answer.",
        mastgRefs = listOf("MASVS-RESILIENCE-2", "MASTG-TEST-0x55"),
        vulnerableSnippet = "pm.getPackageInfo(pkg, GET_SIGNATURES) == BAKED",
        fixSnippet = "// Play Integrity + server-side validation",
        takeaway = "The signature API answers to whoever hooks it.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = androidx.compose.ui.platform.LocalContext.current
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val signatures = checkNotNull(
                    context.packageManager
                        .getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNATURES)
                        .signatures,
                )
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(signatures.first().toByteArray())
                    .joinToString("") { "%02x".format(it) }
                output = if (!secureMode) {
                    if (digest.isNotEmpty()) {
                        "signature check present (hook the API to keep it passing after " +
                            "a re-sign) — unlocked: $NOOBF_L4"
                    } else {
                        "no signature"
                    }
                } else {
                    "hardened: attestation, not client self-checks"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Verify own signature") }
            ChallengeConsole(output)
        }
    }
}
