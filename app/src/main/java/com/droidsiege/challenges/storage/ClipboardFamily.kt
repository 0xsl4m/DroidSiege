package com.droidsiege.challenges.storage

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.droidsiege.challenges.common.ActionChallengeScreen
import com.droidsiege.challenges.common.KitAction
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import java.io.File

private const val FLAG_L1 = "DS{storage_clipboard_L1_5e8b24}"
private const val FLAG_L2 = "DS{storage_clipboard_L2_c7f190}"
private const val FLAG_L3 = "DS{storage_clipboard_L3_32ad65}"
private const val FLAG_L4 = "DS{storage_clipboard_L4_b94e08}"

object ClipboardVault {
    private fun clipboard(context: Context): ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun copyRecoveryCode(
        context: Context,
        secureMode: Boolean,
    ): String {
        if (!secureMode) {
            clipboard(context).setPrimaryClip(ClipData.newPlainText("recovery code", FLAG_L1))
        } else {
            val clip = ClipData.newPlainText("recovery code", "[copied securely — expires now]")
            clip.description.extras = android.os.PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
            clipboard(context).setPrimaryClip(clip)
            clipboard(context).clearPrimaryClip()
        }
        return if (!secureMode) {
            "Recovery code copied to the clipboard."
        } else {
            "Copy marked sensitive and cleared immediately."
        }
    }

    fun readClipboard(context: Context): String {
        val clip = clipboard(context).primaryClip
        val text = clip?.getItemAt(0)?.text?.toString() ?: "(clipboard empty)"
        return "Clipboard now holds: $text"
    }

    fun couponField(secureMode: Boolean): String? = if (!secureMode) FLAG_L2 else null

    fun pushClipboardHistory(
        context: Context,
        secureMode: Boolean,
    ): String {
        val history = context.getSharedPreferences("siege_clip_history", Context.MODE_PRIVATE)
        if (!secureMode) {
            history.edit()
                .putString("last_clip", FLAG_L3)
                .putStringSet("history", setOf(FLAG_L3, "https://siege.app/promo"))
                .commit()
        } else {
            history.edit().clear().commit()
        }
        return if (!secureMode) {
            "Coupon copied — the clipboard history keeps it after you switch apps."
        } else {
            "Sensitive clip marked and history cleared on background."
        }
    }

    fun cacheKeyboardInput(
        context: Context,
        secureMode: Boolean,
    ): String {
        val cache = File(context.filesDir, "keyboard_suggestions.cache")
        cache.writeText(
            if (!secureMode) {
                "note_draft=$FLAG_L4\nautofill_hint=note\n"
            } else {
                "note_draft=[noAutoFill field]\nautofill_hint=no\n"
            },
        )
        return if (!secureMode) {
            "Draft cached for the keyboard/autofill layer: ${cache.absolutePath}"
        } else {
            "Field excluded from autofill and caching: ${cache.absolutePath}"
        }
    }
}

class ClipboardL1Challenge : TieredChallenge(
    category = "storage",
    slug = "clipboard",
    level = Difficulty.EASY,
    title = "Copy Recovery Code",
    brief = "Support asks users to copy their recovery code so they can paste it into " +
        "the ticket form. The clipboard is a shared system service.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "MASTG-TEST-0x66"),
    hints = listOf(
        "The clipboard is readable from other contexts — even the same screen can read it back.",
        "Press copy, then press the inspect button.",
        "On a real device, any focused app (or the keyboard's clipboard manager) sees it too.",
    ),
    flag = FLAG_L1,
    learn = LearnContent(
        theory = "The clipboard is an OS-level handoff point: whatever the app copies, " +
            "every later focused context can paste — keyboards, clipboard managers, and " +
            "the next app the user opens. Secrets pasted into ticket forms live on in " +
            "clipboard history.\n\n" +
            "Either never copy secrets, or mark them sensitive and clear promptly.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASTG-TEST-0x66"),
        vulnerableSnippet = "clipboard.setPrimaryClip(ClipData.newPlainText(\"code\", recoveryCode))",
        fixSnippet = "clip.description.extras = PersistableBundle().apply {\n" +
            "    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)\n" +
            "}\nclipboard.setPrimaryClip(clip); clipboard.clearPrimaryClip()",
        takeaway = "The clipboard is a waiting room everyone can enter — do not leave secrets there.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Copy recovery code") { ctx, secure -> ClipboardVault.copyRecoveryCode(ctx, secure) },
                KitAction("Inspect clipboard (other context)") { ctx, _ -> ClipboardVault.readClipboard(ctx) },
            ),
        )
    }
}

