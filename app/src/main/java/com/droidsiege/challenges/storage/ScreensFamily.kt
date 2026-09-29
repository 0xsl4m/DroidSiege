package com.droidsiege.challenges.storage

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import com.droidsiege.ui.theme.ChallengeSpacing
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream

private const val FLAG_L1 = "DS{storage_screens_L1_6f3d81}"
private const val FLAG_L2 = "DS{storage_screens_L2_a2c954}"
private const val FLAG_L3 = "DS{storage_screens_L3_70e417}"
private const val FLAG_L4 = "DS{storage_screens_L4_f8b6d3}"

private fun setSecureFlag(
    activity: Activity,
    secure: Boolean,
) {
    if (secure) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

object ScreensVault {
    /** L2 helper: renders content into a bitmap and saves it into the app gallery dir. */
    fun saveSnapshot(
        context: Context,
        secureMode: Boolean,
        content: String,
    ): String {
        if (secureMode) {
            return "Snapshot blocked: the screen is flagged secure."
        }
        val bitmap = android.graphics.Bitmap.createBitmap(640, 360, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.rgb(15, 23, 42))
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 30f
            isAntiAlias = true
        }.let { paint ->
            canvas.drawText("Inventory snapshot", 24f, 60f, paint)
            canvas.drawText(content, 24f, 140f, paint)
        }
        val dir = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
            "snapshots",
        ).apply { mkdirs() }
        val out = File(dir, "snapshot_${System.currentTimeMillis()}.jpg")
        java.io.FileOutputStream(out).use { stream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
        }
        return "Snapshot saved to gallery dir: ${out.absolutePath}"
    }
}

