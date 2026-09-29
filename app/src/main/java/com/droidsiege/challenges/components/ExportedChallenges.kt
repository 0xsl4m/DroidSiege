package com.droidsiege.challenges.components

import androidx.compose.runtime.Composable
import com.droidsiege.challenges.common.ActionChallengeScreen
import com.droidsiege.challenges.common.KitAction
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent

private fun adbStart(component: String) = "adb shell am start -n com.droidsiege/$component"

class ExportedL1Challenge : TieredChallenge(
    category = "components",
    slug = "exported",
    level = Difficulty.EASY,
    title = "Recovery Portal",
    brief = "The wallet ships a recovery portal screen. Launch it the way another app would.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-1", "MASTG-TEST-0x61"),
    hints = listOf(
        "Manifest-declared activities are visible to anyone: aapt2 dump or jadx lists them.",
        "ExportFlagActivity is exported=true — no permission gates it.",
        adbStart("com.droidsiege.challenges.components.ExportFlagActivity"),
    ),
    flag = "DS{components_exported_L1_5c9e14}",
    learn = LearnContent(
        theory = "An exported component is a public API surface you did not document. " +
            "Anything it renders or returns, another app — or the device owner with adb — " +
            "gets by launching it directly, with no in-app navigation required.\n\n" +
            "Components must be non-exported unless they are real entry points, and entry " +
            "points must validate their callers.",
        mastgRefs = listOf("MASVS-PLATFORM-1", "MASTG-TEST-0x61"),
        vulnerableSnippet = "<activity android:name=\".ExportFlagActivity\"\n" +
            "    android:exported=\"true\" />",
        fixSnippet = "android:exported=\"false\"\n" +
            "// or android:permission=\"<signature-level permission>\"",
        takeaway = "Every exported component is a front door — count yours and lock the rest.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            note = "The component is live in the manifest. Launch it externally, then submit " +
                "the code it renders. Secure mode enables the caller guard.",
            actions = listOf(
                KitAction("Show the adb launch command") { _, _ ->
                    adbStart("com.droidsiege.challenges.components.ExportFlagActivity")
                },
            ),
        )
    }
}

class ExportedL2Challenge : TieredChallenge(
    category = "components",
    slug = "exported",
    level = Difficulty.MEDIUM,
    title = "Keyed Export",
    brief = "The keyed export screen demands a master key. The key ships with the app — " +
        "the screen is just particular about the extra's name.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-1", "MASTG-TEST-0x61"),
    hints = listOf(
        "The extra name matters as much as the value: --es master_key <value>.",
        "The master key is a string constant in the app — strings.xml and jadx both have it.",
        "KeyedExportActivity compares against siege-master-key-2026.",
    ),
    flag = "DS{components_exported_L2_7d2f68}",
    learn = LearnContent(
        theory = "Requiring an intent extra is not access control when the required value " +
            "ships inside the APK. The attacker reads the constant from the decompiled " +
            "code and crafts the exact intent the screen demands.\n\n" +
            "Caller validation must depend on something the caller cannot read from the " +
            "package — a signature-level permission or a uid check.",
        mastgRefs = listOf("MASVS-PLATFORM-1", "MASTG-TEST-0x61"),
        vulnerableSnippet = "if (intent.getStringExtra(\"master_key\") == MASTER_KEY) { reveal() }",
        fixSnippet = "// validate who is calling, not what they typed:\n" +
            "checkCallingPermission(SIGNATURE_PERMISSION)",
        takeaway = "A password written next to the lock is decoration.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            note = "Craft the launch intent with the right extra. Secure mode adds caller validation.",
            actions = listOf(
                KitAction("Show the adb launch command") { _, _ ->
                    adbStart("com.droidsiege.challenges.components.KeyedExportActivity") +
                        " --es master_key siege-master-key-2026"
                },
            ),
        )
    }
}

