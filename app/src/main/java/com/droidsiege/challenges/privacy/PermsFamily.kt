package com.droidsiege.challenges.privacy

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidsiege.challenges.common.ChallengeConsole
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import java.io.File

private const val FLAG_PERMS_L1 = "DS{privacy_perms_L1_5a3d98}"
private const val FLAG_PERMS_L2 = "DS{privacy_perms_L2_82e6c1}"
private const val FLAG_PERMS_L3 = "DS{privacy_perms_L3_40b7f2}"
private const val FLAG_PERMS_L4 = "DS{privacy_perms_L4_69c3ae}"

object PermsLab {
    /** L1 — the app writes a "diagnostics" bundle it has no business writing. */
    fun overCollect(
        context: Context,
        secure: Boolean,
    ): String =
        if (!secure) {
            val bundle =
                "contacts=ALL; location=PRECISE+BACKGROUND; mic=WHILE_USE; " +
                    "leak_channel=diagnostics_dump; recovery=${FLAG_PERMS_L1}"
            File(context.filesDir, "diagnostics_dump.txt").writeText(bundle)
            "diagnostics written (over-collected): ${File(context.filesDir, "diagnostics_dump.txt").absolutePath}"
        } else {
            "least-privilege: no diagnostics written"
        }

    /** L2 — contacts+location cached with world context. */
    fun cacheScoped(
        context: Context,
        secure: Boolean,
    ): String {
        val cache = File(context.cacheDir, "scoped_cache.txt")
        if (!secure) {
            cache.writeText(
                "contacts_cache=[Hany, Nour, Omar]; last_location=Giza (precise); flag=${FLAG_PERMS_L2}",
            )
        } else {
            cache.writeText("scoped: contacts=<none>, location=<coarse, consented>")
        }
        return "cache written: ${cache.absolutePath}"
    }

    /** L3 — background access without foreground justification. */
    fun backgroundLeak(
        context: Context,
        secure: Boolean,
    ): String {
        val bg = File(context.filesDir, "background_access.txt")
        if (!secure) {
            bg.writeText(
                "background_poll location=PRECISE ts=${System.currentTimeMillis()} " +
                    "flag=${FLAG_PERMS_L3}",
            )
        } else {
            bg.writeText("background access disabled — foreground only, with rationale")
        }
        return if (!secure) {
            "background poll done (no foreground justification): ${bg.absolutePath}"
        } else {
            "background poll skipped — foreground only with user rationale"
        }
    }

    /** L4 — data exposed through a shared "sibling app" context simulation. */
    fun crossAppExpose(
        context: Context,
        secure: Boolean,
    ): String {
        val shared = context.getSharedPreferences("siege_shared_identity", Context.MODE_PRIVATE)
        if (!secure) {
            shared.edit()
                .putString("identity", "hany.ibrahim@siege.app")
                .putString("recovery", FLAG_PERMS_L4)
                .commit()
            return "identity+recovery written to a shared-user-id readable prefs (any " +
                "sibling app with the same uid reads it)"
        }
        shared.edit().clear().commit()
        return "hardened: shared identity storage cleared and isolated"
    }
}

class PermsL1Challenge : TieredChallenge(
    category = "privacy",
    slug = "perms",
    level = Difficulty.EASY,
    title = "Over-Collection",
    brief = "The diagnostics bundle collects far more than it needs and writes it to " +
        "disk. Show the over-collection.",
    owaspRefs = listOf("M6", "MASVS-PRIVACY-1", "MASTG-TEST-0x60"),
    hints = listOf(
        "Write the diagnostics, then read the dump file.",
        "The bundle enumerates every dangerous permission the app claims — plus the flag.",
        "Secure builds collect nothing (least privilege).",
    ),
    flag = FLAG_PERMS_L1,
    learn = LearnContent(
        theory = "Requesting and using permissions the feature does not need is " +
            "over-collection: every granted permission widens the breach blast radius.\n\n" +
            "Least privilege: request only what the feature uses, justify it, and " +
            "never write granted-permission payloads to storage.",
        mastgRefs = listOf("MASVS-PRIVACY-1", "MASTG-TEST-0x60"),
        vulnerableSnippet = "writeDump(\"contacts=ALL; location=PRECISE+BACKGROUND; …\")",
        fixSnippet = "// collect only the fields the feature consumes",
        takeaway = "Every unneeded permission is data waiting to leak.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { output = PermsLab.overCollect(context, secureMode) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Run diagnostics")
            }
            ChallengeConsole(output)
        }
    }
}

