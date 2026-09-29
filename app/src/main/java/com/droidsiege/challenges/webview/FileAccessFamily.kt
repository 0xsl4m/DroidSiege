package com.droidsiege.challenges.webview

import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
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
import java.io.File

private const val SECRET_FILE_L1 = "wv_secret.txt"
private const val SECRET_FILE_L3 = "secret_flag.txt"
private const val SECRET_FILE_L4 = "wv_escrow.txt"

private fun plantWebViewSecret(
    context: Context,
    fileName: String = SECRET_FILE_L1,
    content: String = WebViewFlags.FILE_L1,
): File {
    val f = File(context.filesDir, fileName)
    if (!f.exists()) f.writeText(content)
    return f
}

class FileAccessL1Challenge : TieredChallenge(
    category = "webview",
    slug = "fileaccess",
    level = Difficulty.EASY,
    title = "Universal File Access",
    brief = "This reader WebView allows file access with universal reads. Plant the " +
        "secret, load the file:// payload, read the app-private file from the page.",
    owaspRefs = listOf("M4", "MASVS-PLATFORM-2", "M9", "MASTG-TEST-0x58"),
    hints = listOf(
        "Plant the escrow file first, then press the payload button.",
        "The page runs XHR against file:///data/data/com.droidsiege/files/wv_secret.txt — " +
            "universal access makes it same-origin-trusted.",
        "Secure builds set both file-access flags false: the XHR fails.",
    ),
    flag = WebViewFlags.FILE_L1,
    learn = LearnContent(
        theory = "allowFileAccess plus allowUniversalAccessFromFileURLs lets a file:// " +
            "page read OTHER file:// resources, including the app's private files. One " +
            "attacker-supplied URL is a sandbox read primitive.\n\n" +
            "Both flags must be false in any WebView that renders untrusted input.",
        mastgRefs = listOf("MASVS-PLATFORM-2", "MASTG-TEST-0x58"),
        vulnerableSnippet = "settings.allowFileAccess = true\n" +
            "settings.allowUniversalAccessFromFileURLs = true\n" +
            "webView.loadUrl(attackerUrl)",
        fixSnippet = "settings.allowFileAccess = false\n" +
            "settings.allowUniversalAccessFromFileURLs = false",
        takeaway = "File flags on a WebView are sandbox keys.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var output by remember { mutableStateOf("") }
        var jsResult by remember { mutableStateOf("") }

        fun fetchEscrowViaFileXhr() {
            plantWebViewSecret(context)
            val webView = WebView(context)
            if (!secureMode) {
                webView.settings.allowFileAccess = true
                webView.settings.allowUniversalAccessFromFileURLs = true
            }
            webView.webViewClient =
                object : android.webkit.WebViewClient() {
                    override fun onPageFinished(
                        view: WebView?,
                        url: String?,
                    ) {
                        view?.evaluateJavascript(
                            "try { " +
                                "var x = new XMLHttpRequest(); " +
                                "x.open('GET', 'file:///data/data/com.droidsiege/files/$SECRET_FILE_L1', false); " +
                                "x.send(null); " +
                                "x.responseText; " +
                                "} catch (e) { 'FILE ACCESS ERROR: ' + e.message }",
                        ) { result -> jsResult = result.replace("\"", "") }
                    }
                }
            webView.loadUrl("file:///android_asset/droidsiege_secret.html")
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    plantWebViewSecret(context)
                    output = "escrow planted: " + File(context.filesDir, SECRET_FILE_L1).absolutePath
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Plant escrow file") }
            Button(
                onClick = { fetchEscrowViaFileXhr() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Load file:// payload page") }
            ChallengeConsole(output)
            ChallengeConsole(jsResult)
        }
    }
}

class FileAccessL2Challenge : TieredChallenge(
    category = "webview",
    slug = "fileaccess",
    level = Difficulty.MEDIUM,
    title = "Unvalidated Load",
    brief = "The reader takes its URL from the caller with no validation. Feed it a " +
        "file:// URL pointing at the escrow file.",
    owaspRefs = listOf("M4", "MASVS-PLATFORM-2", "MASTG-TEST-0x58"),
    hints = listOf(
        "file:///data/data/com.droidsiege/files/wv_secret.txt — allowFileAccess is on.",
        "Secure builds allowlist the https scheme only.",
    ),
    flag = WebViewFlags.FILE_L2,
    learn = LearnContent(
        theory = "loadUrl without scheme validation converts any caller into a local " +
            "file reader. Combined with allowFileAccess it reaches the app sandbox.\n\n" +
            "Validate scheme and host against an allowlist before loading.",
        mastgRefs = listOf("MASVS-PLATFORM-2", "MASTG-TEST-0x58"),
        vulnerableSnippet = "webView.loadUrl(intent.getStringExtra(\"url\"))",
        fixSnippet = "if (!url.startsWith(\"https://trusted.siege.local/\")) refuse()",
        takeaway = "Unvalidated URLs turn WebViews into file readers.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        plantWebViewSecret(context)
        var url by rememberSaveable { mutableStateOf("https://offers.siegeapp.dev/") }
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("reader URL") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    if (secureMode) {
                        output = if (!url.startsWith("https://offers.siegeapp.dev/")) {
                            "REFUSED: url not on the allowlist"
                        } else {
                            "loaded trusted page"
                        }
                    } else {
                        val webView = WebView(context)
                        webView.settings.allowFileAccess = true
                        webView.webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(
                                view: WebView?,
                                loadedUrl: String?,
                            ) {
                                output = "loaded: $loadedUrl"
                            }
                        }
                        webView.loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Load URL") }
            ChallengeConsole(output)
        }
    }
}

class FileAccessL3Challenge : TieredChallenge(
    category = "webview",
    slug = "fileaccess",
    level = Difficulty.HARD,
    title = "Provider Traversal Read",
    brief = "The Phase 3 vault provider serves files with a traversable path. The " +
        "WebView renders any content: chain the provider into the reader.",
    owaspRefs = listOf("M4", "MASVS-PLATFORM-2", "M8", "MASTG-TEST-0x58"),
    hints = listOf(
        "content://com.droidsiege.vault/files/notes/../../secret_flag.txt (plant first).",
        "The WebView loads the content:// URL; the provider resolves the traversal.",
        "Hardened: the provider canonicalizes and the read is refused.",
    ),
    flag = WebViewFlags.FILE_L3,
    learn = LearnContent(
        theory = "A WebView that renders content:// URIs inherits every flaw the " +
            "provider has — here, the Phase 3 path traversal. One URL reads an " +
            "app-private file without any native code in the attacker's hands.\n\n" +
            "Chains mean the provider must be fixed even if 'the WebView is trusted'.",
        mastgRefs = listOf("MASVS-PLATFORM-2", "MASTG-TEST-0x61"),
        vulnerableSnippet = "webView.loadUrl(\"content://com.droidsiege.vault/files/notes/../../secret_flag.txt\")",
        fixSnippet = "// provider canonicalizes; the traversal read fails",
        takeaway = "The WebView renders what the provider leaks — chains don't forgive.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        // the L4 plant from the provider family writes filesDir/secret_flag.txt
        androidx.compose.runtime.LaunchedEffect(Unit) {
            com.droidsiege.challenges.components.ProviderFiles.plantSecret(context)
        }
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val webView = WebView(context)
                    webView.webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(
                            view: WebView?,
                            url: String?,
                        ) {
                            output = "loaded: $url"
                        }
                    }
                    webView.loadUrl(
                        "content://com.droidsiege.vault/files/notes/../../$SECRET_FILE_L3",
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Load provider traversal URL") }
            ChallengeConsole(output)
        }
    }
}

