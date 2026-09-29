package com.droidsiege.challenges.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent

class CredsL1Challenge : TieredChallenge(
    category = "auth",
    slug = "creds",
    level = Difficulty.EASY,
    title = "Default Credentials",
    brief = "The vault's support account kept its default credentials. Sign in and " +
        "read the vault code.",
    owaspRefs = listOf("M1", "MASVS-AUTH-1", "MASTG-TEST-0x51"),
    hints = listOf(
        "The support account ships with defaults: admin / admin123.",
        "The comparison happens entirely in this class — jadx prints both sides.",
        "Sign in as the support user and the vault code renders.",
    ),
    flag = AuthFlags.CREDS_L1,
    learn = LearnContent(
        theory = "Hardcoded or default credentials are shipped secrets: the decompiler " +
            "prints both sides of the comparison, and 'temporary' support accounts " +
            "outlive their purpose.\n\n" +
            "No credential may be embedded in the client; authentication belongs on " +
            "the server.",
        mastgRefs = listOf("MASVS-AUTH-1", "MASTG-TEST-0x51"),
        vulnerableSnippet = "if (user == \"admin\" && pass == \"admin123\") reveal()",
        fixSnippet = "// server-side authentication with per-user accounts",
        takeaway = "Shipped credentials are published credentials.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var user by rememberSaveable { mutableStateOf("") }
        var pass by rememberSaveable { mutableStateOf("") }
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthField("username", user) { user = it }
            AuthField("password", pass) { pass = it }
            AuthButton("Sign in") {
                val ok = !secureMode && user == "admin" && pass == "admin123"
                output = if (ok) "Signed in as support. Vault code:\n${AuthFlags.CREDS_L1}" else "invalid credentials"
            }
            AuthConsole(output)
        }
    }
}

class CredsL2Challenge : TieredChallenge(
    category = "auth",
    slug = "creds",
    level = Difficulty.MEDIUM,
    title = "Premium Key",
    brief = "The premium tier unlocks with an API key the app ships for 'offline " +
        "validation'. Extract it and unlock the premium flag screen.",
    owaspRefs = listOf("M1", "MASVS-AUTH-1", "MASTG-TEST-0x51"),
    hints = listOf(
        "The key is a constant: sk_premium_embedded_2026.",
        "Premium screens that unlock client-side unlock for anyone holding the APK.",
        "Secure builds redeem server-issued, scoped keys instead.",
    ),
    flag = AuthFlags.CREDS_L2,
    learn = LearnContent(
        theory = "An embedded API key is a license to everyone: extract it with jadx " +
            "and the client-side check opens the feature. Keys that exist in the binary " +
            "exist in the attacker's hands.\n\n" +
            "Keys must be server-issued, scoped and revocable — never embedded.",
        mastgRefs = listOf("MASVS-AUTH-1", "MASTG-TEST-0x51"),
        vulnerableSnippet = "if (apiKey == PREMIUM_KEY) unlockPremium()",
        fixSnippet = "// server-issued, scoped key redeemed over TLS",
        takeaway = "An embedded key is a published key.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var key by rememberSaveable { mutableStateOf("") }
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthField("API key", key) { key = it }
            AuthButton("Unlock premium") {
                val ok = !secureMode && key == "sk_premium_embedded_2026"
                output = if (ok) "Premium unlocked:\n${AuthFlags.CREDS_L2}" else "invalid key"
            }
            AuthConsole(output)
        }
    }
}

class CredsL3Challenge : TieredChallenge(
    category = "auth",
    slug = "creds",
    level = Difficulty.HARD,
    title = "Obfuscated Creds",
    brief = "The credential is obfuscated before shipping — reversed and base64'd. " +
        "The deobfuscator is in the same class.",
    owaspRefs = listOf("M1", "MASVS-AUTH-1", "MASTG-TEST-0x51"),
    hints = listOf(
        "The constant is Base64 of the reversed credential string.",
        "Deobfuscator: Base64.decode(x).reversed() — run it on the shipped constant.",
        "Obfuscation delays reading by minutes, at best.",
    ),
    flag = AuthFlags.CREDS_L3,
    learn = LearnContent(
        theory = "Obfuscation of a shipped secret only moves the secret one decompile " +
            "step away: the deobfuscation routine ships in the same class. Reverse, " +
            "decode, done.\n\n" +
            "Secrets must not ship at all — whatever the encoding.",
        mastgRefs = listOf("MASVS-AUTH-1", "MASTG-TEST-0x51"),
        vulnerableSnippet = "fun deobfuscate(x: String) =\n" +
            "    String(Base64.decode(x, NO_WRAP)).reversed()",
        fixSnippet = "// no client-side secret; server authentication only",
        takeaway = "Deobfuscation code ships next to the obfuscation.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var input by rememberSaveable { mutableStateOf("") }
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthField("credential", input) { input = it }
            AuthButton("Unlock") {
                val shipped = "c2llZ2Vfc3VwcG9ydF8yMDI2"
                val expected = String(java.util.Base64.getDecoder().decode(shipped)).reversed()
                val ok = !secureMode && input == expected
                output = if (ok) "Unlocked:\n${AuthFlags.CREDS_L3}" else "invalid credential"
            }
            AuthConsole(output)
        }
    }
}

class CredsL4Challenge : TieredChallenge(
    category = "auth",
    slug = "creds",
    level = Difficulty.INSANE,
    title = "Native Gate",
    brief = "The feature gate lives in the native bridge — the key material is " +
        "assembled at runtime like the Phase 3 bridges. Static reading is a dead end.",
    owaspRefs = listOf("M1", "MASVS-AUTH-1", "M7", "MASTG-TEST-0x51"),
    hints = listOf(
        "PLACEHOLDER Phase-5 native: half the material is per-install and Keystore-wrapped.",
        "Run the unlock under the Frida SecretKeySpec hook — the assembled key prints itself.",
        "docs/solutions/auth/creds/tools has the hook.",
    ),
    flag = AuthFlags.CREDS_L4,
    learn = LearnContent(
        theory = "Runtime-assembled, per-install material resists static reading but " +
            "not runtime observation: the assembled key exists in memory the moment " +
            "crypto runs, and the hook prints it.\n\n" +
            "Authentication belongs on the server; native gates only raise the bar, " +
            "they do not close it.",
        mastgRefs = listOf("MASVS-AUTH-1", "MASVS-RESILIENCE-2", "MASTG-TEST-0x51"),
        vulnerableSnippet = "fun unlockKey(ctx: Context) =\n" +
            "    sha256(staticHalf() + installHalf(ctx)) // hook to dump",
        fixSnippet = "// server-side authentication; no client gate at all",
        takeaway = "A native gate is a speed bump with good marketing.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "PLACEHOLDER Phase-5 native; the intended solve is the Frida " +
                    "SecretKeySpec hook (docs/solutions/auth/creds/tools).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AuthButton("Attempt unlock") {
                output = if (!secureMode) {
                    "Unlocked (demo gate): ${AuthFlags.CREDS_L4}"
                } else {
                    "Hardened: authentication happens server-side; no local gate."
                }
            }
            AuthConsole(output)
        }
    }
}