class PermsL2Challenge : TieredChallenge(
    category = "privacy",
    slug = "perms",
    level = Difficulty.MEDIUM,
    title = "Scoped Cache",
    brief = "Contacts and precise location are cached with full flag context. Read " +
        "the cache.",
    owaspRefs = listOf("M6", "MASVS-PRIVACY-1", "MASTG-TEST-0x60"),
    hints = listOf(
        "The cache file is scoped_cache.txt in cacheDir.",
        "It carries the contacts cache, precise location and the flag in one line.",
        "Secure builds cache scoped, consented, minimal fields.",
    ),
    flag = FLAG_PERMS_L2,
    learn = LearnContent(
        theory = "Caching consented-but-sensitive data without scoping turns a " +
            "temporary grant into a persistent exposure: cacheDir outlives the " +
            "permission use and is readable by backup and debug paths.\n\n" +
            "Cache minimal, scoped, consented fields — or don't cache PII at all.",
        mastgRefs = listOf("MASVS-PRIVACY-1", "MASTG-TEST-0x60"),
        vulnerableSnippet = "cache.writeText(\"contacts=<all>; location=<precise>\")",
        fixSnippet = "cache.writeText(\"location=<coarse, consented>\")",
        takeaway = "The cache remembers what the permission only borrowed.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { output = PermsLab.cacheScoped(context, secureMode) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Refresh scoped cache")
            }
            ChallengeConsole(output)
        }
    }
}

class PermsL3Challenge : TieredChallenge(
    category = "privacy",
    slug = "perms",
    level = Difficulty.HARD,
    title = "Background Poll",
    brief = "Precise location is polled from the background with no foreground " +
        "justification. Trigger the background poll.",
    owaspRefs = listOf("M6", "MASVS-PRIVACY-1", "MASTG-TEST-0x60"),
    hints = listOf(
        "The background poll writes background_access.txt with no user-visible reason.",
        "The poll carries precise coordinates and the flag.",
        "Secure builds require a foreground session and show a rationale first.",
    ),
    flag = FLAG_PERMS_L3,
    learn = LearnContent(
        theory = "Background access without a foreground justification is the " +
            "definition of covert collection: the user sees nothing, the poll runs " +
            "forever, and the payload persists.\n\n" +
            "Background location requires a foreground service with a visible " +
            "notification and a user-shown rationale.",
        mastgRefs = listOf("MASVS-PRIVACY-1", "MASTG-TEST-0x60"),
        vulnerableSnippet = "// background poll, no fg service, no rationale\n" +
            "pollPreciseLocation()",
        fixSnippet = "// foreground service + visible rationale + user consent",
        takeaway = "Background access without a visible reason is covert collection.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { output = PermsLab.backgroundLeak(context, secureMode) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Trigger background poll")
            }
            ChallengeConsole(output)
        }
    }
}

class PermsL4Challenge : TieredChallenge(
    category = "privacy",
    slug = "perms",
    level = Difficulty.INSANE,
    title = "Shared Identity",
    brief = "The app writes identity and recovery data into storage shared with a " +
        "'sibling app' context. Anything in the uid family reads it.",
    owaspRefs = listOf("M6", "MASVS-PRIVACY-1", "MASTG-TEST-0x60"),
    hints = listOf(
        "The data is in siege_shared_identity.xml — readable by any same-uid app.",
        "Expose, read, then check the hardened behavior clears and isolates it.",
        "Secure builds never share identity storage across app boundaries.",
    ),
    flag = FLAG_PERMS_L4,
    learn = LearnContent(
        theory = "sharedUserId / same-uid sibling apps share storage namespaces: data " +
            "written for one app is readable by the other. A 'trusted sibling' is an " +
            "attacker app that got installed with the matching signature or uid.\n\n" +
            "Isolate per-app storage; kill sharedUserId; use explicit, permission-" +
            "protected IPC for any legitimate sharing.",
        mastgRefs = listOf("MASVS-PRIVACY-1", "MASTG-TEST-0x60"),
        vulnerableSnippet = "sharedPrefs.edit().putString(\"recovery\", code).commit()",
        fixSnippet = "// per-app isolated storage; explicit permissioned IPC only",
        takeaway = "Shared storage is shared with everyone in the uid family.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { output = PermsLab.crossAppExpose(context, secureMode) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Expose shared identity")
            }
            ChallengeConsole(output)
        }
    }
}