class FileAccessL4Challenge : TieredChallenge(
    category = "webview",
    slug = "fileaccess",
    level = Difficulty.INSANE,
    title = "Full Chain",
    brief = "file:// page, universal access and a mounted bridge: the reader can " +
        "read the secret file and exfiltrate it through the bridge in one payload.",
    owaspRefs = listOf("M4", "MASVS-PLATFORM-2", "M9", "MASTG-TEST-0x58"),
    hints = listOf(
        "The file:// page runs XHR (universal access) then calls SiegeBridge.reportResult.",
        "Payload: read file:///data/data/com.droidsiege/files/wv_escrow.txt via XHR and report it.",
        "Secure builds: flags off and no bridge — both surfaces closed.",
    ),
    flag = WebViewFlags.FILE_L4,
    learn = LearnContent(
        theory = "The full chain: file flags grant the read, universal access grants " +
            "the origin, the bridge grants the exfil. Each surface alone is a finding; " +
            "together they are a one-payload sandbox read-and-send.\n\n" +
            "Close both surfaces — file flags off, no bridge on untrusted loads.",
        mastgRefs = listOf("MASVS-PLATFORM-2", "MASVS-STORAGE-1", "MASTG-TEST-0x58"),
        vulnerableSnippet = "// file:// page:\n" +
            "xhr(file:///…/wv_secret.txt) → SiegeBridge.reportResult(response)",
        fixSnippet = "// flags off + no bridge:\n" +
            "allowFileAccess = false; no addJavascriptInterface",
        takeaway = "Chained WebView surfaces are a full read-exfil primitive.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        plantWebViewSecret(context, SECRET_FILE_L4, WebViewFlags.FILE_L4)
        var result by remember { mutableStateOf("") }
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            ChallengeWebView(
                pageHtml = WebViewPages.offersShell(""),
                bridge = if (!secureMode) {
                    BridgeSurface(
                        secure = false,
                        bridgeSecret = "",
                        premiumSecret = "",
                        onResult = { result = it },
                    )
                } else {
                    null
                },
                onWebViewReady = { webView ->
                    if (!secureMode) {
                        webView.settings.allowFileAccess = true
                        webView.settings.allowUniversalAccessFromFileURLs = true
                        webView.webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(
                                view: WebView?,
                                url: String?,
                            ) {
                                view?.evaluateJavascript(
                                    "var x = new XMLHttpRequest(); " +
                                        "x.open('GET', " +
                                        "'file:///data/data/com.droidsiege/files/wv_secret.txt', false); " +
                                        "x.send(null); " +
                                        "SiegeBridge.reportResult(x.responseText); ''",
                                    null,
                                )
                            }
                        }
                        webView.loadUrl("file:///android_asset/droidsiege_secret.html")
                    }
                },
            )
            ChallengeConsole(result)
        }
    }
}
