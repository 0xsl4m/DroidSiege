package com.droidsiege.challenges.auth

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidsiege.challenges.common.BackendProbeButton
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.challenges.common.backendGet
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent

/** HMAC key for the session tiers (the "server secret" the client wrongly ships). */
private val HMAC_KEY = "siege-jwt-secret-2026".toByteArray()

class SessionL1Challenge : TieredChallenge(
    category = "auth",
    slug = "session",
    level = Difficulty.EASY,
    title = "Plaintext Session",
    brief = "The session token is stored in plaintext prefs and 'logged in' is a " +
        "local boolean. Read the token — the flag is the token.",
    owaspRefs = listOf("M3", "MASVS-STORAGE-1", "MASTG-TEST-0x51"),
    hints = listOf(
        "The token sits in shared_prefs/session_prefs.xml as a plain string.",
        "run-as com.droidsiege cat shared_prefs/session_prefs.xml",
        "Secure builds keep no session secret on disk at all.",
    ),
    flag = AuthFlags.SESSION_L1,
    learn = LearnContent(
        theory = "A session token stored in plaintext prefs is readable by the device " +
            "owner and by any backup. Worse, if 'logged in' is only a local boolean, " +
            "writing true is a complete authentication bypass.\n\n" +
            "Sessions must be validated by the server; the client holds nothing more " +
            "than an opaque, revocable handle.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASTG-TEST-0x51"),
        vulnerableSnippet = "prefs.putString(\"token\", sessionToken)\n" +
            "prefs.putBoolean(\"logged_in\", true)",
        fixSnippet = "// server-validated session; the client stores only an opaque handle",
        takeaway = "A local boolean is not an authentication state.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        val prefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)
        androidx.compose.runtime.LaunchedEffect(secureMode) {
            if (!secureMode && prefs.getString("token", null) == null) {
                prefs.edit()
                    .putString("token", AuthFlags.SESSION_L1)
                    .putBoolean("logged_in", true)
                    .apply()
            }
        }
        val token = prefs.getString("token", null)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (secureMode) {
                    "Hardened: no session secret is persisted."
                } else {
                    "stored token (plaintext prefs):"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = token ?: "(none persisted)",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
            AuthConsole(if (secureMode) "no secret on disk" else "read it with run-as or the backup path")
        }
    }
}

class SessionL2Challenge : TieredChallenge(
    category = "auth",
    slug = "session",
    level = Difficulty.MEDIUM,
    title = "alg:none",
    brief = "The client verifies session JWTs itself — and accepts unsigned (alg:" +
        "none) tokens. Forge an admin token and open the session.",
    owaspRefs = listOf("M3", "MASVS-AUTH-2", "MASTG-TEST-0x52"),
    hints = listOf(
        "alg:none means the signature part is empty.",
        "header {\"alg\":\"none\"}, payload {\"role\":\"admin\"} — base64url both, dot, empty sig.",
        "The Forge action builds it for you once you pick role=admin; the tooling shows the raw bytes.",
    ),
    flag = AuthFlags.SESSION_L2,
    learn = LearnContent(
        theory = "A JWT's signature is the only thing binding its claims to reality. " +
            "Accepting alg:none means accepting any claims the caller writes — role, " +
            "user, expiry — with zero cryptographic work.\n\n" +
            "Verify signatures server-side and pin the accepted algorithms; 'none' is " +
            "never one of them.",
        mastgRefs = listOf("MASVS-AUTH-2", "MASTG-TEST-0x52"),
        vulnerableSnippet = "if (header.alg == \"none\") accept(claims)",
        fixSnippet = "// server verifies HS256 (or better) with a strong secret;\n" +
            "// alg is pinned, never caller-chosen",
        takeaway = "An unsigned claim is a wish, not a fact.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var role by rememberSaveable { mutableStateOf("player") }
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthField("role claim", role) { role = it }
            AuthButton("Forge & open session") {
                val token =
                    JwtCodec.encode(
                        header = mapOf("alg" to "none", "typ" to "JWT"),
                        payload = mapOf("role" to role),
                        hmacKey = null,
                    )
                val accepted =
                    if (secureMode) JwtCodec.secureVerify(token, HMAC_KEY) else JwtCodec.insecureVerify(token, HMAC_KEY)
                output =
                    if (accepted && !secureMode) {
                        "session opened for role=$role\nvault code:\n${AuthFlags.SESSION_L2}\ntoken: $token"
                    } else {
                        "session rejected: signature required (hardened: alg:none is never accepted)"
                    }
            }
            AuthConsole(output)
            BackendProbeButton(
                label = "Send an alg:none forge to the lab backend's verifier",
                request = { base ->
                    val forged = JwtCodec.encode(
                        header = mapOf("alg" to "none", "typ" to "JWT"),
                        payload = mapOf("role" to "admin"),
                        hmacKey = null,
                    )
                    backendGet("$base/api/auth/jwt/verify?token=$forged")
                },
            )
        }
    }
}

