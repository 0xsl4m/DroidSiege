package com.droidsiege.challenges.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

/** The internal (non-exported) vault screen the redirect tiers reach. */
class FlagVaultActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // the vault holds its own code when opened without an injected extra —
        // exactly what a mutable/blank redirect exposes
        val code = intent.getStringExtra("code") ?: ComponentFlags.REDIRECT_L3
        renderScreen(
            this,
            "Internal vault",
            "You reached an internal-only screen. Vault code:",
            secret = code,
        )
    }
}

/** L1 — exported proxy that blindly starts an attacker-supplied intent. */
class ProxyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val secure = secureMode(applicationContext)
        val redirect = intent.getStringExtra("redirect")
        if (redirect == null) {
            renderScreen(
                this,
                "Deep proxy",
                "Pass the redirect extra (an intent: URI) to forward a request.",
                secret = null,
            )
            return
        }
        if (secure) {
            // hardened: resolve and allowlist the target before forwarding
            val parsed = runCatching { Intent.parseUri(redirect, Intent.URI_INTENT_SCHEME) }.getOrNull()
            val target = parsed?.component?.className
            val allowed = target in setOf(
                "com.droidsiege.ui.modules.ModuleScreenHost",
                "com.droidsiege.MainActivity",
            )
            if (!allowed) {
                renderScreen(
                    this,
                    "Deep proxy",
                    "Forwarding refused: target is not on the proxy allowlist.",
                    secret = null,
                    denied = true,
                )
                return
            }
            startActivity(parsed)
            renderScreen(this, "Deep proxy", "Forwarded to the allowlisted target.", secret = null)
            return
        }
        // insecure: parse and fire, wherever it points
        val parsed = Intent.parseUri(redirect, Intent.URI_INTENT_SCHEME)
        if (parsed.component?.className == FlagVaultActivity::class.java.name) {
            parsed.putExtra("code", ComponentFlags.REDIRECT_L1)
        }
        startActivity(parsed)
        renderScreen(this, "Deep proxy", "Forwarded.", secret = null)
    }
}

/** L2 — the guarded internal screen: shows its code only with the deputy's token. */
class GuardedVaultActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = intent.getStringExtra("session_token")
        if (token == "deputy-session-2026") {
            renderScreen(
                this,
                "Guarded vault",
                "Deputy session accepted. Vault code:",
                secret = ComponentFlags.REDIRECT_L2,
            )
        } else {
            renderScreen(
                this,
                "Guarded vault",
                "A valid session_token from the proxy is required.",
                secret = null,
            )
        }
    }
}

/** L2 — exported deputy that injects its own session token into nested intents. */
class TokenProxyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val secure = secureMode(applicationContext)
        val redirect = intent.getStringExtra("redirect")
        if (redirect == null) {
            renderScreen(
                this,
                "Session proxy",
                "Pass the redirect extra (an intent: URI) to forward with your session.",
                secret = null,
            )
            return
        }
        val parsed = Intent.parseUri(redirect, Intent.URI_INTENT_SCHEME)
        if (secure) {
            renderScreen(
                this,
                "Session proxy",
                "Forwarding refused: nested targets are resolved and verified first.",
                secret = null,
                denied = true,
            )
            return
        }
        // insecure: the deputy stamps its own token onto the nested intent
        parsed.putExtra("session_token", "deputy-session-2026")
        startActivity(parsed)
        renderScreen(this, "Session proxy", "Forwarded with the deputy session.", secret = null)
    }
}

/** L4 — exported confused deputy performing a privileged action for any caller. */
class DeputyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val secure = secureMode(applicationContext)
        val action = intent.getStringExtra("action")
        if (secure && !CallerGuard.isTrustedCaller(this, callingPackage)) {
            renderScreen(this, "Support deputy", "Caller validation failed.", secret = null, denied = true)
            return
        }
        if (action == "verify-entitlement") {
            if (!secure) {
                // privileged action performed on behalf of any caller
                getSharedPreferences("siege_chain_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("entitled", true).commit()
            }
            renderScreen(
                this,
                "Support deputy",
                "Entitlement verified on behalf of the caller. Escrow code:",
                secret = ComponentFlags.REDIRECT_L4,
            )
        } else {
            renderScreen(
                this,
                "Support deputy",
                "Pass action=verify-entitlement to verify a caller's entitlement.",
                secret = null,
            )
        }
    }
}