class ExportedL3Challenge : TieredChallenge(
    category = "components",
    slug = "exported",
    level = Difficulty.HARD,
    title = "Messenger Service",
    brief = "The recovery service answers messages from any app bound to it. Ask it " +
        "politely — or bind and see what the weak check misses.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-1", "MASTG-TEST-0x61"),
    hints = listOf(
        "ExportMessengerService is exported and binds with a Messenger reply-to.",
        "Send MSG_GET_RECOVERY (what = 1). The insecure path trusts a caller_package " +
            "string the message itself carries.",
        "Secure builds validate the real uid via CallerGuard instead.",
    ),
    flag = "DS{components_exported_L3_94ba30}",
    learn = LearnContent(
        theory = "Exported services hand your code to other processes. A Messenger " +
            "protocol that trusts a caller identity written into the message is a " +
            "self-issued passport: the attacker writes whatever the check wants to see.\n\n" +
            "Caller identity must come from the binder transaction (uid/package), never " +
            "from message contents — and privileged services need signature permissions.",
        mastgRefs = listOf("MASVS-PLATFORM-1", "MASTG-TEST-0x61"),
        vulnerableSnippet = "val claimed = msg.data.getString(\"caller_package\")\n" +
            "if (claimed == packageName) reply(recovery)",
        fixSnippet = "val real = packageManager.getNameForUid(Binder.getCallingUid())\n" +
            "if (CallerGuard.isTrustedCaller(this, real)) reply(recovery)",
        takeaway = "Trust the binder, not the envelope.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            note = "The service runs. Bind from any client (see the tools pack for a " +
                "two-line client app and an adb-driven script).",
            actions = listOf(
                KitAction("Show the component and message contract") { _, _ ->
                    "ExportMessengerService, exported; msg.what=1 (MSG_GET_RECOVERY), " +
                        "reply bundle key=recovery; insecure path trusts data.caller_package"
                },
            ),
        )
    }
}

class ExportedL4Challenge : TieredChallenge(
    category = "components",
    slug = "exported",
    level = Difficulty.INSANE,
    title = "Escrow Chain",
    brief = "Two components, one chain: a receiver arms the escrow, an activity reveals " +
        "it. Each link trusts the previous one.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-1", "MASTG-TEST-0x61"),
    hints = listOf(
        "Arm the chain first: broadcast com.droidsiege.CHAIN_ARM with arm=true.",
        "Then launch ChainActivity — it renders the code because the state is armed.",
        "adb: am broadcast -a com.droidsiege.CHAIN_ARM --ez arm true, then am start ChainActivity.",
    ),
    flag = "DS{components_exported_L4_c1e75b}",
    learn = LearnContent(
        theory = "Component chains multiply exposure: if either link trusts the input it " +
            "receives, the whole chain is invocable from outside. Here a broadcast arms " +
            "state and an exported activity reads it — two exported links, zero checks.\n\n" +
            "Chain participants must validate both the signal and the caller, and " +
            "privileged state belongs behind non-exported boundaries.",
        mastgRefs = listOf("MASVS-PLATFORM-1", "MASTG-TEST-0x61"),
        vulnerableSnippet = "// receiver: arms on any broadcast\n" +
            "if (intent.action == \"com.droidsiege.CHAIN_ARM\") armChain(true)\n" +
            "// activity: renders whenever armed\n" +
            "if (chainArmed()) reveal()",
        fixSnippet = "// validate the sender, and arm only for trusted callers:\n" +
            "if (senderId != INTERNAL_TOKEN) return",
        takeaway = "A chain is as weak as its most trusting link.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            note = "Both links are exported. Fire the broadcast, then the activity. " +
                "Secure builds require the internal sender id and caller validation.",
            actions = listOf(
                KitAction("Show the chain commands") { _, _ ->
                    "adb shell am broadcast -a com.droidsiege.CHAIN_ARM --ez arm true\n" +
                        adbStart("com.droidsiege.challenges.components.ChainActivity")
                },
            ),
        )
    }
}
