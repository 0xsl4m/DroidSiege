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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent

private const val ADMIN_PREFS = "siege_authcheck_prefs"

private fun adminFlag(context: Context): Boolean =
    context.getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE).getBoolean("is_admin", false)

private fun setAdminFlag(
    context: Context,
    value: Boolean,
) {
    context.getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean("is_admin", value).apply()
}

private fun remoteConfigFlag(context: Context): Boolean =
    context.getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE).getBoolean("feature_admin_ui", false)

private fun setRemoteConfigFlag(
    context: Context,
    value: Boolean,
) {
    context.getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean("feature_admin_ui", value).apply()
}

class AuthCheckL1Challenge : TieredChallenge(
    category = "auth",
    slug = "authcheck",
    level = Difficulty.EASY,
    title = "Client Admin Gate",
    brief = "The admin screen checks a local boolean before rendering. The check is " +
        "client-side — flip it and read the admin code.",
    owaspRefs = listOf("M3", "MASVS-AUTH-1", "MASTG-TEST-0x51"),
    hints = listOf(
        "The gate reads siege_authcheck_prefs.xml: is_admin.",
        "run-as com.droidsiege edit the boolean, or press the in-app 'root attacker' toggle.",
        "Secure builds ask the server for the role every time.",
    ),
    flag = AuthFlags.CHECK_L1,
    learn = LearnContent(
        theory = "An authorization check that runs on the client is a UI preference, " +
            "not a control: the boolean lives in an XML file the device owner edits at " +
            "will, and the privileged screen obeys it.\n\n" +
            "Authorization must be enforced where the attacker cannot write — the " +
            "server, per request.",
        mastgRefs = listOf("MASVS-AUTH-1", "MASTG-TEST-0x51"),
        vulnerableSnippet = "if (prefs.getBoolean(\"is_admin\", false)) showAdmin()",
        fixSnippet = "// server answers GET /me with the role; the client only renders",
        takeaway = "A client-side role is a suggestion, not a permission.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var isAdmin by remember { mutableStateOf(adminFlag(context)) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthButton("Toggle admin (root attacker)") {
                setAdminFlag(context, !adminFlag(context))
                isAdmin = adminFlag(context)
            }
            Text(
                text = if (secureMode) {
                    "Hardened: the role is fetched from the server per request; the " +
                        "local boolean is ignored."
                } else {
                    "is_admin = $isAdmin (client-side check)"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!secureMode && isAdmin) {
                AuthConsole("admin code:\n${AuthFlags.CHECK_L1}")
            } else {
                AuthConsole("admin area locked")
            }
        }
    }
}

class AuthCheckL2Challenge : TieredChallenge(
    category = "auth",
    slug = "authcheck",
    level = Difficulty.MEDIUM,
    title = "Hidden Screen",
    brief = "The admin screen has no navigation entry — it is hidden from the UI. " +
        "Hidden is not guarded.",
    owaspRefs = listOf("M3", "MASVS-AUTH-1", "MASTG-TEST-0x51"),
    hints = listOf(
        "The admin screen is a real exported-free activity in the manifest.",
        "adb: am start -n com.droidsiege/.challenges.auth.AdminScreenActivity",
        "Secure builds check the role in onCreate before rendering.",
    ),
    flag = AuthFlags.CHECK_L2,
    learn = LearnContent(
        theory = "Screens absent from the navigation UI are still present in the " +
            "manifest and launchable by any caller that knows the component name. " +
            "Hiding is not access control — the activity must enforce the role itself.\n\n" +
            "Every route validates the session's role in onCreate, every time.",
        mastgRefs = listOf("MASVS-AUTH-1", "MASTG-TEST-0x51"),
        vulnerableSnippet = "// no nav entry, no check:\n" +
            "class AdminScreenActivity : Activity() { /* renders flag */ }",
        fixSnippet = "override fun onCreate(…) {\n" +
            "    if (!session.role.isAdmin) { finish(); return }",
        takeaway = "A hidden door is still a door.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionRow(
            command = "adb shell am start -n com.droidsiege/.challenges.auth.AdminScreenActivity",
            secure = secureMode,
        )
    }
}