class ScreensL1Challenge : TieredChallenge(
    category = "storage",
    slug = "screens",
    level = Difficulty.EASY,
    title = "Recents Thumbnail",
    brief = "This recovery screen shows the secret once per session. Switch away and " +
        "look at your recents switcher.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "MASTG-TEST-0x62"),
    hints = listOf(
        "The system snapshots every visible activity for the recents switcher.",
        "Trigger the secret render, press home, open recents.",
        "A hardened build sets FLAG_SECURE on this window — the thumbnail goes black.",
    ),
    flag = FLAG_L1,
    learn = LearnContent(
        theory = "Android screenshots every visible activity for the recents app " +
            "switcher. The snapshot is system-managed but user-visible — and available " +
            "to screen-recording, and to anyone who picks up the phone and opens " +
            "recents.\n\n" +
            "FLAG_SECURE on the window blanks both the snapshot and " +
            "user screenshots for sensitive screens.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASTG-TEST-0x62"),
        vulnerableSnippet = "// recovery screen renders the secret — no FLAG_SECURE set",
        fixSnippet = "window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)",
        takeaway = "If the screen shows it, the switcher stores it — FLAG_SECURE or nothing.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val activity = LocalContext.current as Activity
        DisposableEffect(secureMode) {
            val wasSecure =
                (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
            setSecureFlag(activity, secureMode)
            onDispose {
                // restore whatever the window had before this screen — no state bleed
                setSecureFlag(activity, wasSecure)
            }
        }
        Column {
            Text(
                text = "Recovery secret (this session):",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = FLAG_L1,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = if (secureMode) {
                    "FLAG_SECURE is on: recents and screenshots are blank here."
                } else {
                    "No FLAG_SECURE: the recents thumbnail captured this screen."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

class ScreensL2Challenge : TieredChallenge(
    category = "storage",
    slug = "screens",
    level = Difficulty.MEDIUM,
    title = "Inventory Snapshot",
    brief = "The inventory tool saves a snapshot of the current session into your " +
        "gallery so you can attach it to reports.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "MASTG-TEST-0x62"),
    hints = listOf(
        "Press the snapshot button, then browse the app gallery dir.",
        "Android/data/com.droidsiege/files/Pictures/snapshots/ holds the JPEG.",
        "The session secret is rendered inside the image — open it or exiftool it.",
    ),
    flag = FLAG_L2,
    learn = LearnContent(
        theory = "Apps that render secrets and then capture their own UI turn a screen " +
            "into a persistent file. Saved snapshots live in gallery directories, sync " +
            "to cloud photos, and outlive the session that produced them.\n\n" +
            "Snapshot features must respect the screen's security posture — a " +
            "FLAG_SECURE screen should refuse programmatic captures too.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASTG-TEST-0x62"),
        vulnerableSnippet = "canvas.drawText(sessionSecret, 24f, 140f, paint)\n" +
            "bitmap.compress(JPEG, 90, galleryOut)",
        fixSnippet = "if (window.flags and FLAG_SECURE != 0) return \"blocked\"\n" +
            "// snapshot features refuse on secure screens",
        takeaway = "A secret rendered into a file is a secret written to storage.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        Column(verticalArrangement = ChallengeSpacing) {
            Text(
                text = FLAG_L2,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
            )
            Button(
                onClick = {
                    ScreensVault.saveSnapshot(context, secureMode, FLAG_L2)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Take inventory snapshot")
            }
            Text(
                text = if (secureMode) {
                    "Hardened: snapshot refused on a secure screen."
                } else {
                    "Snapshot includes the session secret."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

class ScreensL3Challenge : TieredChallenge(
    category = "storage",
    slug = "screens",
    level = Difficulty.HARD,
    title = "Delayed Obscure",
    brief = "The vault shows the unlock secret briefly, then obscures it 'for your " +
        "protection'. The window between those events is the feature.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "MASTG-TEST-0x62"),
    hints = listOf(
        "Press reveal, then screenshot fast — the mask is on a timer.",
        "adb exec-out screencap -p > now.png right after pressing reveal.",
        "A hardened build masks instantly and never renders between states.",
    ),
    flag = FLAG_L3,
    learn = LearnContent(
        theory = "Delayed masking is UX decoration, not control: the secret is fully " +
            "rendered for an observable window, and screenshots (programmatic or " +
            "human) land inside it. Any 'show then hide' pattern loses to a stopwatch.\n\n" +
            "The secure pattern renders the secret only while explicitly unlocked and " +
            "obscures in onPause/onStop — atomically, not on a timer.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASTG-TEST-0x62"),
        vulnerableSnippet = "showSecret()\n" +
            "delay(4_000) // 'for your protection'\n" +
            "maskSecret()",
        fixSnippet = "// render only while unlocked; obscure in onPause:\n" +
            "override fun onPause() { maskSecret() }",
        takeaway = "Timers are not access control — the window is the vulnerability.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var revealed by remember { mutableStateOf(false) }
        LaunchedEffect(revealed) {
            if (revealed && !secureMode) {
                delay(4_000)
                revealed = false
            }
        }
        Column(verticalArrangement = ChallengeSpacing) {
            Text(
                text = if (revealed) FLAG_L3 else "▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
            )
            Button(
                onClick = { revealed = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (revealed) "Re-reveal" else "Reveal unlock secret")
            }
            Text(
                text = if (secureMode) {
                    "Hardened: reveal is atomic and obscures on background, not on a timer."
                } else {
                    "Secret auto-masks after 4 seconds — catch it."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

class ScreensL4Challenge : TieredChallenge(
    category = "storage",
    slug = "screens",
    level = Difficulty.INSANE,
    title = "Partner Transition",
    brief = "This window is FLAG_SECURE — the team verified it. There is one transition " +
        "where the partner preview momentarily re-renders the secret. Snap it.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "MASTG-TEST-0x62"),
    hints = listOf(
        "Start the partner preview, then screenshot during the 2-second window.",
        "adb exec-out screencap -p in a loop while pressing the button is a valid approach.",
        "Secure flags must be lifecycle-consistent: a single cleared transition leaks everything.",
    ),
    flag = FLAG_L4,
    learn = LearnContent(
        theory = "Security posture applied most of the time is not applied. A single " +
            "transition that clears FLAG_SECURE — a 'preview', a share sheet, a custom " +
            "tab — reopens the capture window exactly when the secret is on screen.\n\n" +
            "Secure state must be part of the screen's lifecycle contract: set on enter, " +
            "held through every sub-state, cleared only on real exit.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASTG-TEST-0x62"),
        vulnerableSnippet = "// partner preview:\n" +
            "window.clearFlags(FLAG_SECURE); render(secret)\n" +
            "delay(2_000); window.addFlags(FLAG_SECURE)",
        fixSnippet = "// the secret never renders outside the secure window:\n" +
            "partnerPreview.render(maskedSecret)",
        takeaway = "One unsecured transition is one screenshot away from the headline.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val activity = LocalContext.current as Activity
        var previewing by remember { mutableStateOf(false) }
        DisposableEffect(Unit) {
            val wasSecure =
                (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
            setSecureFlag(activity, true)
            onDispose {
                setSecureFlag(activity, wasSecure)
            }
        }
        LaunchedEffect(previewing) {
            if (previewing && !secureMode) {
                setSecureFlag(activity, false)
                delay(2_000)
                setSecureFlag(activity, true)
                previewing = false
            }
        }
        Column(verticalArrangement = ChallengeSpacing) {
            Text(
                text = if (previewing && !secureMode) FLAG_L4 else "🔒 flag secure window",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
            )
            Button(
                onClick = { previewing = true },
                enabled = !previewing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open partner preview")
            }
            Text(
                text = if (secureMode) {
                    "Hardened: the preview renders masked content; FLAG_SECURE never drops."
                } else {
                    "FLAG_SECURE drops for 2 seconds during the preview."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
