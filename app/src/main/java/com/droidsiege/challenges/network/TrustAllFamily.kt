package com.droidsiege.challenges.network

import androidx.compose.runtime.Composable
import com.droidsiege.BuildConfig
import com.droidsiege.challenges.common.ActionChallengeScreen
import com.droidsiege.challenges.common.KitAction
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import okhttp3.OkHttpClient

class TrustAllL1Challenge : TieredChallenge(
    category = "network",
    slug = "trustall",
    level = Difficulty.EASY,
    title = "Accept Everything",
    brief = "The trust manager for this endpoint accepts every certificate ever " +
        "issued. Introduce an impostor and watch the payload arrive anyway.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-1", "MASTG-TEST-0x56"),
    hints = listOf(
        "Enable proxy mode: the endpoint you reach is now the attacker's TLS server.",
        "The custom TrustManager validates nothing — the impostor's cert passes.",
        "Secure mode uses the platform trust store: the impostor is rejected.",
    ),
    flag = "DS{network_trustall_L1_78b4c2}",
    learn = LearnContent(
        theory = "A TrustManager whose check methods are empty accepts any chain, " +
            "including a proxy's self-signed impostor. TLS degrades from 'authenticated " +
            "channel' to 'some encrypted channel' — encryption to whom, exactly?\n\n" +
            "Never override trust validation; if a special trust anchor is genuinely " +
            "needed, scope it to one pin and one host.",
        mastgRefs = listOf("MASVS-NETWORK-1", "MASTG-TEST-0x56"),
        vulnerableSnippet = "override fun checkServerTrusted(chain, authType) { /* trust all */ }",
        fixSnippet = "// use the platform default TrustManager — no override",
        takeaway = "Empty validation is total trust.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Toggle proxy mode (attacker)") { _, _ ->
                    MockBackend.mitmEnabled = !MockBackend.mitmEnabled
                    "proxy mode = ${MockBackend.mitmEnabled}"
                },
                KitAction("Fetch payload") { _, secure ->
                    MockBackend.ensureStarted()
                    exchange(
                        if (secure) {
                            MockBackend.defaultTlsClient()
                        } else {
                            NetworkClients.trustAllClient()
                        },
                        NetworkClients.httpsUrl(viaMitm = MockBackend.mitmEnabled, path = "/trust/L1"),
                    )
                },
                KitAction("Show what the proxy captured") { _, _ ->
                    MockBackend.requestLog.joinToString("\n").ifEmpty { "(log empty — fetch first)" }
                },
            ),
        )
    }
}

class TrustAllL2Challenge : TieredChallenge(
    category = "network",
    slug = "trustall",
    level = Difficulty.MEDIUM,
    title = "Any Host Will Do",
    brief = "Certificates validate; hostnames apparently don't. The endpoint answers " +
        "for a different identity — the client is fine with it.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-1", "MASTG-TEST-0x56"),
    hints = listOf(
        "The connection lands on a certificate issued for mitm.siege.local.",
        "The HostnameVerifier returns true unconditionally — the mismatch is waved through.",
        "Secure mode keeps the default hostname verification.",
    ),
    flag = "DS{network_trustall_L2_0d96e5}",
    learn = LearnContent(
        theory = "Certificate validation and hostname verification are separate checks; " +
            "failing either must kill the connection. A HostnameVerifier that returns " +
            "true lets an attacker present ANY valid certificate (their own blog's, a " +
            "CDN's) and terminate your TLS.\n\n" +
            "Hostname verification is not optional hardening — it is half of what TLS " +
            "authentication means.",
        mastgRefs = listOf("MASVS-NETWORK-1", "MASTG-TEST-0x56"),
        vulnerableSnippet = "builder.hostnameVerifier { _, _ -> true }",
        fixSnippet = "// default verification, or a strict allowlist of hosts",
        takeaway = "A valid certificate for the wrong host authenticates the wrong host.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Toggle proxy mode (attacker)") { _, _ ->
                    MockBackend.mitmEnabled = !MockBackend.mitmEnabled
                    "proxy mode = ${MockBackend.mitmEnabled}"
                },
                KitAction("Fetch payload (wrong-host cert)") { _, secure ->
                    MockBackend.ensureStarted()
                    exchange(
                        if (secure) {
                            MockBackend
                                .defaultTlsClient()
                                .newBuilder()
                                .build()
                        } else {
                            NetworkClients.hostnameBypassClient(viaMitm = MockBackend.mitmEnabled)
                        },
                        NetworkClients.httpsUrl(viaMitm = MockBackend.mitmEnabled, path = "/trust/L2"),
                    )
                },
                KitAction("Show what the proxy captured") { _, _ ->
                    MockBackend.requestLog.joinToString("\n").ifEmpty { "(log empty — fetch first)" }
                },
            ),
        )
    }
}

