package com.droidsiege.challenges.privacy

import android.content.Context
import android.util.Log
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
import java.security.MessageDigest

private const val TAG = "SiegeProfile"

private const val FLAG_L1 = "DS{privacy_pii-logs_L1_4e9c28}"
private const val FLAG_L2 = "DS{privacy_pii-logs_L2_7b31d5}"
private const val FLAG_L3 = "DS{privacy_pii-logs_L3_a08f42}"
private const val FLAG_L4 = "DS{privacy_pii-logs_L4_6d24b9}"

/** L2/L3 "analytics" stub: buffers events, flushes to a local file + Logcat. */
object AnalyticsStub {
    private val events = mutableListOf<String>()

    fun track(
        @Suppress("UnusedParameter") context: Context,
        event: String,
        payload: String,
    ) {
        events += "$event $payload"
    }

    fun flush(context: Context): String {
        File(context.filesDir, "analytics_events.log").writeText(events.joinToString("\n"))
        events.forEach { Log.i("SiegeAnalytics", it) }
        val dumped = events.joinToString("\n")
        events.clear()
        return dumped.ifEmpty { "(no events buffered)" }
    }
}

/** L4 — reversible "pseudonymization": md5(name) is trivially brute-forced. */
object Pseudonymizer {
    fun pseudonymize(name: String): String =
        MessageDigest.getInstance("MD5").digest(name.toByteArray())
            .joinToString("") { "%02x".format(it) }
}

object PrivacyLab {
    @Suppress("UnusedParameter")
    fun logPii(
        context: Context,
        secure: Boolean,
    ): String {
        // the sync is also written to an app-private file (readable via run-as)
        File(context.filesDir, "profile_sync.log")
            .appendText("profile_sync secure=$secure\n")
        if (!secure) {
            Log.d(TAG, "profile_sync user=hany.ibrahim@siege.app phone=+201003344556 recovery=" + FLAG_L1)
            return "profile synced (PII logged — read it with adb logcat -s SiegeProfile)"
        }
        Log.d(TAG, "profile_sync user=<redacted> phone=<redacted>")
        return "profile synced with redacted logging"
    }

    fun analyticsTrack(
        context: Context,
        secure: Boolean,
    ): String {
        if (!secure) {
            AnalyticsStub.track(
                context,
                "profile_update",
                "email=hany.ibrahim@siege.app recovery=" + FLAG_L2 + " location=Giza,Egypt",
            )
            AnalyticsStub.track(context, "screen_view", "screen=vault_settings")
        } else {
            AnalyticsStub.track(context, "profile_update", "fields=<minimized, encrypted>")
        }
        return "2 events tracked. Flush to see them."
    }

    fun sdkBreadcrumb(
        context: Context,
        secure: Boolean,
    ): String {
        if (!secure) {
            AnalyticsStub.track(
                context,
                "sdk_breadcrumb",
                "sdk=SiegeAdNet v2.1 pii=hany.ibrahim@siege.app recovery=" + FLAG_L3,
            )
        } else {
            AnalyticsStub.track(context, "sdk_breadcrumb", "fields=<redacted by allowlist>")
        }
        return "SDK breadcrumb recorded. Flush to inspect."
    }

    fun flush(context: Context): String = AnalyticsStub.flush(context)

    /** L4 — reversible pseudonym: md5(name) with a 6-char prefix embedded in the flag. */
    fun pseudonymizedProfile(secure: Boolean): String {
        val name = "hany.ibrahim"
        val pseudo = Pseudonymizer.pseudonymize(name)
        return if (!secure) {
            "pseudonym: $pseudo\n(the id is md5 of the username — brute the name to reverse)\n" +
                "reverse it and the profile yields:\n$FLAG_L4"
        } else {
            "pseudonym: $pseudo\n(hardened: salted+keyed hash with a server-side salt — " +
                "the id is no longer reversible to PII)"
        }
    }
}

