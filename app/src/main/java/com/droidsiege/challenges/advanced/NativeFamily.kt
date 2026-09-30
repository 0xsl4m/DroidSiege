package com.droidsiege.challenges.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import com.droidsiege.challenges.common.ChallengeConsole
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent

/** JNI bridge to the deliberately vulnerable native vault. */
object NativeVault {
    init {
        System.loadLibrary("droidsiege-vault")
    }

    external fun nativeCheck(input: ByteArray): Boolean

    external fun nativeParse(
        input: ByteArray,
        declaredLen: Int,
    ): ByteArray

    external fun nativeLog(
        fmt: String,
        secret: String,
    )

    external fun hiddenPrintFlag(): String
}

private const val NATIVE_FLAG_L1 = "DS{advanced_native_L1_64f3b7}"

class NativeL1Challenge : TieredChallenge(
    category = "advanced",
    slug = "native",
    level = Difficulty.EASY,
    title = "Magic Value Check",
    brief = "The native vault returns the flag only when a magic value is passed. " +
        "Reverse the .so to find it.",
    owaspRefs = listOf("M7", "MASVS-RESILIENCE-3", "MASTG-TEST-0x53"),
    hints = listOf(
        "objection memory dump, or read the .so with strings — the magic is plaintext.",
        "The magic is opensesame (10 bytes) — pass it as the input byte array.",
        "Secure builds embed no secret and do server-side checks.",
    ),
    flag = NATIVE_FLAG_L1,
    learn = LearnContent(
        theory = "A native function with a magic-value comparison ships the answer " +
            "in the binary: strings, objection or a quick Frida hook print it.\n\n" +
            "Never embed secrets in native code — the binary is readable.",
        mastgRefs = listOf("MASVS-RESILIENCE-3", "MASTG-TEST-0x53"),
        vulnerableSnippet = "if (input == MAGIC) return flag; // MAGIC in .rodata",
        fixSnippet = "// server-side check; the native side holds no secret",
        takeaway = "Native code is readable code — strings are plaintext.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (secureMode) {
                    "hardened: no secret embedded in the native layer"
                } else {
                    "nativeCheck(input) — magic value required"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {
                val result = runCatching { NativeVault.nativeCheck("opensesame".toByteArray()) }
                output = result.fold(
                    { ok -> if (ok) "native check passed:\n$NATIVE_FLAG_L1" else "native check failed" },
                    { "native error: ${it.message}" },
                )
            }, modifier = Modifier.fillMaxWidth()) { Text("Try magic value") }
            ChallengeConsole(output)
        }
    }
}

class NativeL2Challenge : TieredChallenge(
    category = "advanced",
    slug = "native",
    level = Difficulty.MEDIUM,
    title = "Pool Over-read",
    brief = "The native parse routine trusts a declared length and over-reads the " +
        "pool past your record. Declare a longer length.",
    owaspRefs = listOf("M7", "MASVS-RESILIENCE-3", "MASTG-TEST-0x53"),
    hints = listOf(
        "nativeParse(input, declaredLen) copies declaredLen bytes from the pool.",
        "Declare a length longer than your input — the copy walks into the secret.",
        "Hardened: the declared length is clamped to the input.",
    ),
    flag = "DS{advanced_native_L2_09d5c8}",
    learn = LearnContent(
        theory = "The parser trusts a declared length and copies from a pool whose " +
            "secret region sits after the user record. An over-declared length walks " +
            "the copy straight into the secret — the classic over-read.\n\n" +
            "Bounds-check every declared length against the actual buffer.",
        mastgRefs = listOf("MASVS-RESILIENCE-3", "MASTG-TEST-0x53"),
        vulnerableSnippet = "copy(out, input, declaredLen) // walks past the record",
        fixSnippet = "val len = declaredLen.coerceAtMost(input.size)",
        takeaway = "The pool remembers what the record left out.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var input by rememberSaveable { mutableStateOf("record=abc") }
        var lenText by rememberSaveable { mutableStateOf("12") }
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("input") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = lenText,
                onValueChange = { lenText = it },
                label = { Text("declared length") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val bytes = NativeVault.nativeParse(
                        input.toByteArray(),
                        lenText.toIntOrNull() ?: input.length,
                    )
                    output = bytes.toString(Charsets.UTF_8)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Parse")
            }
            ChallengeConsole(output)
        }
    }
}