class TrustAllL3Challenge : TieredChallenge(
    category = "network",
    slug = "trustall",
    level = Difficulty.HARD,
    title = "Debug Trust",
    brief = "Trust-all validation only runs when the debug flag is on. This build is " +
        "a debug build. Draw your own conclusions.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-1", "M8", "MASTG-TEST-0x56"),
    hints = listOf(
        "BuildConfig.DEBUG gates the insecure client — and this artifact is the debug build.",
        "Proxy mode + fetch: the debug client trusts the impostor.",
        "The guard protects release builds only — sideloaded debug builds are a real target.",
    ),
    flag = "DS{network_trustall_L3_41f7a3}",
    learn = LearnContent(
        theory = "Gating insecure trust on BuildConfig.DEBUG protects the release " +
            "artifact and nothing else. Debug builds ship to testers, researchers and " +
            "bug-bounty programs, and they carry the same secrets.\n\n" +
            "Trust policy must be identical in every build variant; debug-only " +
            "convenience belongs in build-type-specific code that never reaches the " +
            "shared path.",
        mastgRefs = listOf("MASVS-NETWORK-1", "MASVS-RESILIENCE-2", "MASTG-TEST-0x56"),
        vulnerableSnippet = "val client =\n" +
            "    if (BuildConfig.DEBUG) trustAllClient() else defaultClient()",
        fixSnippet = "val client = defaultClient() // same trust in every variant",
        takeaway = "Debug builds are artifacts too — attackers sideload them on purpose.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Toggle proxy mode (attacker)") { _, _ ->
                    MockBackend.mitmEnabled = !MockBackend.mitmEnabled
                    "proxy mode = ${MockBackend.mitmEnabled}"
                },
                KitAction("Fetch payload") { _, secure ->
                    MockBackend.ensureStarted()
                    val client =
                        if (!secure && BuildConfig.DEBUG) {
                            NetworkClients.trustAllClient()
                        } else {
                            MockBackend.defaultTlsClient()
                        }
                    exchange(
                        client,
                        NetworkClients.httpsUrl(viaMitm = MockBackend.mitmEnabled, path = "/trust/L3"),
                    )
                },
                KitAction("Show what the proxy captured") { _, _ ->
                    MockBackend.requestLog.joinToString("\n").ifEmpty { "(log empty — fetch first)" }
                },
            ),
        )
    }
}

/** L4 — the "bundled SDK" whose networking layer trusts everything. */
object SdkStub {
    fun fetchPayload(
        client: OkHttpClient,
        url: String,
    ): String = exchange(client, url)
}

class TrustAllL4Challenge : TieredChallenge(
    category = "network",
    slug = "trustall",
    level = Difficulty.INSANE,
    title = "SDK Shortcut",
    brief = "Analytics went through a bundled SDK, and the SDK brought its own " +
        "networking layer. Its trust decisions are not your trust decisions.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-1", "M2", "MASTG-TEST-0x56"),
    hints = listOf(
        "Route the request through SdkStub — its client accepts any certificate.",
        "Proxy mode + the SDK path: the payload flows through the attacker's endpoint.",
        "Secure builds dropped the SDK's transport for their own hardened client.",
    ),
    flag = "DS{network_trustall_L4_6c28d9}",
    learn = LearnContent(
        theory = "Bundled SDKs carry their own network stacks, and their trust " +
            "decisions apply to whatever data flows through them. A payload routed " +
            "through a trust-all SDK path is transmitted with no certificate validation " +
            "at all — even if your own client is hardened.\n\n" +
            "Audit third-party transports like first-party ones, or keep sensitive " +
            "payloads out of them entirely.",
        mastgRefs = listOf("MASVS-NETWORK-1", "MASVS-RESILIENCE-3", "MASTG-TEST-0x56"),
        vulnerableSnippet = "SdkStub.fetchPayload(trustAllClient(), url) // the SDK trusts all",
        fixSnippet = "// sensitive payloads use the app's hardened client only",
        takeaway = "Your TLS policy ends where your SDK's transport begins.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Toggle proxy mode (attacker)") { _, _ ->
                    MockBackend.mitmEnabled = !MockBackend.mitmEnabled
                    "proxy mode = ${MockBackend.mitmEnabled}"
                },
                KitAction("Send payload through the SDK") { _, secure ->
                    MockBackend.ensureStarted()
                    val client =
                        if (!secure) {
                            NetworkClients.trustAllClient()
                        } else {
                            MockBackend.trustingClient()
                        }
                    SdkStub.fetchPayload(
                        client,
                        NetworkClients.httpsUrl(viaMitm = MockBackend.mitmEnabled, path = "/sdk/L4"),
                    )
                },
                KitAction("Show what the proxy captured") { _, _ ->
                    MockBackend.requestLog.joinToString("\n").ifEmpty { "(log empty — fetch first)" }
                },
            ),
        )
    }
}
