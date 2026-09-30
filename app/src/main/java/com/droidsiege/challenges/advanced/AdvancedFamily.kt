package com.droidsiege.challenges.advanced

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidsiege.challenges.common.ChallengeConsole
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent

class StrandhoggL1Challenge : TieredChallenge(
    category = "advanced",
    slug = "strandhogg",
    level = Difficulty.EASY,
    title = "Task Affinity",
    brief = "The recovery activity keeps its default taskAffinity and allows " +
        "reparenting — an attacker task can pull it in and overlay a phish.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-1", "MASTG-TEST-0x65"),
    hints = listOf(
        "The activity declares no taskAffinity and allows reparenting.",
        "A malicious app with a matching affinity pulls the task into its own stack.",
        "Hardened: empty taskAffinity + singleTask + no reparenting.",
    ),
    flag = "DS{advanced_strandhogg_L1_b37f92}",
    learn = LearnContent(
        theory = "StrandHogg abuses task affinity to insert a malicious activity " +
            "into the victim app's task: the user sees the legitimate app icon and " +
            "the attacker's overlay on top, harvesting the recovery entry.\n\n" +
            "Empty taskAffinity, explicit launchMode and reparenting opt-outs close " +
            "the classic variant.",
        mastgRefs = listOf("MASVS-PLATFORM-1", "MASTG-TEST-0x65"),
        vulnerableSnippet = "<activity android:allowTaskReparenting=\"true\" />",
        fixSnippet = "android:taskAffinity=\"\"\nandroid:allowTaskReparenting=\"false\"",
        takeaway = "Default task semantics are attacker-friendly for sensitive screens.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (secureMode) {
                    "hardened: taskAffinity=\"\" + singleTask + no reparenting"
                } else {
                    "default affinity + reparenting — a phish task can overlay this screen"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {
                output =
                    if (!secureMode) {
                        "attacker task reparented this activity and drew the overlay — " +
                            "harvested recovery entry: DS{advanced_strandhogg_L1_b37f92}"
                    } else {
                        "hardened: empty taskAffinity + no reparenting — the phish task cannot attach"
                    }
            }, modifier = Modifier.fillMaxWidth()) { Text("Open recovery screen") }
            ChallengeConsole(output)
        }
    }
}

class TapjackingL1Challenge : TieredChallenge(
    category = "advanced",
    slug = "tapjacking",
    level = Difficulty.EASY,
    title = "Obscured Tap",
    brief = "The confirm button does not filter obscured touches. An overlay can " +
        "ride the user's tap and fire the flag reveal.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-1", "MASTG-TEST-0x65"),
    hints = listOf(
        "The button lacks filterTouchesWhenObscured — an overlay passes the tap through.",
        "The attacker's overlay shows 'I agree to terms' while the tap hits Confirm.",
        "Secure builds set filterTouchesWhenObscured=true on sensitive views.",
    ),
    flag = "DS{advanced_tapjacking_L1_8c04d6}",
    learn = LearnContent(
        theory = "Tapjacking stacks an overlay over a sensitive button: the user " +
            "thinks they tap the overlay, the tap passes through to the hidden " +
            "button. Without obscured-touch filtering the framework delivers both.\n\n" +
            "filterTouchesWhenObscured=true makes the view refuse touches while " +
            "obscured.",
        mastgRefs = listOf("MASVS-PLATFORM-1", "MASTG-TEST-0x65"),
        vulnerableSnippet = "// confirm button: no obscured-touch filtering",
        fixSnippet = "button.filterTouchesWhenObscured = true",
        takeaway = "A tap is trustworthy only if nothing covers the view.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (secureMode) {
                    "hardened: sensitive views filter obscured touches"
                } else {
                    "confirm button accepts taps through overlays"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {
                output =
                    if (!secureMode) {
                        "tap accepted through an overlay — flag revealed:\n" +
                            "DS{advanced_tapjacking_L1_8c04d6}"
                    } else {
                        "tap refused: view obscured"
                    }
            }, modifier = Modifier.fillMaxWidth()) { Text("Confirm transfer") }
            ChallengeConsole(output)
        }
    }
}

class CustomTabsL1Challenge : TieredChallenge(
    category = "advanced",
    slug = "customtabs",
    level = Difficulty.EASY,
    title = "Secret in Tab",
    brief = "The app opens its offers page in a Custom Tab and appends the session " +
        "recovery code as a query parameter. Capture the URL.",
    owaspRefs = listOf("M5", "MASVS-NETWORK-1", "MASTG-TEST-0x56"),
    hints = listOf(
        "The launch intent's data URL carries ?code=… — read the log.",
        "Any Custom Tabs callback or the browser history sees the full URL.",
        "Secure builds keep secrets out of URLs entirely.",
    ),
    flag = "DS{advanced_customtabs_L1_25e8a1}",
    learn = LearnContent(
        theory = "Custom Tabs are a browser surface: URLs passed to them are visible " +
            "to the browser, its history, redirects and any logging in the chain. A " +
            "secret appended to the URL is published.\n\n" +
            "No secrets in URLs; tokens ride in headers or bodies over TLS.",
        mastgRefs = listOf("MASVS-NETWORK-1", "MASTG-TEST-0x56"),
        vulnerableSnippet = "tabsIntent.dataUri = Uri.parse(\"https://…?code=${'$'}code\")",
        fixSnippet = "// keep the code out of the URL; use a POST body via your own screen",
        takeaway = "Everything after the ? is public by convention.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                if (!secureMode) {
                    // launch the browser-visible surface with the code in the URL
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse(
                                "https://offers.siegeapp.dev/redeem?code=DS{advanced_customtabs_L1_25e8a1}",
                            )
                        },
                    )
                    output = "Custom Tab opened with the code in the URL — visible in " +
                        "browser history and any logging"
                } else {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("https://offers.siegeapp.dev/redeem")
                        },
                    )
                    output = "hardened: code redeemed server-side, no secret in the URL"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Open offers in a tab") }
            ChallengeConsole(output)
        }
    }
}
