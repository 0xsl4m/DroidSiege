package com.droidsiege.challenges.network

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import androidx.compose.runtime.Composable
import com.droidsiege.challenges.common.ActionChallengeScreen
import com.droidsiege.challenges.common.KitAction
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

private const val SESSION_TOKEN = "DS{network_urlleak_L1_52e9af}"

/** L4 — the analytics stub that persists whatever flows through it. */
object AnalyticsStub {
    fun record(
        context: Context,
        event: String,
        payload: String,
    ) {
        File(context.filesDir, "urlleak_analytics.log")
            .appendText("${System.currentTimeMillis()} $event $payload\n")
    }

    fun logPath(context: Context): String = File(context.filesDir, "urlleak_analytics.log").absolutePath
}

object UrlLeakVault {
    private fun plainClient() = MockBackend.plainClient()

    fun tokenInQuery(secure: Boolean): String {
        MockBackend.ensureStarted()
        return if (!secure) {
            // the token rides the query string, where proxies and server logs live
            exchange(
                plainClient(),
                "http://127.0.0.1:${MockBackend.plainPort}/urlleak?token=$SESSION_TOKEN",
            )
        } else {
            val body = "{\"token\":\"$SESSION_TOKEN\"}".toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://trusted.siege.local:${MockBackend.tlsPort}/urlleak")
                .post(body)
                .build()
            plainClient().newCall(request).execute().use { response ->
                "POST https://…/urlleak (token in body, over TLS)\n<-- ${response.code}\n" +
                    "(the URL itself carries no secret)"
            }
        }
    }

    fun refererLeak(secure: Boolean): String {
        MockBackend.ensureStarted()
        val fragment = "#token=DS{network_urlleak_L2_d7b310}"
        val request =
            Request
                .Builder()
                .url("https://trusted.siege.local:${MockBackend.tlsPort}/urlleak/pixel")
                .header(
                    "Referer",
                    if (!secure) "https://app.siege.local/vault$fragment" else "https://app.siege.local/vault",
                )
                .build()
        return plainClient().newCall(request).execute().use { response ->
            val referer = response.request.header("Referer") ?: "(none)"
            "GET pixel\n<-- ${response.code}\nReferer seen server-side: $referer"
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun webViewHistoryDump(
        webView: WebView,
        secure: Boolean,
    ): String {
        val url =
            if (!secure) {
                "https://app.siege.local/vault?token=DS{network_urlleak_L3_84c26e}"
            } else {
                "https://app.siege.local/vault"
            }
        webView.loadUrl(url)
        return "loaded: $url (the history dump action reads it back)"
    }

    fun historyDump(webView: WebView): String {
        val list = webView.copyBackForwardList()
        val entries = (0 until list.size).map { list.getItemAtIndex(it).url }
        return if (entries.isEmpty()) "(history empty)" else entries.joinToString("\n")
    }

    fun analyticsDeepLink(
        context: Context,
        secure: Boolean,
    ): String {
        val link =
            if (!secure) {
                "siege://vault/open?code=DS{network_urlleak_L4_1a5f97}"
            } else {
                "siege://vault/open?code=[redacted]"
            }
        AnalyticsStub.record(context, "deeplink_open", link)
        return "Analytics event recorded to ${AnalyticsStub.logPath(context)}"
    }
}

class UrlLeakL1Challenge : TieredChallenge(
    category = "network",
    slug = "urlleak",
    level = Difficulty.EASY,
    title = "Token in Query",
    brief = "The session bootstrap sends its token as a query parameter. The server " +
        "logs every request it receives.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-1", "M9", "MASTG-TEST-0x56"),
    hints = listOf(
        "Fetch the bootstrap, then read the server's request log — the console prints it.",
        "The token is in the URL: ?token=DS{…} — exactly where proxies and access logs live.",
        "Secure mode moves the token into the POST body over TLS.",
    ),
    flag = SESSION_TOKEN,
    learn = LearnContent(
        theory = "URLs are the most-logged part of any request: access logs, proxies, " +
            "SSO referrers, browser history. A secret in the query string is recorded " +
            "everywhere on the path and retained far longer than the session lasts.\n\n" +
            "Secrets belong in request bodies over TLS — and logging must redact them.",
        mastgRefs = listOf("MASVS-NETWORK-1", "MASTG-TEST-0x56"),
        vulnerableSnippet = "GET /bootstrap?token=${'$'}sessionToken",
        fixSnippet = "POST /bootstrap {\"token\": \"…\"} // over TLS, never logged as a URL",
        takeaway = "The URL is a logging surface — never put secrets in it.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Bootstrap session") { _, secure -> UrlLeakVault.tokenInQuery(secure) },
                KitAction("Read the server request log") { _, _ ->
                    MockBackend.requestLog.joinToString("\n").ifEmpty { "(log empty — bootstrap first)" }
                },
            ),
        )
    }
}