class ClipboardL2Challenge : TieredChallenge(
    category = "storage",
    slug = "clipboard",
    level = Difficulty.MEDIUM,
    title = "Coupon Field",
    brief = "Paste your loyalty coupon below — the field supports copy, select-all and " +
        "autofill for convenience.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "MASTG-TEST-0x66"),
    hints = listOf(
        "The field is prefilled with the store's most valuable coupon.",
        "Long-press is all it takes — the field is a normal text field.",
        "Hardened builds turn it into a password-style field that refuses copying.",
    ),
    flag = FLAG_L2,
    learn = LearnContent(
        theory = "Sensitive inputs must opt out of the conveniences text fields offer: " +
            "select, copy, and clipboard preview. A coupon code pasted in a plain " +
            "EditText is two long-presses from the clipboard of any app the user opens " +
            "next.\n\n" +
            "Password-style input types and copy suppression exist exactly for fields " +
            "like this one.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASTG-TEST-0x66"),
        vulnerableSnippet = "TextField(value = coupon, onValueChange = {}) // plain, copyable",
        fixSnippet = "TextField(\n" +
            "    value = coupon,\n" +
            "    visualTransformation = PasswordVisualTransformation(),\n" +
            "    // + longClick suppressed / clipboard hints disabled\n" +
            ")",
        takeaway = "Convenience features on sensitive fields are leak features.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val coupon = remember(secureMode) { ClipboardVault.couponField(secureMode) ?: "••••••••••••" }
        var text by rememberSaveable { mutableStateOf("") }
        Column {
            OutlinedTextField(
                value = if (secureMode) coupon else (text.ifEmpty { coupon }),
                onValueChange = { if (!secureMode) text = it },
                label = { Text("Loyalty coupon") },
                visualTransformation =
                    if (secureMode) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = if (secureMode) {
                    "Hardened: field refuses copy/select and is masked."
                } else {
                    "Select and copy the coupon — nothing stops you."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

class ClipboardL3Challenge : TieredChallenge(
    category = "storage",
    slug = "clipboard",
    level = Difficulty.HARD,
    title = "Clipboard History",
    brief = "The wallet keeps its own clipboard history so power users can juggle codes. " +
        "History outlives the copy by design.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "MASTG-TEST-0x66"),
    hints = listOf(
        "The history feature persists its entries — find where.",
        "shared_prefs/siege_clip_history.xml holds the set of recent clips.",
        "Copy the coupon, switch apps, come back — it is still there, on disk and in the manager.",
    ),
    flag = FLAG_L3,
    learn = LearnContent(
        theory = "Clipboard history features turn a transient handoff into durable " +
            "storage, often syncing it across devices. A secret that entered the " +
            "clipboard once is now a record with a lifetime measured in days.\n\n" +
            "Sensitive clips must be marked as sensitive (kept out of history), and " +
            "cleared when the app loses focus.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASTG-TEST-0x66"),
        vulnerableSnippet = "history.edit().putString(\"last_clip\", coupon).commit()",
        fixSnippet = "clip.description.extras = PersistableBundle().apply {\n" +
            "    putBoolean(EXTRA_IS_SENSITIVE, true) // excluded from history\n" +
            "}\n// + clearPrimaryClip() in onStop()",
        takeaway = "History features resurrect deleted secrets — keep sensitive data out of them.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Copy store coupon") { ctx, secure -> ClipboardVault.pushClipboardHistory(ctx, secure) },
            ),
        )
    }
}

class ClipboardL4Challenge : TieredChallenge(
    category = "storage",
    slug = "clipboard",
    level = Difficulty.INSANE,
    title = "Keyboard Cache",
    brief = "Users dictate their safe-deposit note into this field. The field is a " +
        "normal note field — the keyboard and autofill layers see everything it sees.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "M6", "MASTG-TEST-0x66"),
    hints = listOf(
        "Keyboards and autofill keep their own suggestion caches keyed by field type.",
        "The app simulates that cache in filesDir/keyboard_suggestions.cache.",
        "A non-password field with autofill enabled is what lands the secret in the cache.",
    ),
    flag = FLAG_L4,
    learn = LearnContent(
        theory = "Input reaches further than the app: the keyboard's suggestion engine, " +
            "autofill services and clipboard previews all observe the field. A secret " +
            "typed into a plain text field is cached by layers the app does not control " +
            "and cannot wipe.\n\n" +
            "Sensitive input must use password semantics (no suggestions, no " +
            "autofill), or opt out of autofill explicitly.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASVS-PRIVACY-1", "MASTG-TEST-0x66"),
        vulnerableSnippet = "TextField(value = note, onValueChange = { cache(it) }) // plain input type",
        fixSnippet = "TextField(\n" +
            "    value = note,\n" +
            "    visualTransformation = PasswordVisualTransformation(),\n" +
            "    // + importantForAutofill = no, keyboard type = noSuggestions\n" +
            ")",
        takeaway = "Input fields have invisible observers — password semantics are the opt-out.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Save safe-deposit note") { ctx, secure -> ClipboardVault.cacheKeyboardInput(ctx, secure) },
            ),
        )
    }
}
