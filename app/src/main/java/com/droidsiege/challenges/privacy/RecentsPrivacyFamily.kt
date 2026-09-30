package com.droidsiege.challenges.privacy

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidsiege.challenges.common.ChallengeConsole
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent

private const val RECENTS_L1 = "DS{privacy_recents-priv_L1_b4f861}"
private const val RECENTS_L2 = "DS{privacy_recents-priv_L2_27a0c3}"
private const val RECENTS_L3 = "DS{privacy_recents-priv_L3_e815d4}"
private const val RECENTS_L4 = "DS{privacy_recents-priv_L4_93c2f7}"

class RecentsL1Challenge : TieredChallenge(
    category = "privacy",
    slug = "recents-priv",
    level = Difficulty.EASY,
    title = "Recents Snapshot",
    brief = "The identity screen shows the full profile — and the recents switcher " +
        "snapshots it. Open recents and read the snapshot.",
    owaspRefs = listOf("M6", "MASVS-PRIVACY-1", "MASTG-TEST-0x62"),
    hints = listOf(
        "Render the profile, press home, open the recents switcher.",
        "The thumbnail shows the full identity record.",
        "Secure builds set FLAG_SECURE so the thumbnail is blank.",
    ),
    flag = RECENTS_L1,
    learn = LearnContent(
        theory = "The recents switcher snapshots every visible activity for the task " +
            "switcher. PII on screen becomes PII in the thumbnail — readable by anyone " +
            "who borrows the phone for five seconds.\n\n" +
            "FLAG_SECURE on sensitive screens blanks both the snapshot and user " +
            "screenshots.",
        mastgRefs = listOf("MASVS-PRIVACY-1", "MASTG-TEST-0x62"),
        vulnerableSnippet = "// identity screen: renders PII, no FLAG_SECURE",
        fixSnippet = "window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)",
        takeaway = "PII on screen is PII in the task switcher.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val activity = LocalContext.current as Activity
        androidx.compose.runtime.LaunchedEffect(secureMode) {
            if (secureMode) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        androidx.compose.runtime.DisposableEffect(Unit) {
            onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "identity on screen:\nHany Ibrahim · hany.ibrahim@siege.app\n$RECENTS_L1",
                style = MaterialTheme.typography.bodyLarge,
            )
            ChallengeConsole(if (secureMode) "FLAG_SECURE on — snapshot blank" else "no FLAG_SECURE — snapshot visible")
        }
    }
}

class RecentsL2Challenge : TieredChallenge(
    category = "privacy",
    slug = "recents-priv",
    level = Difficulty.MEDIUM,
    title = "Lockscreen Notification",
    brief = "The transfer notification shows the full recovery code on the lockscreen. " +
        "Read it without unlocking.",
    owaspRefs = listOf("M6", "MASVS-PRIVACY-1", "MASTG-TEST-0x62"),
    hints = listOf(
        "The notification posts the full recovery code with VISIBILITY_PUBLIC.",
        "Lock the device and read the notification from the lockscreen.",
        "Secure builds set VISIBILITY_SECRET — nothing on the lockscreen.",
    ),
    flag = RECENTS_L2,
    learn = LearnContent(
        theory = "Notifications default to lockscreen-visible. A recovery code in the " +
            "notification text is readable by anyone holding the locked phone, and is " +
            "mirrored to paired wearables.\n\n" +
            "Sensitive notifications set VISIBILITY_SECRET and show a redacted " +
            "summary on the lockscreen.",
        mastgRefs = listOf("MASVS-PRIVACY-1", "MASTG-TEST-0x62"),
        vulnerableSnippet = "NotificationCompat.Builder(ctx, id)\n" +
            "    .setContentText(\"code: ${'$'}recovery\") // public visibility",
        fixSnippet = "setVisibility(Notification.VISIBILITY_SECRET)",
        takeaway = "The lockscreen publishes everything the notification says.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val builder =
                        androidx.core.app.NotificationCompat
                            .Builder(context, "transfer")
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentTitle("Transfer complete")
                            .setContentText(
                                if (!secureMode) {
                                    "code: $RECENTS_L2"
                                } else {
                                    "transfer complete (content hidden on lockscreen)"
                                },
                            )
                    if (secureMode) {
                        builder.setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_SECRET)
                    }
                    val manager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        manager.createNotificationChannel(
                            android.app.NotificationChannel(
                                "transfer",
                                "Transfers",
                                android.app.NotificationManager.IMPORTANCE_HIGH,
                            ),
                        )
                    }
                    manager.notify(2001, builder.build())
                    output =
                        if (!secureMode) {
                            "notification posted — visible on the lockscreen"
                        } else {
                            "notification posted — content hidden on the lockscreen"
                        }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Post transfer notification") }
            ChallengeConsole(output)
        }
    }
}

