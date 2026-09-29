package com.droidsiege.challenges.components

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView

/** L1+L2 — siege://recover deep links. */
class RecoverLinkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val secure = secureMode(applicationContext)
        val account = intent.data?.getQueryParameter("account")
        when {
            secure -> renderScreen(
                this,
                "Account recovery",
                "Hardened: deep links must arrive from an authenticated session.",
                secret = null,
                denied = true,
            )
            account == "admin_vault" -> renderScreen(
                this,
                "Account recovery",
                "Recovery code for the $account account:",
                secret = "DS{components_deeplink_L2_48e7b1}",
            )
            else -> renderScreen(
                this,
                "Account recovery",
                "Recovery code for this device:",
                secret = "DS{components_deeplink_L1_9c52fa}",
            )
        }
    }
}

/** L3 — unverified app-link host (no autoVerify in the manifest). */
class OfferLinkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val secure = secureMode(applicationContext)
        val link = intent.data
        val isOurHost = link?.host == "offers.siegeapp.dev"
        when {
            secure -> renderScreen(
                this,
                "Offers link",
                "Hardened: app links must be verified (assetlinks.json) before use.",
                secret = null,
                denied = true,
            )
            isOurHost -> renderScreen(
                this,
                "Offers link",
                "Link accepted (host: ${link!!.host}, verification: UNVERIFIED — " +
                    "any app may claim this host). Redeem code:",
                secret = "DS{components_deeplink_L3_f06d93}",
            )
            else -> renderScreen(this, "Offers link", "Unknown host.", secret = null)
        }
    }
}

/** L4 — deep link whose url parameter is loaded straight into a WebView. */
class WebLinkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val secure = secureMode(applicationContext)
        val url = intent.data?.getQueryParameter("url") ?: "https://offers.siegeapp.dev/"
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        setContentView(webView)
        if (secure) {
            // hardened: allowlist the scheme/host, never load file:// or arbitrary input
            val allowed = url.startsWith("https://offers.siegeapp.dev/")
            webView.loadData(
                if (allowed) {
                    "<h1>offers</h1><p>trusted page</p>"
                } else {
                    "<h1>blocked</h1><p>url not on the allowlist</p>"
                },
                "text/html",
                "utf-8",
            )
        } else {
            // insecure: whatever the deep link carries is loaded, file:// included
            webView.loadUrl(url)
        }
    }
}
