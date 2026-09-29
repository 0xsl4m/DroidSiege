package com.droidsiege.challenges.components

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import com.droidsiege.challenges.common.ActionChallengeScreen
import com.droidsiege.challenges.common.KitAction
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent

private fun redirectCommand(component: String) =
    "adb shell am start -n com.droidsiege/.challenges.components.ProxyActivity " +
        "--es redirect 'intent:#Intent;component=com.droidsiege/$component;end'"

class IntentRedirL1Challenge : TieredChallenge(
    category = "components",
    slug = "intentredir",
    level = Difficulty.EASY,
    title = "Blind Forward",
    brief = "The deep proxy forwards requests it receives to wherever they point — " +
        "including places only the app itself should reach.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-1", "MASTG-TEST-0x63"),
    hints = listOf(
        "The proxy parses a redirect extra as an intent: URI and startActivity()es it.",
        "Point the component at FlagVaultActivity — it is internal-only, the proxy is not.",
        redirectCommand("com.droidsiege.challenges.components.FlagVaultActivity"),
    ),
    flag = "DS{components_intentredir_L1_b3d670}",
    learn = LearnContent(
        theory = "Intent redirection turns a trusted component into a launch rail for " +
            "attacker-written intents. The proxy holds the privileges; the attacker " +
            "supplies the destination; internal-only components are reachable because " +
            "the launch originates from inside the app.\n\n" +
            "Forwarding components must resolve the target and allowlist it before " +
            "calling startActivity.",
        mastgRefs = listOf("MASVS-PLATFORM-1", "MASTG-TEST-0x63"),
        vulnerableSnippet = "val nested = Intent.parseUri(redirect, URI_INTENT_SCHEME)\n" +
            "startActivity(nested)",
        fixSnippet = "val resolved = packageManager.resolveActivity(nested, 0)\n" +
            "if (resolved !in allowlist) refuse()",
        takeaway = "Whoever controls the intent controls the privileges.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            note = "The proxy runs. Craft the redirect, then submit the vault code it reveals.",
            actions = listOf(
                KitAction("Show the redirect command") { _, _ ->
                    redirectCommand("com.droidsiege.challenges.components.FlagVaultActivity")
                },
            ),
        )
    }
}

class IntentRedirL2Challenge : TieredChallenge(
    category = "components",
    slug = "intentredir",
    level = Difficulty.MEDIUM,
    title = "Token Courier",
    brief = "The guarded vault needs a session token only the app's proxy holds. " +
        "Conveniently, the proxy stamps its token onto whatever you forward.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-1", "MASTG-TEST-0x63"),
    hints = listOf(
        "Same extra, different target: the redirect must point at GuardedVaultActivity.",
        "The deputy adds session_token=deputy-session-2026 on the way through.",
        redirectCommand("com.droidsiege.challenges.components.GuardedVaultActivity")
            .replace("ProxyActivity", "TokenProxyActivity"),
    ),
    flag = "DS{components_intentredir_L2_25f8c4}",
    learn = LearnContent(
        theory = "A component that decorates nested intents with its own credentials is a " +
            "confused deputy in the making: the attacker chooses the destination and the " +
            "deputy supplies the authority. The guard on the destination checks the " +
            "token — which arrived, legitimately, from the deputy.\n\n" +
            "Nested intents must be resolved and verified before decoration, and " +
            "privileges must never ride on forwardable extras.",
        mastgRefs = listOf("MASVS-PLATFORM-1", "MASTG-TEST-0x63"),
        vulnerableSnippet = "val nested = Intent.parseUri(redirect, URI_INTENT_SCHEME)\n" +
            "nested.putExtra(\"session_token\", deputyToken)\n" +
            "startActivity(nested)",
        fixSnippet = "// verify the nested component before attaching credentials:\n" +
            "if (resolve(nested) !in allowlist) refuse()",
        takeaway = "Never stamp credentials onto intents you did not build.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Show the redirect command") { _, _ ->
                    redirectCommand("com.droidsiege.challenges.components.GuardedVaultActivity")
                        .replace("ProxyActivity", "TokenProxyActivity")
                },
            ),
        )
    }
}

/**
 * The L3 engine: issues the offer-notification PendingIntent (mutable + blank base in
 * the insecure mode) and plays the attacker who received it.
 */
object PendingIntentVault {
    private const val REQUEST_CODE = 1001
    private var issued: android.app.PendingIntent? = null

