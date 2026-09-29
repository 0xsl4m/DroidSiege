package com.droidsiege.challenges.webview

import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent

private const val XSS_SECRET_L1 = "DS{webview_xss_L1_f7a24e}"
private const val XSS_SECRET_L2 = "DS{webview_xss_L2_3d61b9}"
private const val XSS_SECRET_L3 = "DS{webview_xss_L3_8c05e1}"
private const val XSS_SECRET_L4 = "DS{webview_xss_L4_7192cd}"

class XssL1Challenge : TieredChallenge(
    category = "webview",
    slug = "xss",
    level = Difficulty.EASY,
    title = "Reflected Note",
    brief = "The note editor reflects your note straight into the page DOM. The page " +
        "also carries a hidden secret. Inject script that reads it.",
    owaspRefs = listOf("M4", "MASVS-CODE-2", "MASTG-TEST-0x59"),
    hints = listOf(
        "The note goes into <div id=\"note\"> verbatim — script tags survive.",
        "Injected: <script>SiegeBridge.reportResult(document.getElementById('secret').textContent)</script>",
        "Secure builds HTML-encode the note before render.",
    ),
    flag = XSS_SECRET_L1,
    learn = LearnContent(
        theory = "Reflected input that reaches the DOM unencoded is XSS: the injected " +
            "script runs with the page's origin and can read anything on it — including " +
            "hidden elements the UI never shows.\n\n" +
            "HTML-encode on render and keep a strict content security policy.",
        mastgRefs = listOf("MASVS-CODE-2", "MASTG-TEST-0x59"),
        vulnerableSnippet = "loadDataWithBaseURL(base, \"<div id='note'>\" + note + \"</div>\", …)",
        fixSnippet = "// encode before embed:\n" +
            "val safe = note.replace(\"<\", \"&lt;\").replace(\">\", \"&gt;\")",
        takeaway = "Hidden in the DOM is hidden from the UI, not from scripts.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var note by rememberSaveable { mutableStateOf("hello") }
        var result by remember { mutableStateOf("") }
        val webView = remember { mutableStateOf<WebView?>(null) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("note") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val rendered = if (secureMode) {
                        note.replace("<", "&lt;").replace(">", "&gt;")
                    } else {
                        note
                    }
                    webView.value?.let { view ->
                        if (!secureMode) {
                            // the bridge the injected reader reports through
                            view.addJavascriptInterface(
                                com.droidsiege.challenges.webview.BridgeSurface(
                                    secure = false,
                                    bridgeSecret = "",
                                    premiumSecret = "",
                                    onResult = { result = it },
                                ),
                                "SiegeBridge",
                            )
                        }
                        view.loadDataWithBaseURL(
                            "https://offers.siegeapp.dev/",
                            WebViewPages.notePage(rendered, XSS_SECRET_L1),
                            "text/html",
                            "utf-8",
                            null,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Render note") }
            Button(
                onClick = {
                    webView.value?.evaluateJavascript(
                        "SiegeBridge.reportResult(document.getElementById('secret').textContent); ''",
                        null,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Run injected reader (simulates your payload)") }
            ChallengeConsole(result)
        }
    }
}

class XssL2Challenge : TieredChallenge(
    category = "webview",
    slug = "xss",
    level = Difficulty.MEDIUM,
    title = "Stored Note",
    brief = "Notes are stored and rendered on every open, unsanitized. Store the " +
        "payload once, trigger it on every read.",
    owaspRefs = listOf("M4", "MASVS-CODE-2", "MASTG-TEST-0x59"),
    hints = listOf(
        "Store <img src=x onerror=\"SiegeBridge.reportResult(document.getElementById('secret').textContent)\">",
        "Then trigger: render the stored note — the onerror fires.",
        "Secure builds sanitize on render and on store.",
    ),
    flag = XSS_SECRET_L2,
    learn = LearnContent(
        theory = "Stored XSS persists: one unsanitized write poisons every future " +
            "render, for every user of the same storage. The payload fires without the " +
            "attacker present.\n\n" +
            "Sanitize on render (the render is the dangerous act), and validate on " +
            "store as defense in depth.",
        mastgRefs = listOf("MASVS-CODE-2", "MASTG-TEST-0x59"),
        vulnerableSnippet = "store(note); render(note) // both verbatim",
        fixSnippet = "render(sanitize(note))",
        takeaway = "Stored payloads fire forever — sanitize where it renders.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        val prefs = context.getSharedPreferences("siege_xss_prefs", Context.MODE_PRIVATE)
        var stored by rememberSaveable { mutableStateOf(prefs.getString("note", "no note") ?: "") }
        var result by remember { mutableStateOf("") }
        val webView = remember { mutableStateOf<WebView?>(null) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = stored,
                onValueChange = {
                    stored = it
                    prefs.edit().putString("note", it).apply()
                },
                label = { Text("stored note") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val rendered =
                        if (secureMode) {
                            stored.replace("<", "&lt;").replace(">", "&gt;")
                        } else {
                            stored
                        }
                    webView.value?.loadDataWithBaseURL(
                        "https://offers.siegeapp.dev/",
                        WebViewPages.notePage(rendered, XSS_SECRET_L2),
                        "text/html",
                        "utf-8",
                        null,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Render stored note") }
            ChallengeConsole(result)
        }
    }
}

class XssL3Challenge : TieredChallenge(
    category = "webview",
    slug = "xss",
    level = Difficulty.HARD,
    title = "DOM Sink",
    brief = "The renderer passes user data straight into evaluateJavascript. Break " +
        "out of the string and take over the JS context.",
    owaspRefs = listOf("M4", "MASVS-CODE-2", "MASTG-TEST-0x59"),
    hints = listOf(
        "The sink is: showNote('<data>') — close the quote and paren, add your payload.",
        "Payload: '); SiegeBridge.reportResult('DS{webview_xss_L3_8c05e1}'); //",
        "Secure builds pass data as a JSON-encoded argument, never by concatenation.",
    ),
    flag = XSS_SECRET_L3,
    learn = LearnContent(
        theory = "evaluateJavascript with concatenated user data is DOM XSS with " +
            "native reach: the payload executes in the page's JS context, from which " +
            "the bridge is one call away. The sink trusts that a string is just a " +
            "string.\n\n" +
            "Encode the argument (JSONObject.quote) or pass it as a parameter — never " +
            "by concatenation.",
        mastgRefs = listOf("MASVS-CODE-2", "MASTG-TEST-0x59"),
        vulnerableSnippet = "webView.evaluateJavascript(\"showNote('<data>')\", null)",
        fixSnippet = "val encoded = JSONObject.quote(data)\n" +
            "webView.evaluateJavascript(\"showNote(<encoded>)\", null)",
        takeaway = "Concatenated JS is attacker-written JS.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var data by rememberSaveable { mutableStateOf("hi") }
        var result by remember { mutableStateOf("") }
        val webView = remember { mutableStateOf<WebView?>(null) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = data,
                onValueChange = { data = it },
                label = { Text("note data") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val view = webView.value ?: return@Button
                    view.addJavascriptInterface(
                        BridgeSurface(
                            secure = secureMode,
                            bridgeSecret = XSS_SECRET_L3,
                            premiumSecret = "",
                            onResult = { result = it },
                        ),
                        "SiegeBridge",
                    )
                    view.loadDataWithBaseURL(
                        "https://offers.siegeapp.dev/",
                        WebViewPages.notePage("", XSS_SECRET_L3) +
                            "<script>function showNote(t){document.getElementById('out').textContent=t}</script>",
                        "text/html",
                        "utf-8",
                        null,
                    )
                    val injected =
                        if (secureMode) {
                            val encoded = org.json.JSONObject.quote(data)
                            "showNote($encoded)"
                        } else {
                            "showNote('$data')"
                        }
                    view.evaluateJavascript(injected, null)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Render note data") }
            Button(
                onClick = {
                    val payload =
                        if (secureMode) {
                            "showNote('[encoded]')"
                        } else {
                            "'); SiegeBridge.reportResult('$XSS_SECRET_L3'); //"
                        }
                    webView.value?.evaluateJavascript("showNote('$payload')", null)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Fire pre-built break-out") }
            ChallengeConsole(result)
        }
    }
}

class XssL4Challenge : TieredChallenge(
    category = "webview",
    slug = "xss",
    level = Difficulty.INSANE,
    title = "XSS to Bridge",
    brief = "The stored-note page carries a bridge. Full chain: XSS in the note, " +
        "bridge call from the payload, native code in the answer.",
    owaspRefs = listOf("M4", "MASVS-PLATFORM-2", "MASTG-TEST-0x59"),
    hints = listOf(
        "Store: <script>SiegeBridge.reportResult(SiegeBridge.getRecoveryCode())</script>",
        "Render — the stored script runs and the console shows the native answer.",
        "Secure builds: sanitized render + no bridge.",
    ),
    flag = XSS_SECRET_L4,
    learn = LearnContent(
        theory = "XSS alone is page-scope; a mounted bridge promotes it to " +
            "native-scope. The stored payload calls the bridge and the answer arrives " +
            "in the console — an unauthenticated native read delivered through a " +
            "note field.\n\n" +
            "Close both surfaces: sanitize the render and never mount a bridge on " +
            "pages that render user data.",
        mastgRefs = listOf("MASVS-CODE-2", "MASVS-PLATFORM-2", "MASTG-TEST-0x59"),
        vulnerableSnippet = "// stored note + mounted bridge:\n" +
            "<script>SiegeBridge.reportResult(SiegeBridge.getRecoveryCode())</script>",
        fixSnippet = "// sanitize + no bridge:\n" +
            "render(sanitize(note)); // no addJavascriptInterface",
        takeaway = "XSS plus a bridge equals a native API without authentication.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var result by remember { mutableStateOf("") }
        val webView = remember { mutableStateOf<WebView?>(null) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "the stored note for this device carries the payload:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    val note =
                        if (secureMode) {
                            "[sanitized]"
                        } else {
                            "<script>SiegeBridge.reportResult(SiegeBridge.getRecoveryCode())</script>"
                        }
                    val bridge =
                        if (!secureMode) {
                            com.droidsiege.challenges.webview.BridgeSurface(
                                secure = false,
                                bridgeSecret = XSS_SECRET_L4,
                                premiumSecret = "",
                                onResult = { result = it },
                            )
                        } else {
                            null
                        }
                    webView.value?.let { view ->
                        bridge?.let { view.addJavascriptInterface(it, "SiegeBridge") }
                        view.loadDataWithBaseURL(
                            "https://offers.siegeapp.dev/",
                            WebViewPages.notePage(note, XSS_SECRET_L4),
                            "text/html",
                            "utf-8",
                            null,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Render stored note") }
            ChallengeConsole(result)
        }
    }
}
