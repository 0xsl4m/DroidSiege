package com.droidsiege.challenges.webview

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** Flags for the webview family tiers. */
object WebViewFlags {
    const val BRIDGE_L1 = "DS{webview_jsbridge_L1_a3f8c1}"
    const val BRIDGE_L2 = "DS{webview_jsbridge_L2_7e29b4}"
    const val BRIDGE_L3 = "DS{webview_jsbridge_L3_d65018}"
    const val BRIDGE_L4 = "DS{webview_jsbridge_L4_2b8c73}"
    const val FILE_L1 = "DS{webview_fileaccess_L1_e94d07}"
    const val FILE_L2 = "DS{webview_fileaccess_L2_51c8a2}"
    const val FILE_L3 = "DS{webview_fileaccess_L3_68b3f5}"
    const val FILE_L4 = "DS{webview_fileaccess_L4_cd2719}"
    const val XSS_L1 = "DS{webview_xss_L1_f7a24e}"
    const val XSS_L2 = "DS{webview_xss_L2_3d61b9}"
    const val XSS_L3 = "DS{webview_xss_L3_8c05e1}"
    const val XSS_L4 = "DS{webview_xss_L4_7192cd}"
}

/** The in-page content served to the challenge WebViews (locally generated). */
object WebViewPages {
    fun offersShell(bodyHtml: String): String =
        """<!DOCTYPE html>
<html><head><meta charset="utf-8"></head>
<body style="background:#0f172a;color:#e2e8f0;font-family:monospace;padding:12px">
<h3>offers.siegeapp.dev</h3>
<div id="out" style="color:#4ade80">bridge idle</div>
$bodyHtml
</body></html>"""

    /** XSS tiers: the stored note is reflected verbatim; the secret sits in the DOM. */
    fun notePage(
        noteHtml: String,
        secret: String,
    ): String =
        """<!DOCTYPE html>
<html><head><meta charset="utf-8"></head>
<body style="background:#0f172a;color:#e2e8f0;font-family:monospace;padding:12px">
<h3>Stored note</h3>
<div id="note">$noteHtml</div>
<p id="secret" style="display:none">$secret</p>
<div id="out" style="color:#4ade80"></div>
</body></html>"""
}

/**
 * The JS surface a page can reach. [onResult] feeds whatever the injected JS reports
 * back into the hosting screen's console.
 */
class BridgeSurface(
    private val secure: Boolean,
    private val bridgeSecret: String,
    private val premiumSecret: String,
    private val onResult: (String) -> Unit,
    /** L4 — privileged native file read, mounted only in the insecure mode. */
    private val privateFileReader: ((String) -> String)? = null,
) {
    @JavascriptInterface
    fun reportResult(result: String) {
        onResult(result)
    }

    /** L1 — flag handed to any page that asks. */
    @JavascriptInterface
    fun getRecoveryCode(): String = if (!secure) bridgeSecret else "DENIED"

    /** L2 — gated by a token that lives in the page's own DOM. */
    @JavascriptInterface
    fun getPremiumCode(token: String): String =
        if (!secure && token == "premium-unlock-777") premiumSecret else "DENIED"

    /** L4 — privileged native action exposed to the page. */
    @JavascriptInterface
    fun readPrivateFile(name: String): String =
        if (!secure) privateFileReader?.invoke(name) ?: "not found" else "DENIED"
}

/** A challenge WebView: JS enabled, optional bridge, loads the given HTML inline. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChallengeWebView(
    pageHtml: String,
    bridge: BridgeSurface?,
    modifier: Modifier = Modifier,
    onWebViewReady: ((WebView) -> Unit)? = null,
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                bridge?.let { addJavascriptInterface(it, "SiegeBridge") }
                onWebViewReady?.invoke(this)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                "https://offers.siegeapp.dev/",
                pageHtml,
                "text/html",
                "utf-8",
                null,
            )
        },
    )
}

/** Small labelled input reused by the interactive tiers. */
@Composable
fun ChallengeInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}
