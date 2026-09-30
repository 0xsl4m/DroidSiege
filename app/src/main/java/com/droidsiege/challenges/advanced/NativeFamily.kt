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

    external fun nativeLog(fmt: String)

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