class PiiLogsL1Challenge : TieredChallenge(
    category = "privacy",
    slug = "pii-logs",
    level = Difficulty.EASY,
    title = "Sync Log",
    brief = "The profile sync writes a full record to logcat — recovery code included. " +
        "Pull it from the log.",
    owaspRefs = listOf("M6", "MASVS-PRIVACY-1", "MASTG-TEST-0x60"),
    hints = listOf(
        "adb logcat -s SiegeProfile after pressing sync.",
        "The record carries email, phone and the recovery code in one line.",
        "Hardened builds log only redacted fields.",
    ),
    flag = FLAG_L1,
    learn = LearnContent(
        theory = "PII in logs is a privacy breach with a built-in exfil channel: " +
            "logcat is readable by the user, by adb, and is shipped in bug reports.\n\n" +
            "Log redaction must be mechanical, not aspirational.",
        mastgRefs = listOf("MASVS-PRIVACY-1", "MASTG-TEST-0x60"),
        vulnerableSnippet = "Log.d(TAG, \"user=<email> phone=<phone> recovery=<code>\")",
        fixSnippet = "Log.d(TAG, \"profile_sync user=<redacted>\")",
        takeaway = "Every log line is a publication.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { output = PrivacyLab.logPii(context, secureMode) }, modifier = Modifier.fillMaxWidth()) {
                Text("Sync profile")
            }
            Button(onClick = {
                output = context.let {
                    android.util.Log.d(TAG, "(simulated external read)")
                    "read with: adb logcat -d -s SiegeProfile"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Show the logcat command") }
            ChallengeConsole(output)
        }
    }
}

class PiiLogsL2Challenge : TieredChallenge(
    category = "privacy",
    slug = "pii-logs",
    level = Difficulty.MEDIUM,
    title = "Analytics Payload",
    brief = "Profile events go to an analytics endpoint in the clear — PII and the " +
        "recovery code ride together. Track, flush, inspect.",
    owaspRefs = listOf("M6", "MASVS-PRIVACY-1", "MASTG-TEST-0x60"),
    hints = listOf(
        "Track two events, then flush — the analytics log file/printout shows the payload.",
        "The payload carries email + recovery code + location in one event.",
        "Secure builds minimize fields and encrypt the batch.",
    ),
    flag = FLAG_L2,
    learn = LearnContent(
        theory = "Analytics is the quiet exfiltration channel: fields nobody audits ride " +
            "in events to third-party endpoints in cleartext.\n\n" +
            "Minimize fields, encrypt batches, and require consent.",
        mastgRefs = listOf("MASVS-PRIVACY-1", "MASTG-TEST-0x60"),
        vulnerableSnippet = "track(\"profile_update\", \"email=<email> recovery=<code>\")",
        fixSnippet = "track(\"profile_update\", encrypted(minimizedFields))",
        takeaway = "Analytics is outbound PII — audit it like an API.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { output = PrivacyLab.analyticsTrack(context, secureMode) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Track profile events")
            }
            Button(onClick = { output = PrivacyLab.flush(context) }, modifier = Modifier.fillMaxWidth()) {
                Text("Flush analytics")
            }
            ChallengeConsole(output)
        }
    }
}

class PiiLogsL3Challenge : TieredChallenge(
    category = "privacy",
    slug = "pii-logs",
    level = Difficulty.HARD,
    title = "SDK Breadcrumb",
    brief = "A third-party ad SDK records its own breadcrumbs — PII and the recovery " +
        "code end up in its payload on every screen.",
    owaspRefs = listOf("M6", "MASVS-PRIVACY-1", "M2", "MASTG-TEST-0x60"),
    hints = listOf(
        "The SDK tracks an sdk_breadcrumb event with pii and recovery fields.",
        "Record the breadcrumb, flush, and read the SDK log.",
        "Secure builds run a redaction allowlist before anything reaches the SDK.",
    ),
    flag = FLAG_L3,
    learn = LearnContent(
        theory = "Third-party SDKs silently amass PII through breadcrumbs and events. " +
            "Whatever flows into the SDK leaves your control — its log file and network " +
            "payloads are part of your privacy surface.\n\n" +
            "Allowlist what may reach an SDK, and redact before the call.",
        mastgRefs = listOf("MASVS-PRIVACY-1", "MASVS-RESILIENCE-3", "MASTG-TEST-0x60"),
        vulnerableSnippet = "sdk.track(\"breadcrumb\", \"pii=<email> recovery=<code>\")",
        fixSnippet = "sdk.track(\"breadcrumb\", redact(mapOf(\"pii\" to null)))",
        takeaway = "Your SDK is your data breach liability.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { output = PrivacyLab.sdkBreadcrumb(context, secureMode) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Record SDK breadcrumb")
            }
            Button(onClick = { output = PrivacyLab.flush(context) }, modifier = Modifier.fillMaxWidth()) {
                Text("Flush analytics")
            }
            ChallengeConsole(output)
        }
    }
}

class PiiLogsL4Challenge : TieredChallenge(
    category = "privacy",
    slug = "pii-logs",
    level = Difficulty.INSANE,
    title = "Reversible Pseudonym",
    brief = "The profile is 'anonymized' with a pseudonym derived from the username. " +
        "Reverse the id back to the identity and the profile opens.",
    owaspRefs = listOf("M6", "MASVS-PRIVACY-1", "MASTG-TEST-0x60"),
    hints = listOf(
        "The pseudonym is an unsalted MD5 of the username.",
        "The username is hany.ibrahim — md5 it and compare to the pseudonym on screen.",
        "Hardened builds use a keyed, salted hash with a server-side salt.",
    ),
    flag = FLAG_L4,
    learn = LearnContent(
        theory = "Pseudonymization is only as strong as the difficulty of reversing " +
            "the id. An unsalted MD5 of a guessable username is a lookup table away " +
            "from full identity recovery.\n\n" +
            "Real anonymization uses keyed, salted hashes (or random ids) with the " +
            "mapping held server-side.",
        mastgRefs = listOf("MASVS-PRIVACY-1", "MASTG-TEST-0x60"),
        vulnerableSnippet = "val pseudo = md5(username) // reversible by dictionary",
        fixSnippet = "val pseudo = hmac(secretServerSalt, username) // or random id",
        takeaway = "A reversible pseudonym is a pseudonym in name only.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { output = PrivacyLab.pseudonymizedProfile(secureMode) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Load anonymized profile")
            }
            ChallengeConsole(output)
        }
    }
}