    fun issue(
        context: Context,
        secure: Boolean,
    ): String {
        // the escrow code the mutable PI would expose lives in app storage —
        // written only in the insecure mode
        context.getSharedPreferences("siege_chain_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("escrow_code", if (!secure) ComponentFlags.REDIRECT_L3 else null)
            .commit()
        val base =
            if (secure) {
                // hardened: explicit component to a benign screen — never the vault
                Intent(context, OfferLinkActivity::class.java)
            } else {
                Intent() // blank, implicit base — the bug
            }
        val mutability =
            if (secure) {
                android.app.PendingIntent.FLAG_IMMUTABLE
            } else {
                android.app.PendingIntent.FLAG_MUTABLE
            }
        val pi =
            android.app.PendingIntent.getActivity(
                context,
                REQUEST_CODE,
                base,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or mutability,
            )
        issued = pi
        return "PendingIntent issued: mutable=${!secure}, baseComponent=" +
            (if (secure) "OfferLinkActivity" else "<none>")
    }

    fun hijack(
        context: Context,
        secure: Boolean,
    ): String {
        val pi =
            issued
                ?: return "Issue the notification first."
        // the attacker fills the mutable PI at send time; immutable PIs ignore fill-ins
        val fillIn =
            Intent(context, FlagVaultActivity::class.java)
        pi.send(context, 0, fillIn)
        return if (!secure) {
            "fillIn applied — the internal vault opened with the attacker's redirect. " +
                "Its code is on screen."
        } else {
            "fillIn ignored (immutable): the original explicit target ran instead."
        }
    }
}

class IntentRedirL3Challenge : TieredChallenge(
    category = "components",
    slug = "intentredir",
    level = Difficulty.HARD,
    title = "Mutable Pending",
    brief = "The wallet hands out a notification PendingIntent built from a blank " +
        "implicit intent — and what the receiver fills in, the app executes.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-1", "MASTG-TEST-0x64"),
    hints = listOf(
        "Issue the offer notification, then look at what the engine reports: mutable, blank base.",
        "The hijack simulator re-registers the same requestCode with a filled intent — " +
            "FLAG_UPDATE_CURRENT overwrites the base the PI will run.",
        "A hardened build creates it immutable with an explicit component, and the " +
            "simulator's overwrite is rejected.",
    ),
    flag = "DS{components_intentredir_L3_71a3e9}",
    learn = LearnContent(
        theory = "A PendingIntent executes with YOUR identity, in YOUR process. If its " +
            "base intent is implicit and mutable, whatever code receives it can fill in " +
            "the blanks — component, extras, action — and your app happily performs the " +
            "filled intent.\n\n" +
            "PendingIntents must be explicit and FLAG_IMMUTABLE unless mutability is a " +
            "documented requirement.",
        mastgRefs = listOf("MASVS-PLATFORM-1", "MASTG-TEST-0x64"),
        vulnerableSnippet = "PendingIntent.getActivity(ctx, 1001, Intent(),\n" +
            "    FLAG_UPDATE_CURRENT or FLAG_MUTABLE) // blank + mutable",
        fixSnippet = "val intent = Intent(ctx, OfferActivity::class.java)\n" +
            "PendingIntent.getActivity(ctx, 1001, intent,\n" +
            "    FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)",
        takeaway = "A mutable PendingIntent is your signature on someone else's intent.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ActionChallengeScreen(
            secureMode = secureMode,
            note = "Issue the notification, inspect the PendingIntent, then let the " +
                "attacker simulator fill it. The internal vault it opens shows the code.",
            actions = listOf(
                KitAction("Issue offer notification") { ctx, secure ->
                    PendingIntentVault.issue(ctx, secure)
                },
                KitAction("Attacker simulator: fill and fire") { ctx, secure ->
                    PendingIntentVault.hijack(ctx, secure)
                },
            ),
        )
    }
}

class IntentRedirL4Challenge : TieredChallenge(
    category = "components",
    slug = "intentredir",
    level = Difficulty.INSANE,
    title = "Confused Deputy",
    brief = "The support deputy verifies entitlements for other components — it does " +
        "not check who is asking. Make it verify yours.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-1", "MASTG-TEST-0x63"),
    hints = listOf(
        "DeputyActivity is exported and reads an action extra.",
        "action=verify-entitlement makes it perform the privileged verification and " +
            "print the escrow code on the caller's behalf.",
        "adb: am start DeputyActivity --es action verify-entitlement",
    ),
    flag = "DS{components_intentredir_L4_d84c16}",
    learn = LearnContent(
        theory = "Privileged helpers that skip caller validation do the bidding of " +
            "whoever asks. The deputy verifies entitlements and returns escrow codes — " +
            "an attacker needs neither the entitlement nor the code, only the deputy's " +
            "component name.\n\n" +
            "Privileged actions validate the caller (signature/uid) before performing " +
            "anything on a caller's behalf.",
        mastgRefs = listOf("MASVS-PLATFORM-1", "MASTG-TEST-0x63"),
        vulnerableSnippet = "if (action == \"verify-entitlement\") {\n" +
            "    entitle(caller); reveal(escrowCode) // no caller check",
        fixSnippet = "if (!CallerGuard.isTrustedCaller(this, callingPackage)) refuse()",
        takeaway = "Privilege without caller validation is a public service.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Show the deputy command") { _, _ ->
                    "adb shell am start -n com.droidsiege/.challenges.components.DeputyActivity " +
                        "--es action verify-entitlement"
                },
            ),
        )
    }
}
