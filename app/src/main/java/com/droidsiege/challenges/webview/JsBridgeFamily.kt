package com.droidsiege.challenges.webview

import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import java.io.File

/**
 * Shared host for the jsbridge screens: arms the WebView, the bridge and the result
 * console. The page content is attacker-influenced by design.
 */
@Composable
internal fun BridgeHost(
    secure: Boolean,
    initialJs: String,
    requireToken: Boolean = false,
    withFileReader: Boolean = false,
) {
    val context = LocalContext.current
    var result by remember { mutableStateOf("") }
    var js by rememberSaveable { mutableStateOf(initialJs) }
    val webView = remember { mutableStateOf<WebView?>(null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        ChallengeWebView(
            pageHtml = WebViewPages.offersShell(
                if (requireToken) "<input id=\"token\" value=\"premium-unlock-777\">" else "",
            ),
            bridge = BridgeSurface(
                secure = secure,
                bridgeSecret = WebViewFlags.BRIDGE_L1,
                premiumSecret = WebViewFlags.BRIDGE_L2,
                onResult = { result = it },
                privateFileReader = if (withFileReader) {
                    { name ->
                        val f = File(context.filesDir, name)
                        if (f.exists()) f.readText() else "not found"
                    }
                } else {
                    null
                },
            ),
            onWebViewReady = { webView.value = it },
        )
        if (requireToken) {
            Text(
                text = "the premium token lives in the page DOM: input#token",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = js,
            onValueChange = { js = it },
            label = { Text("injected JavaScript") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { webView.value?.evaluateJavascript(js, null) }, modifier = Modifier.fillMaxWidth()) {
            Text("Run in WebView")
        }
        Text(
            text = "bridge result: $result",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

internal fun plantEscrowFile(
    context: Context,
    secure: Boolean,
): File {
    val f = File(context.filesDir, "escrow_code.txt")
    if (!secure && !f.exists()) f.writeText(WebViewFlags.BRIDGE_L4)
    return f
}

class JsBridgeL1Challenge : TieredChallenge(
    category = "webview",
    slug = "jsbridge",
    level = Difficulty.EASY,
    title = "Open Bridge",
    brief = "The offers WebView exposes a native bridge. The page content is attacker-" +
        "influenced — inject JS that walks the bridge and reports the code.",
    owaspRefs = listOf("M4", "MASVS-PLATFORM-2", "MASTG-TEST-0x59"),
    hints = listOf(
        "The bridge object is SiegeBridge; reportResult(result) feeds the console.",
        "Injected JS: SiegeBridge.reportResult(SiegeBridge.getRecoveryCode())",
        "Secure builds mount no flag-bearing bridge at all.",
    ),
    flag = WebViewFlags.BRIDGE_L1,
    learn = LearnContent(
        theory = "addJavascriptInterface hands a native object to every page the " +
            "WebView renders. If the page content can be influenced (offers HTML, " +
            "redirects, a MITM), the injected script owns the bridge's methods.\n\n" +
            "Bridges must expose the minimum surface, only over trusted content, and " +
            "must never return secrets to the page.",
        mastgRefs = listOf("MASVS-PLATFORM-2", "MASTG-TEST-0x59"),
        vulnerableSnippet = "webView.addJavascriptInterface(BridgeSurface(), \"SiegeBridge\")\n" +
            "// BridgeSurface.getRecoveryCode() returns the flag to any page",
        fixSnippet = "// no bridge over attacker-influenced content:\n" +
            "if (page.isTrusted) addJavascriptInterface(minimalBridge, \"SiegeBridge\")",
        takeaway = "A JS bridge is a native API with a web-side door.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        BridgeHost(
            secure = secureMode,
            initialJs = "SiegeBridge.reportResult(SiegeBridge.getRecoveryCode())",
        )
    }
}

class JsBridgeL2Challenge : TieredChallenge(
    category = "webview",
    slug = "jsbridge",
    level = Difficulty.MEDIUM,
    title = "Token Gate",
    brief = "The premium bridge method demands a token. The check runs on the JS side " +
        "of the same page you are injecting into.",
    owaspRefs = listOf("M4", "MASVS-PLATFORM-2", "MASTG-TEST-0x59"),
    hints = listOf(
        "The token sits in the page DOM: input#token value=premium-unlock-777.",
        "Two-line attack: read the DOM value, pass it to getPremiumCode(token).",
        "Secure builds gate premium data server-side, not by a page-readable token.",
    ),
    flag = WebViewFlags.BRIDGE_L2,
    learn = LearnContent(
        theory = "A gate implemented in the page's own JS is readable and replayable " +
            "by injected JS: read the token from the DOM, hand it to the bridge, done. " +
            "Checks that run in the attacker's context are not checks.\n\n" +
            "Entitlement must be decided where the attacker cannot read or write — " +
            "the server (or native code bound to hardware).",
        mastgRefs = listOf("MASVS-PLATFORM-2", "MASTG-TEST-0x59"),
        vulnerableSnippet = "// page JS decides:\n" +
            "if (token === 'premium-unlock-777') return premiumCode",
        fixSnippet = "// server validates the entitlement before the bridge answers",
        takeaway = "A check an attacker can read is a check an attacker can pass.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        BridgeHost(
            secure = secureMode,
            requireToken = true,
            initialJs = "var t = document.getElementById('token').value; " +
                "SiegeBridge.reportResult(SiegeBridge.getPremiumCode(t))",
        )
    }
}

class JsBridgeL3Challenge : TieredChallenge(
    category = "webview",
    slug = "jsbridge",
    level = Difficulty.HARD,
    title = "Chained Bridge",
    brief = "This bridge lives behind the offers deep link — the Phase 3 entry point. " +
        "Chain the link to the bridge to reach the code.",
    owaspRefs = listOf("M4", "MASVS-PLATFORM-2", "MASTG-TEST-0x59"),
    hints = listOf(
        "Fire the offers deep link with url=…/bridge to land on the bridge page.",
        "The page then runs the same injected call — the bridge is mounted for " +
            "deep-link-sourced loads.",
        "Hardened builds mount no bridge for untrusted-origin loads.",
    ),
    flag = WebViewFlags.BRIDGE_L3,
    learn = LearnContent(
        theory = "Bridges become far more dangerous when chained: a deep link delivers " +
            "the attacker's page, the page calls the bridge, the bridge answers with " +
            "native data. Each link is a Phase 3 finding; together they are an " +
            "unauthenticated native read.\n\n" +
            "Defense in depth means every link refuses: verify the load, drop the " +
            "bridge for untrusted origins.",
        mastgRefs = listOf("MASVS-PLATFORM-2", "MASTG-TEST-0x59"),
        vulnerableSnippet = "offers://load?url=https://…/bridge  // phase-3 entry\n" +
            "// page JS: SiegeBridge.reportResult(SiegeBridge.getRecoveryCode())",
        fixSnippet = "// deep-link loads get no bridge:\n" +
            "if (load.source == DEEPLINK) disableBridge()",
        takeaway = "Chains turn small flaws into native reads — break one link.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        BridgeHost(
            secure = secureMode,
            initialJs = "SiegeBridge.reportResult(SiegeBridge.getRecoveryCode())",
        )
    }
}

class JsBridgeL4Challenge : TieredChallenge(
    category = "webview",
    slug = "jsbridge",
    level = Difficulty.INSANE,
    title = "Privileged Bridge",
    brief = "This bridge's method reads an app-private file by name and returns its " +
        "content to the page. Chain JS → bridge → file read.",
    owaspRefs = listOf("M4", "MASVS-PLATFORM-2", "M9", "MASTG-TEST-0x59"),
    hints = listOf(
        "readPrivateFile(name) resolves inside filesDir — the escrow file is escrow_code.txt.",
        "JS: SiegeBridge.reportResult(SiegeBridge.readPrivateFile('escrow_code.txt'))",
        "Secure builds expose no file-reading bridge surface at all.",
    ),
    flag = WebViewFlags.BRIDGE_L4,
    learn = LearnContent(
        theory = "The terminal bridge sin: a method that performs a privileged native " +
            "action — here a file read inside the app sandbox — and hands the result " +
            "to whatever page asks. Chained with attacker content it is a full " +
            "sandbox read primitive.\n\n" +
            "Bridges must never expose privileged operations; file and crypto work " +
            "stays behind a server-validated request.",
        mastgRefs = listOf("MASVS-PLATFORM-2", "MASVS-STORAGE-1", "MASTG-TEST-0x59"),
        vulnerableSnippet = "@JavascriptInterface\nfun readPrivateFile(name: String) =\n" +
            "    File(filesDir, name).readText() // returned to the page",
        fixSnippet = "// no privileged surface:\n" +
            "// file access happens behind a server-validated request only",
        takeaway = "A bridge that reads your sandbox reads it for the attacker too.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        androidx.compose.runtime.LaunchedEffect(secureMode) {
            plantEscrowFile(context, secureMode)
        }
        BridgeHost(
            secure = secureMode,
            withFileReader = true,
            initialJs = "SiegeBridge.reportResult(SiegeBridge.readPrivateFile('escrow_code.txt'))",
        )
    }
}