class UrlLeakL2Challenge : TieredChallenge(
    category = "network",
    slug = "urlleak",
    level = Difficulty.MEDIUM,
    title = "Referer Trail",
    brief = "The vault page embeds a tracking pixel. The page URL carries a secret in " +
        "its fragment — and the browser reuses the page URL as the Referer.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-1", "M6", "MASTG-TEST-0x56"),
    hints = listOf(
        "Fragments are not sent to servers — but they ARE included in the Referer the page sends.",
        "Fire the pixel request, then read the server log: the Referer carries the fragment.",
        "Hardened builds strip the fragment from referrers.",
    ),
    flag = "DS{network_urlleak_L2_d7b310}",
    learn = LearnContent(
        theory = "Fragments never reach the server on their own request — but any " +
            "subresource the page loads sends the full page URL, fragment included, as " +
            "the Referer. A secret placed after '#' is exfiltrated by the first image " +
            "tag on the page.\n\n" +
            "Keep secrets out of URLs entirely; fragments are client-visible state, " +
            "not storage.",
        mastgRefs = listOf("MASVS-NETWORK-1", "MASTG-TEST-0x56"),
        vulnerableSnippet = "// page URL: /vault#token=<secret>\n" +
            "<img src=\"/pixel\"> <!-- Referer: /vault#token=<secret> -->",
        fixSnippet = "Referrer-Policy: no-referrer\n// + never place secrets in fragments",
        takeaway = "The fragment rides along in the Referer — it is not private.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Fire the tracking pixel") { _, secure -> UrlLeakVault.refererLeak(secure) },
                KitAction("Read the server request log") { _, _ ->
                    MockBackend.requestLog.joinToString("\n").ifEmpty { "(log empty — fire first)" }
                },
            ),
        )
    }
}

class UrlLeakL3Challenge : TieredChallenge(
    category = "network",
    slug = "urlleak",
    level = Difficulty.HARD,
    title = "WebView History",
    brief = "The in-app vault browser navigates with the session token in the URL. " +
        "WebView keeps a back-forward list — read it.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-1", "M9", "MASTG-TEST-0x56"),
    hints = listOf(
        "Load the vault page first (the token is in the URL).",
        "Then press the history-dump action: WebView.copyBackForwardList() returns every entry.",
        "Hardened builds never put the token in a navigable URL.",
    ),
    flag = "DS{network_urlleak_L3_84c26e}",
    learn = LearnContent(
        theory = "WebView retains every navigated URL in its back-forward list, and " +
            "those entries are readable by the embedding app's code paths (and some " +
            "tools). A token embedded in a navigated URL persists in that list after " +
            "the page is gone.\n\n" +
            "Navigable URLs must be secret-free; pass sensitive values out-of-band to " +
            "the page instead.",
        mastgRefs = listOf("MASVS-NETWORK-1", "MASTG-TEST-0x56"),
        vulnerableSnippet = "webView.loadUrl(\"https://…/vault?token=${'$'}token\")",
        fixSnippet = "// token delivered via headers/JS bridge, never the URL",
        takeaway = "Navigated URLs live in history — the list outlives the page.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val webView = android.webkit.WebView(androidx.compose.ui.platform.LocalContext.current)
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Load vault page") { _, secure ->
                    UrlLeakVault.webViewHistoryDump(webView, secure)
                },
                KitAction("Dump WebView history") { _, _ -> UrlLeakVault.historyDump(webView) },
            ),
        )
    }
}

class UrlLeakL4Challenge : TieredChallenge(
    category = "network",
    slug = "urlleak",
    level = Difficulty.INSANE,
    title = "Analytics Trail",
    brief = "Opening the vault from a deep link records an analytics event with the " +
        "full link. The analytics file sits in the app's own storage.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-1", "M6", "MASTG-TEST-0x56"),
    hints = listOf(
        "Open the deep link (the action does it), then pull the analytics log file.",
        "run-as com.droidsiege cat files/urlleak_analytics.log",
        "The secure build records a redacted link.",
    ),
    flag = "DS{network_urlleak_L4_1a5f97}",
    learn = LearnContent(
        theory = "Deep links carrying secrets become analytics events when tracking " +
            "code records the full incoming URL — writing the secret to a file and, in " +
            "production, to a vendor's endpoint. The storage is out of the app's " +
            "control from that moment.\n\n" +
            "Analytics payloads must be allowlisted field-by-field, with URLs redacted.",
        mastgRefs = listOf("MASVS-NETWORK-1", "MASVS-PRIVACY-1", "MASTG-TEST-0x56"),
        vulnerableSnippet = "analytics.record(\"deeplink_open\", intent.data.toString())",
        fixSnippet = "analytics.record(\"deeplink_open\", redact(intent.data))",
        takeaway = "Every URL you log is a secret you published.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Open vault deep link") { ctx, secure -> UrlLeakVault.analyticsDeepLink(ctx, secure) },
                KitAction("Read the analytics log") { ctx, _ ->
                    val f = File(ctx.filesDir, "urlleak_analytics.log")
                    if (f.exists()) f.readText() else "(log empty — open the link first)\n${AnalyticsStub.logPath(ctx)}"
                },
            ),
        )
    }
}
