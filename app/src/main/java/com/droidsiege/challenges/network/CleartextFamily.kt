package com.droidsiege.challenges.network

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.droidsiege.challenges.common.ActionChallengeScreen
import com.droidsiege.challenges.common.BackendProbeButton
import com.droidsiege.challenges.common.KitAction
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.challenges.common.backendGet
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import okhttp3.OkHttpClient
import okhttp3.Request

/** Runs a request and renders the exchange (request line + response body) as text. */
internal fun exchange(
    client: OkHttpClient,
    url: String,
): String {
    val call = client.newCall(Request.Builder().url(url).build())
    call.execute().use { response ->
        val body = response.body?.string() ?: ""
        return "GET $url\n<-- ${response.code}\n$body"
    }
}

internal fun failedExchange(
    url: String,
    reason: String,
): String = "GET $url\n<-- FAILED ($reason)"

class CleartextL1Challenge : TieredChallenge(
    category = "network",
    slug = "cleartext",
    level = Difficulty.EASY,
    title = "Plain Session",
    brief = "The wallet fetches your session recovery over the wire. Watch the wire.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-1", "MASTG-TEST-0x56"),
    hints = listOf(
        "The console prints the full exchange — request line and response body.",
        "On a real setup the same exchange is visible in Burp/mitmproxy: http means plaintext.",
        "Secure mode moves the call to TLS — the wire no longer carries the code.",
    ),
    flag = "DS{network_cleartext_L1_61d4b8}",
    learn = LearnContent(
        theory = "Cleartext HTTP hands the payload to every network hop: the Wi-Fi AP, " +
            "the carrier, any proxy in between. The response body arrives fully " +
            "readable, and nothing in the protocol flags the exposure.\n\n" +
            "TLS everywhere, enforced by the platform (cleartextTrafficPermitted=false) " +
            "rather than by developer discipline.",
        mastgRefs = listOf("MASVS-NETWORK-1", "MASTG-TEST-0x56"),
        vulnerableSnippet = "client.newCall(Request.Builder().url(\"http://…/clear/L1\").build())",
        fixSnippet = "// https only, and the platform refuses cleartext:\n" +
            "<base-config android:cleartextTrafficPermitted=\"false\" />",
        takeaway = "HTTP is a postcard — the flag was written on a postcard.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        androidx.compose.runtime.LaunchedEffect(Unit) { MockBackend.clearLog() }
        androidx.compose.foundation.layout.Column(
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            ActionChallengeScreen(
                secureMode = secureMode,
                note = "Fetches the session recovery from the local endpoint.",
                actions = listOf(
                    KitAction("Fetch recovery (watch the wire)") { _, secure ->
                        MockBackend.ensureStarted()
                        if (!secure) {
                            exchange(
                                MockBackend.plainClient(),
                                "http://127.0.0.1:${MockBackend.plainPort}/clear/L1",
                            )
                        } else {
                            failedExchange(
                                "http://127.0.0.1:${MockBackend.plainPort}/clear/L1",
                                "blocked: cleartextTrafficPermitted=false; call moved to TLS",
                            )
                        }
                    },
                ),
            )
            BackendProbeButton(
                label = "Fetch /health from the real lab backend (plain HTTP)",
                request = { base ->
                    backendGet("$base/health")
                },
            )
        }
    }
}

class CleartextL2Challenge : TieredChallenge(
    category = "network",
    slug = "cleartext",
    level = Difficulty.MEDIUM,
    title = "One Odd Call",
    brief = "Almost everything moved to TLS. One sync endpoint was left behind — " +
        "find it and read what it carries.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-1", "MASTG-TEST-0x56"),
    hints = listOf(
        "The console lists every exchange in the sync batch — check the scheme column.",
        "Two calls are https; one is still http and its body carries a payload.",
        "Secure mode upgrades the straggler to TLS.",
    ),
    flag = "DS{network_cleartext_L2_c95e07}",
    learn = LearnContent(
        theory = "Partial migrations are the classic M5 finding: the audit fixes the " +
            "documented endpoints and a rarely-touched sync call keeps its old URL. The " +
            "one exception carries the same secrets as the rest.\n\n" +
            "Network policy must be all-or-nothing: platform-level cleartext blocking " +
            "turns 'we should fix that' into a crash in development.",
        mastgRefs = listOf("MASVS-NETWORK-1", "MASTG-TEST-0x56"),
        vulnerableSnippet = "// v2 endpoints moved to https — the legacy sync did not:\n" +
            "fetch(\"http://…/plain/sync\")",
        fixSnippet = "fetch(\"https://…/plain/sync\") // and block http globally",
        takeaway = "One http call is all it takes — audit by scheme, not by module.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        androidx.compose.runtime.LaunchedEffect(Unit) { MockBackend.clearLog() }
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Run sync batch") { _, secure ->
                    MockBackend.ensureStarted()
                    val outputs = mutableListOf<String>()
                    listOf("/api/v2/a", "/plain/sync", "/api/v2/c").forEach { path ->
                        if (!secure && path == "/plain/sync") {
                            outputs += exchange(
                                MockBackend.plainClient(),
                                "http://127.0.0.1:${MockBackend.plainPort}$path",
                            )
                        } else {
                            outputs += exchange(
                                MockBackend.trustingClient(),
                                "https://trusted.siege.local:${MockBackend.tlsPort}$path",
                            )
                        }
                    }
                    outputs.joinToString("\n\n")
                },
            ),
        )
    }
}

