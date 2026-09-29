package com.droidsiege.challenges.components

import androidx.compose.runtime.Composable
import com.droidsiege.challenges.common.ActionChallengeScreen
import com.droidsiege.challenges.common.KitAction
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent

class DeepLinkL1Challenge : TieredChallenge(
    category = "components",
    slug = "deeplink",
    level = Difficulty.EASY,
    title = "Recover Link",
    brief = "Recovery codes are one deep link away in this build — the scheme is " +
        "documented in the manifest like every other entry point.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-1", "MASTG-TEST-0x65"),
    hints = listOf(
        "The manifest lists the scheme: siege://recover.",
        "adb shell am start -a android.intent.action.VIEW -d 'siege://recover'",
        "Secure builds require an authenticated session before rendering.",
    ),
    flag = "DS{components_deeplink_L1_9c52fa}",
    learn = LearnContent(
        theory = "Deep links are exported entry points with a nicer syntax. Anything " +
            "they render, a browser, an NFC tag, or another app can trigger — no app " +
            "navigation or authentication involved unless the link itself demands it.\n\n" +
            "Sensitive screens must not be reachable by link without an authenticated " +
            "session.",
        mastgRefs = listOf("MASVS-PLATFORM-1", "MASTG-TEST-0x65"),
        vulnerableSnippet = "<data android:scheme=\"siege\" android:host=\"recover\" />\n" +
            "// activity renders the recovery code for any viewer",
        fixSnippet = "// gate on the session before rendering:\n" +
            "if (!session.valid) showLogin()",
        takeaway = "A deep link is startActivity for people who never opened your app.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction(
                    "Show the link",
                ) { _, _ -> "adb shell am start -a android.intent.action.VIEW -d 'siege://recover'" },
            ),
        )
    }
}

class DeepLinkL2Challenge : TieredChallenge(
    category = "components",
    slug = "deeplink",
    level = Difficulty.MEDIUM,
    title = "Account Parameter",
    brief = "The recovery link takes an account parameter, so support can pull codes " +
        "for any user. Try the one account you should not be able to pull.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-1", "M1", "MASTG-TEST-0x65"),
    hints = listOf(
        "The parameter is ?account=<name> — the privileged account is named admin_vault.",
        "adb: am start -a VIEW -d 'siege://recover?account=admin_vault'",
        "Hardened builds validate the account against the signed-in user's allowlist.",
    ),
    flag = "DS{components_deeplink_L2_48e7b1}",
    learn = LearnContent(
        theory = "A deep-link parameter that selects whose data to return is an IDOR " +
            "with a link wrapper: the caller picks the account, the screen trusts it. " +
            "Parameterized privilege — 'show me account X' — must be bounded by the " +
            "authenticated user, never by the link.\n\n" +
            "Validate every parameter against the session's entitlements.",
        mastgRefs = listOf("MASVS-PLATFORM-1", "MASTG-TEST-0x65"),
        vulnerableSnippet = "val account = data.getQueryParameter(\"account\")\n" +
            "show(recoveryCodeFor(account))",
        fixSnippet = "// only the signed-in user's own account is linkable:\n" +
            "if (account != session.username) refuse()",
        takeaway = "Caller-chosen parameters are caller-chosen privileges.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Show the link") { _, _ ->
                    "adb shell am start -a android.intent.action.VIEW -d 'siege://recover?account=admin_vault'"
                },
            ),
        )
    }
}

class DeepLinkL3Challenge : TieredChallenge(
    category = "components",
    slug = "deeplink",
    level = Difficulty.HARD,
    title = "Unverified Host",
    brief = "The offers site moved to https links handled by the app. Verification " +
        "was 'on the roadmap'. Claim the host, collect the code.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-1", "MASTG-TEST-0x65"),
    hints = listOf(
        "The intent filter has no autoVerify — Android treats offers.siegeapp.dev as unverified.",
        "Any app (or adb) can fire https://offers.siegeapp.dev/redeem and the app answers.",
        "adb: am start -a VIEW -d 'https://offers.siegeapp.dev/redeem'",
    ),
    flag = "DS{components_deeplink_L3_f06d93}",
    learn = LearnContent(
        theory = "Without App Link verification (assetlinks.json + autoVerify), an " +
            "https deep link is just a custom scheme in a suit: any installed app can " +
            "register the same host and intercept, or the link fires on an attacker's " +
            "terms. The browser-style trust the URL implies does not exist.\n\n" +
            "Publish assetlinks.json, set autoVerify, and treat unverified links as " +
            "untrusted input.",
        mastgRefs = listOf("MASVS-PLATFORM-1", "MASTG-TEST-0x65"),
        vulnerableSnippet = "<intent-filter> <!-- no android:autoVerify=\"true\" -->\n" +
            "  <data android:scheme=\"https\" android:host=\"offers.siegeapp.dev\" />",
        fixSnippet = "<intent-filter android:autoVerify=\"true\"> …\n" +
            "// + assetlinks.json published for the host",
        takeaway = "Unverified app links are squattable — by anyone.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Show the link") { _, _ ->
                    "adb shell am start -a android.intent.action.VIEW " +
                        "-d 'https://offers.siegeapp.dev/redeem'"
                },
            ),
        )
    }
}

class DeepLinkL4Challenge : TieredChallenge(
    category = "components",
    slug = "deeplink",
    level = Difficulty.INSANE,
    title = "Reader Injection",
    brief = "The offers reader opens whatever URL its deep link carries. The app's " +
        "own asset pages are part of 'whatever'.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-1", "M4", "MASTG-TEST-0x65"),
    hints = listOf(
        "The link shape is offers://load?url=<url> — the url lands in a WebView loadUrl.",
        "file:///android_asset/droidsiege_secret.html is inside the app and inside reach.",
        "adb: am start -a VIEW -d 'offers://load?url=file:///android_asset/droidsiege_secret.html'",
    ),
    flag = "DS{components_deeplink_L4_37ac28}",
    learn = LearnContent(
        theory = "A WebView that loads caller-controlled URLs chains every weakness " +
            "the URL scheme allows: app-private file:// pages, later script bridges " +
            "and redirect traps. The deep link supplies the attacker-controlled input " +
            "for free.\n\n" +
            "Allowlist scheme + host before loadUrl, and keep file access disabled " +
            "for WebViews that render remote input.",
        mastgRefs = listOf("MASVS-PLATFORM-1", "MASVS-CODE-2", "MASTG-TEST-0x65"),
        vulnerableSnippet = "val url = data.getQueryParameter(\"url\")\n" +
            "webView.loadUrl(url) // file:// included",
        fixSnippet = "if (!url.startsWith(\"https://offers.siegeapp.dev/\")) refuse()\n" +
            "webView.settings.allowFileAccess = false",
        takeaway = "A WebView is a door out of your app — the URL decides where.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Show the injection") { _, _ ->
                    "adb shell am start -a android.intent.action.VIEW " +
                        "-d 'offers://load?url=file:///android_asset/droidsiege_secret.html'"
                },
            ),
        )
    }
}