class NativeL3Challenge : TieredChallenge(
    category = "advanced",
    slug = "native",
    level = Difficulty.HARD,
    title = "Format String",
    brief = "The native log call uses the user string as the format. Leak the " +
        "adjacent secret with %s.",
    owaspRefs = listOf("M7", "MASVS-RESILIENCE-3", "MASTG-TEST-0x53"),
    hints = listOf(
        "The nativeLog(fmt, secret) call uses fmt AS the format.",
        "Pass %s as the format to print the secret argument.",
        "Hardened: the format is fixed; the user text is an argument.",
    ),
    flag = "DS{advanced_native_L3_72a1e4}",
    learn = LearnContent(
        theory = "Format-string vulnerabilities in native code are as dangerous as " +
            "they are in C: %x leaks stack values and %s dereferences arbitrary " +
            "pointers. When the user controls the format, the attacker controls " +
            "what is printed.",
        mastgRefs = listOf("MASVS-RESILIENCE-3", "MASTG-TEST-0x53"),
        vulnerableSnippet = "__android_log_print(INFO, TAG, userFmt, secret)",
        fixSnippet = "__android_log_print(INFO, TAG, \"%s\", userFmt)",
        takeaway = "User-controlled format strings are code injection.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (secureMode) {
                    "hardened: the format is fixed; user text is an argument"
                } else {
                    "nativeLog(fmt, secret) — the secret is the vararg"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {
                output = if (!secureMode) {
                    runCatching { NativeVault.nativeLog("%s", "DS{advanced_native_L3_72a1e4}") }
                    "check logcat -s SiegeNative — the secret was printed via %s"
                } else {
                    "hardened: fixed format — %s is treated as literal text"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Trigger native log") }
            ChallengeConsole(output)
        }
    }
}

class NativeL4Challenge : TieredChallenge(
    category = "advanced",
    slug = "native",
    level = Difficulty.INSANE,
    title = "Hidden Flag Function",
    brief = "The .so exports hiddenPrintFlag — a function with no legitimate caller. " +
        "Reach it by controlling a function pointer via the over-read.",
    owaspRefs = listOf("M7", "MASVS-RESILIENCE-3", "MASTG-TEST-0x53"),
    hints = listOf(
        "hiddenPrintFlag exists in the symbol table — check with nm or objdump.",
        "Chain the L2 over-read to corrupt a function pointer to reach it.",
        "Hardened builds don't export hidden functions or embed secrets.",
    ),
    flag = "DS{advanced_native_L4_3b96f2}",
    learn = LearnContent(
        theory = "Exported functions that reveal secrets are callable by anyone who " +
            "can control a function pointer — the L2 over-read provides exactly that " +
            "control. The export exists in the symbol table for anyone to find.\n\n" +
            "Remove the secret function and use server-side checks.",
        mastgRefs = listOf("MASVS-RESILIENCE-3", "MASTG-TEST-0x53"),
        vulnerableSnippet = "extern \"C\" jstring hiddenPrintFlag(JNIEnv*, jobject)",
        fixSnippet = "// remove the function; no secret in native code",
        takeaway = "Every exported symbol is an invitation to the attacker.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (secureMode) {
                    "hardened: no hidden functions, no embedded secrets"
                } else {
                    "hiddenPrintFlag is in the symbol table — reach it via the L2 over-read"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {
                output = if (!secureMode) {
                    runCatching { NativeVault.hiddenPrintFlag() }
                        .getOrElse { "call hiddenPrintFlag via JNI or Frida" }
                } else {
                    "hardened: function removed"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Try direct JNI call") }
            ChallengeConsole(output)
        }
    }
}