class CleartextL3Challenge : TieredChallenge(
    category = "network",
    slug = "cleartext",
    level = Difficulty.HARD,
    title = "Policy Exception",
    brief = "A network security policy now governs cleartext. One domain was granted " +
        "an exception. Aim at that domain.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-1", "MASTG-TEST-0x56"),
    hints = listOf(
        "The policy simulator blocks http for every host EXCEPT trusted.siege.local.",
        "Fetch http://trusted.siege.local:<plainPort>/nsa/L3 — the excepted domain answers.",
        "Secure builds carry no exception: every http fetch is refused.",
    ),
    flag = "DS{network_cleartext_L3_2fa836}",
    learn = LearnContent(
        theory = "Network Security Config exceptions read like pragmatism and age into " +
            "incidents: the 'temporary' cleartext domain outlives the migration, and it " +
            "is exactly where nobody looks. An exception re-opens the plaintext door " +
            "for everything on that host.\n\n" +
            "Zero exceptions is the only defensible end state.",
        mastgRefs = listOf("MASVS-NETWORK-1", "MASTG-TEST-0x56"),
        vulnerableSnippet = "<domain-config cleartextTrafficPermitted=\"true\">\n" +
            "  <domain includeSubdomains=\"true\">trusted.siege.local</domain>\n" +
            "</domain-config>",
        fixSnippet = "<base-config cleartextTrafficPermitted=\"false\" />\n" +
            "<!-- no domain-config exceptions -->",
        takeaway = "Every cleartext exception is a permanent plaintext door.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        androidx.compose.runtime.LaunchedEffect(Unit) { MockBackend.clearLog() }
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Fetch from the excepted domain") { _, secure ->
                    MockBackend.ensureStarted()
                    val host = MockBackend.VIRTUAL_HOST
                    val allowedByPolicy = !secure && host == MockBackend.VIRTUAL_HOST
                    if (!allowedByPolicy) {
                        failedExchange("http://$host:${MockBackend.plainPort}/nsa/L3", "blocked by policy")
                    } else {
                        exchange(
                            MockBackend.plainClient(),
                            "http://$host:${MockBackend.plainPort}/nsa/L3",
                        )
                    }
                },
            ),
        )
    }
}

class CleartextL4Challenge : TieredChallenge(
    category = "network",
    slug = "cleartext",
    level = Difficulty.INSANE,
    title = "Downgrade",
    brief = "The client prefers TLS but 'keeps working' when the secure endpoint is " +
        "unavailable. Make it keep working.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-1", "MASTG-TEST-0x56"),
    hints = listOf(
        "Press 'force TLS failure' — the engine then retries over http.",
        "The retry is the vulnerability: the downgraded call carries the payload in cleartext.",
        "Secure builds surface the error instead of downgrading.",
    ),
    flag = "DS{network_cleartext_L4_8b71d4}",
    learn = LearnContent(
        theory = "Fail-open networking inverts the security promise: an attacker who " +
            "can break the TLS path (blocked port, stripped handshake) receives the " +
            "same payload over http, and the downgrade is silent.\n\n" +
            "TLS failures must fail the operation. Availability never outranks " +
            "confidentiality for sensitive data.",
        mastgRefs = listOf("MASVS-NETWORK-1", "MASTG-TEST-0x56"),
        vulnerableSnippet = "try { fetch(httpsUrl) }\n" +
            "catch (io: IOException) { fetch(httpUrl) // 'keep working' }",
        fixSnippet = "try { fetch(httpsUrl) }\n" +
            "catch (io: IOException) { showError(io) // never downgrade",
        takeaway = "A fallback to http is a planned incident.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        androidx.compose.runtime.LaunchedEffect(Unit) { MockBackend.clearLog() }
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Fetch with TLS failure forced") { _, secure ->
                    MockBackend.ensureStarted()
                    val brokenTls = "https://trusted.siege.local:1/dsg/L4"
                    try {
                        exchange(MockBackend.defaultTlsClient(), brokenTls)
                    } catch (io: Exception) {
                        val failure = failedExchange(brokenTls, "TLS failure: ${io.javaClass.simpleName}")
                        if (secure) {
                            failure + "\n\nHardened: no downgrade — the operation failed."
                        } else {
                            failure + "\n\n" + exchange(
                                MockBackend.plainClient(),
                                "http://127.0.0.1:${MockBackend.plainPort}/dsg/L4",
                            )
                        }
                    }
                },
            ),
        )
    }
}
