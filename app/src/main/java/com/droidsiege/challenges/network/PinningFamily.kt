package com.droidsiege.challenges.network

import androidx.compose.runtime.Composable
import com.droidsiege.challenges.common.ActionChallengeScreen
import com.droidsiege.challenges.common.KitAction
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent

class PinningL1Challenge : TieredChallenge(
    category = "network",
    slug = "pinning",
    level = Difficulty.EASY,
    title = "No Pin",
    brief = "The premium channel runs over TLS but trusts whatever certificate the " +
        "connection presents. Turn on the proxy and read the channel.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-2", "MASTG-TEST-0x56"),
    hints = listOf(
        "Enable 'proxy mode', then fetch — the console shows what the proxy captured.",
        "With no pin, the attacker's certificate is trusted and the channel is fully visible.",
        "Secure mode uses the platform trust store only: the proxy's cert is rejected.",
    ),
    flag = "DS{network_pinning_L1_e4c192}",
    learn = LearnContent(
        theory = "TLS without pinning secures the channel against everyone the device " +
            "trusts — which, on a stock device with a user-installed CA (or a rooted " +
            "one), includes the attacker. The session payload is fully readable in the " +
            "proxy.\n\n" +
            "Certificate pinning binds the app to the server identity it expects, so " +
            "the proxy's certificate ends the handshake.",
        mastgRefs = listOf("MASVS-NETWORK-2", "MASTG-TEST-0x56"),
        vulnerableSnippet = "OkHttpClient() // default trust; no CertificatePinner",
        fixSnippet = "val pinner = CertificatePinner.Builder()\n" +
            "    .add(host, \"sha256/<server-key-pin>\")\n" +
            "    .build()",
        takeaway = "Default TLS trusts whoever the device trusts — pin the server.",
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
                KitAction("Fetch premium payload") { _, secure ->
                    MockBackend.ensureStarted()
                    exchange(
                        if (secure) {
                            MockBackend.defaultTlsClient()
                        } else {
                            NetworkClients.userCaClient(
                                viaMitm = MockBackend.mitmEnabled,
                            )
                        },
                        NetworkClients.httpsUrl(viaMitm = MockBackend.mitmEnabled, path = "/pin/L1"),
                    )
                },
                KitAction("Show what the proxy captured") { _, _ ->
                    MockBackend.requestLog.joinToString("\n").ifEmpty { "(log empty — fetch first)" }
                },
            ),
        )
    }
}

class PinningL2Challenge : TieredChallenge(
    category = "network",
    slug = "pinning",
    level = Difficulty.MEDIUM,
    title = "User CA Trust",
    brief = "The security config trusts user-installed CAs 'for testing convenience'. " +
        "The proxy ships its own CA.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-2", "MASTG-TEST-0x56"),
    hints = listOf(
        "Proxy mode + fetch: the trust store includes the user CA the attacker installed.",
        "The captured-body line is the flag's route through the proxy.",
        "Hardened builds restrict trust anchors to the system store.",
    ),
    flag = "DS{network_pinning_L2_570ad8}",
    learn = LearnContent(
        theory = "Trusting user-installed CAs moves the root of trust onto the user's " +
            "device — and the user (or a five-minute malware) is the attacker. With a " +
            "user CA installed, the proxy's certificate validates and the TLS session " +
            "is transparent.\n\n" +
            "Production builds must trust only the system store; debug convenience " +
            "belongs in debug builds.",
        mastgRefs = listOf("MASVS-NETWORK-2", "MASTG-TEST-0x56"),
        vulnerableSnippet = "<certificates src=\"user\" /> <!-- trusts user CAs -->",
        fixSnippet = "<certificates src=\"system\" />",
        takeaway = "A user CA is an attacker CA with a friendlier name.",
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
                            NetworkClients.userCaClient(
                                viaMitm = MockBackend.mitmEnabled,
                            )
                        },
                        NetworkClients.httpsUrl(viaMitm = MockBackend.mitmEnabled, path = "/pin/L2"),
                    )
                },
                KitAction("Show what the proxy captured") { _, _ ->
                    MockBackend.requestLog.joinToString("\n").ifEmpty { "(log empty — fetch first)" }
                },
            ),
        )
    }
}