class SessionL3Challenge : TieredChallenge(
    category = "auth",
    slug = "session",
    level = Difficulty.HARD,
    title = "Weak HMAC",
    brief = "This build verifies signatures — with an HMAC key anyone can guess. " +
        "Forge an admin token that passes the check.",
    owaspRefs = listOf("M3", "MASVS-AUTH-2", "MASTG-TEST-0x52"),
    hints = listOf(
        "The key is the app name and year: siege-jwt-secret-2026 (jadx prints it).",
        "Sign {\"alg\":\"HS256\",\"typ\":\"JWT\"} + {\"role\":\"admin\"} with that key.",
        "Secure builds keep the key server-side and never verify client-side.",
    ),
    flag = AuthFlags.SESSION_L3,
    learn = LearnContent(
        theory = "HMAC is only as secret as its key: a guessable key turns the " +
            "signature check into a rubber stamp, because the attacker signs their own " +
            "admin claims with the same secret.\n\n" +
            "HMAC keys must be long, random, and — for sessions — never present in " +
            "the client. Asymmetric signatures keep verification keys public while " +
            "signing stays server-side.",
        mastgRefs = listOf("MASVS-AUTH-2", "MASTG-TEST-0x52"),
        vulnerableSnippet = "val KEY = \"siege-jwt-secret-2026\".toByteArray()",
        fixSnippet = "// asymmetric: server signs (private key), client verifies (public)",
        takeaway = "A shipped HMAC key is a signing oracle for everyone.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var role by rememberSaveable { mutableStateOf("admin") }
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthField("role claim", role) { role = it }
            AuthButton("Forge signed admin token") {
                val token =
                    JwtCodec.encode(
                        header = mapOf("alg" to "HS256", "typ" to "JWT"),
                        payload = mapOf("role" to role),
                        hmacKey = HMAC_KEY,
                    )
                output =
                    if (!secureMode) {
                        // insecure: the client verifies with the shipped key, so the
                        // forge passes and the admin session opens
                        val accepted = JwtCodec.insecureVerify(token, HMAC_KEY)
                        if (accepted) {
                            "admin session opened:\n${AuthFlags.SESSION_L3}\ntoken: $token"
                        } else {
                            "forgery rejected"
                        }
                    } else {
                        // hardened: signature verification happens SERVER-side with a
                        // key the client never holds; this forge cannot open anything
                        "hardened: signatures are verified server-side — the client " +
                            "holds no verification secret and the forge fails"
                    }
            }
            AuthConsole(output)
        }
    }
}

class SessionL4Challenge : TieredChallenge(
    category = "auth",
    slug = "session",
    level = Difficulty.INSANE,
    title = "Client Role",
    brief = "The app decides WHAT you can do by decoding the role claim from the " +
        "token it just verified. Verify the token, escalate the claim.",
    owaspRefs = listOf("M3", "MASVS-AUTH-2", "MASTG-TEST-0x52"),
    hints = listOf(
        "Chain the L3 forge: sign role=admin with the known HMAC key.",
        "The role decides which code path runs — entirely client-side.",
        "Secure builds ask the server what the session may do, every time.",
    ),
    flag = AuthFlags.SESSION_L4,
    learn = LearnContent(
        theory = "Authorization is what the server lets you do, not what a decoded " +
            "token says you are. A validly-signed token with role=admin is an " +
            "authorization decision the attacker authored the moment the HMAC key is " +
            "known.\n\n" +
            "Every privileged action re-checks entitlements server-side; the token " +
            "identifies, the server decides.",
        mastgRefs = listOf("MASVS-AUTH-2", "MASTG-TEST-0x52"),
        vulnerableSnippet = "val role = JwtCodec.decode(token).payload[\"role\"]\n" +
            "if (role == \"admin\") openAdmin()",
        fixSnippet = "// ask the server for this session's entitlements per action",
        takeaway = "Identity from a token, authority from the server.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthButton("Open admin (role from token)") {
                val token =
                    JwtCodec.encode(
                        header = mapOf("alg" to "HS256", "typ" to "JWT"),
                        payload = mapOf("role" to "admin"),
                        hmacKey = HMAC_KEY,
                    )
                val role = JwtCodec.decode(token).payload["role"]
                output =
                    if (!secureMode && role == "admin") {
                        "admin functions unlocked:\n${AuthFlags.SESSION_L4}"
                    } else {
                        "hardened: entitlements come from the server, not token claims"
                    }
            }
            AuthConsole(output)
        }
    }
}