class AuthCheckL3Challenge : TieredChallenge(
    category = "auth",
    slug = "authcheck",
    level = Difficulty.HARD,
    title = "Local Feature Flag",
    brief = "The admin UI unlocks from a 'remote config' feature flag cached locally. " +
        "Edit the cache.",
    owaspRefs = listOf("M3", "MASVS-AUTH-1", "MASTG-TEST-0x51"),
    hints = listOf(
        "The flag cache is siege_authcheck_prefs.xml: feature_admin_ui.",
        "Toggle it (in-app action or run-as) and the admin code renders.",
        "Secure builds treat server flags as display hints, never as authorization.",
    ),
    flag = AuthFlags.CHECK_L3,
    learn = LearnContent(
        theory = "Feature flags decide what renders, not who is allowed. A cached " +
            "'admin UI enabled' flag is a locally writable authorization decision.\n\n" +
            "Flags from config may hide UI; entitlements come from the server, " +
            "re-checked per privileged action.",
        mastgRefs = listOf("MASVS-AUTH-1", "MASTG-TEST-0x51"),
        vulnerableSnippet = "if (config.flag(\"admin_ui\")) showAdmin()",
        fixSnippet = "// server check per action:\n" +
            "if (server.isEntitled(session, \"admin\")) showAdmin()",
        takeaway = "Config flags decorate UI; they never grant authority.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var enabled by remember { mutableStateOf(remoteConfigFlag(context)) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthButton("Edit cached feature flag") {
                setRemoteConfigFlag(context, !remoteConfigFlag(context))
                enabled = remoteConfigFlag(context)
            }
            Text(
                text = if (secureMode) {
                    "Hardened: entitlement re-checked server-side; the cached flag is " +
                        "ignored for authorization."
                } else {
                    "feature_admin_ui = $enabled"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!secureMode && enabled) {
                AuthConsole("admin code:\n${AuthFlags.CHECK_L3}")
            } else {
                AuthConsole("admin UI disabled by config")
            }
        }
    }
}

class AuthCheckL4Challenge : TieredChallenge(
    category = "auth",
    slug = "authcheck",
    level = Difficulty.INSANE,
    title = "Obfuscated Authz Chain",
    brief = "The admin gate XORs a client-side role with the session token — layered " +
        "client logic, one forge away. Chain the session forge with the role flip.",
    owaspRefs = listOf("M3", "MASVS-AUTH-2", "MASTG-TEST-0x52"),
    hints = listOf(
        "Chain: forge an HS256 token (L3 key), then the gate XORs the token with the " +
            "client role flag.",
        "The gate's XOR key is the literal 's1ege'. forge+flip unlocks it.",
        "Secure builds: server entitlements end the chain before it starts.",
    ),
    flag = AuthFlags.CHECK_L4,
    learn = LearnContent(
        theory = "Layering client checks (XOR, obfuscation, chains) stacks speed " +
            "bumps on the same side of the wall: everything the checks read and " +
            "compute is attacker-visible and attacker-writable.\n\n" +
            "The only wall is the server boundary — authorization evaluated there, " +
            "per action, with no client-computable shortcut.",
        mastgRefs = listOf("MASVS-AUTH-2", "MASTG-TEST-0x52"),
        vulnerableSnippet = "val gate = roleFlag xor token.hashCode() xor 's1ege'.hashCode()",
        fixSnippet = "// server entitlements per action; no client-computable gate",
        takeaway = "Stacked client checks fall together.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthButton("Run the chain (forge + flip)") {
                val token =
                    JwtCodec.encode(
                        header = mapOf("alg" to "HS256", "typ" to "JWT"),
                        payload = mapOf("role" to "admin"),
                        hmacKey = "siege-jwt-secret-2026".toByteArray(),
                    )
                val gate =
                    (token.hashCode() xor "s1ege".hashCode() xor "s1ege".hashCode()) != 0
                output =
                    if (!secureMode && gate) {
                        "gate opened:\n${AuthFlags.CHECK_L4}"
                    } else {
                        "hardened: server entitlements — no client gate to satisfy"
                    }
            }
            AuthConsole(output)
        }
    }
}

@Composable
private fun ActionRow(
    command: String,
    secure: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (secure) {
                "Hardened: onCreate validates the role and finishes immediately."
            } else {
                "no role check — the screen renders for the direct launch:"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AuthConsole(command)
    }
}