class PinningL3Challenge : TieredChallenge(
    category = "network",
    slug = "pinning",
    level = Difficulty.HARD,
    title = "Wrong Pin",
    brief = "Pinning shipped: a CertificatePinner with exactly one pin. Read the pin " +
        "configuration closely — then collect the channel anyway.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-2", "MASTG-TEST-0x56"),
    hints = listOf(
        "The pin's host pattern decides when it is enforced. Read which host it covers.",
        "The pin is registered for other.siege.local — the connection goes to " +
            "trusted.siege.local, so the pin never applies.",
        "Proxy mode + fetch still captures the payload; the fix pins the actual host " +
            "with a backup pin.",
    ),
    flag = "DS{network_pinning_L3_b26f45}",
    learn = LearnContent(
        theory = "A pin on the wrong host is theater: CertificatePinner matches pins by " +
            "hostname pattern, and a pattern that never matches the connected host is " +
            "decorative. The channel behaves exactly like the un-pinned one.\n\n" +
            "Pin the host you connect to, pin the backup key too, and test the failure " +
            "path — a pin that never fires is a pin you don't have.",
        mastgRefs = listOf("MASVS-NETWORK-2", "MASTG-TEST-0x56"),
        vulnerableSnippet = "pinner.add(\"other.siege.local\", pin) // host never used",
        fixSnippet = "pinner.add(\"trusted.siege.local\", primaryPin)\n" +
            "pinner.add(\"trusted.siege.local\", backupPin)",
        takeaway = "Test your pins by breaking them — an untested pin is absent.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Show the pin configuration") { _, _ ->
                    "pinner.add(\"other.siege.local\", …)  // wrong pattern\n" +
                        "connects to: trusted.siege.local"
                },
                KitAction("Toggle proxy mode (attacker)") { _, _ ->
                    MockBackend.mitmEnabled = !MockBackend.mitmEnabled
                    "proxy mode = ${MockBackend.mitmEnabled}"
                },
                KitAction("Fetch payload") { _, secure ->
                    MockBackend.ensureStarted()
                    exchange(
                        if (secure) {
                            NetworkClients.pinnedClient(viaMitm = MockBackend.mitmEnabled, correctPin = true)
                        } else {
                            NetworkClients.pinnedClient(viaMitm = MockBackend.mitmEnabled, correctPin = false)
                        },
                        NetworkClients.httpsUrl(viaMitm = MockBackend.mitmEnabled, path = "/pin/L3"),
                    )
                },
                KitAction("Show what the proxy captured") { _, _ ->
                    MockBackend.requestLog.joinToString("\n").ifEmpty { "(log empty — fetch first)" }
                },
            ),
        )
    }
}

class PinningL4Challenge : TieredChallenge(
    category = "network",
    slug = "pinning",
    level = Difficulty.INSANE,
    title = "Custom Trust",
    brief = "The channel's trust decision now lives in custom code that validates the " +
        "peer principal itself. The proxy's identity does not pass — by design.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-2", "MASTG-TEST-0x56"),
    hints = listOf(
        "Enable proxy mode and fetch: the custom trust manager rejects the attacker's principal.",
        "The payload only travels over the honest channel — the flag is the successful fetch.",
        "Bypassing this tier requires runtime hooks (Phase 5/7 tooling), not a proxy.",
    ),
    flag = "DS{network_pinning_L4_93d7e0}",
    learn = LearnContent(
        theory = "Custom trust code that validates the peer identity directly is the " +
            "hardest channel defense to remove from a proxy's reach: there is no pin " +
            "list to spoof, only a comparison the attacker must satisfy.\n\n" +
            "It still executes in process memory — which is exactly what runtime " +
            "tooling observes. Pair it with integrity checks and keep secrets behind " +
            "server-side authentication.",
        mastgRefs = listOf("MASVS-NETWORK-2", "MASTG-TEST-0x56"),
        vulnerableSnippet = "// correct custom validation:\n" +
            "if (!peer.cn.contains(VIRTUAL_HOST)) throw CertificateException()",
        fixSnippet = "// + attestation binding the channel to a verified app build",
        takeaway = "Custom trust done right resists proxies — but not hooks.",
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
                KitAction("Fetch payload over the custom-pinned channel") { _, _ ->
                    MockBackend.ensureStarted()
                    exchange(
                        NetworkClients.customPinnedClient(),
                        NetworkClients.httpsUrl(viaMitm = false, path = "/pin/L4"),
                    )
                },
            ),
        )
    }
}