class RecentsL3Challenge : TieredChallenge(
    category = "privacy",
    slug = "recents-priv",
    level = Difficulty.HARD,
    title = "Autofill Exposure",
    brief = "The identity form opts into autofill with full values — any autofill " +
        "service (including an attacker's) captures the stored PII.",
    owaspRefs = listOf("M6", "MASVS-PRIVACY-1", "MASTG-TEST-0x62"),
    hints = listOf(
        "The form fields carry importantForAutofill=auto and plain text types.",
        "An attacker-positioned autofill service receives the full values on focus.",
        "Secure builds exclude the fields from autofill entirely.",
    ),
    flag = RECENTS_L3,
    learn = LearnContent(
        theory = "Autofill services see every value in fields that opt into autofill. " +
            "An attacker-positioned service (or a malicious keyboard) harvests the " +
            "stored PII on focus — no exploitation beyond focusing the field.\n\n" +
            "Exclude sensitive fields from autofill (importantForAutofill=no) and use " +
            "password-style semantics.",
        mastgRefs = listOf("MASVS-PRIVACY-1", "MASTG-TEST-0x62"),
        vulnerableSnippet = "// fields autofill=true → service sees the values",
        fixSnippet = "View.setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_NO)",
        takeaway = "Autofill is a data-sharing agreement you signed by default.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var name by rememberSaveable { mutableStateOf("Hany Ibrahim") }
        var recovery by rememberSaveable { mutableStateOf(RECENTS_L3) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (secureMode) {
                    "hardened: fields excluded from autofill (importantForAutofill=no)"
                } else {
                    "fields autofill-enabled — any service captures them"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { androidx.compose.material3.Text("full name") },
                modifier = Modifier.fillMaxWidth(),
            )
            androidx.compose.material3.OutlinedTextField(
                value = recovery,
                onValueChange = { recovery = it },
                label = { androidx.compose.material3.Text("recovery code") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

class RecentsL4Challenge : TieredChallenge(
    category = "privacy",
    slug = "recents-priv",
    level = Difficulty.INSANE,
    title = "Accessibility Field",
    brief = "The recovery field hides its text visually but exposes it to " +
        "accessibility services — any a11y app reads it verbatim.",
    owaspRefs = listOf("M6", "MASVS-PRIVACY-1", "MASTG-TEST-0x62"),
    hints = listOf(
        "The field masks the characters but the accessibility node carries the plain text.",
        "An a11y service (or uiautomator dump) reads contentDescription/text verbatim.",
        "Secure builds clear the a11y text and mark the node sensitive.",
    ),
    flag = RECENTS_L4,
    learn = LearnContent(
        theory = "Visual masking (password dots) does not change what the " +
            "accessibility layer exposes: an a11y service with the observed permission " +
            "reads the verbatim text from the node.\n\n" +
            "Sensitive nodes must clear their a11y text and mark themselves as " +
            "sensitive to accessibility.",
        mastgRefs = listOf("MASVS-PRIVACY-1", "MASTG-TEST-0x62"),
        vulnerableSnippet = "textField.typeface = MONOSPACE // visual mask only",
        fixSnippet = "node.isPassword = true; node.contentDescription = null",
        takeaway = "If a11y can read it, an a11y app can exfiltrate it.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (secureMode) {
                    "node is password-class; a11y reads nothing"
                } else {
                    "recovery field (masked visually, plain in the a11y tree):"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    // simulating what an accessibility service reads from the node
                    output =
                        if (!secureMode) {
                            "a11y node text: $RECENTS_L4"
                        } else {
                            "a11y node text: (sensitive — excluded)"
                        }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Read field as an a11y service") }
            ChallengeConsole(output)
        }
    }
}
