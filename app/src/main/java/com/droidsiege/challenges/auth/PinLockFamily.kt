package com.droidsiege.challenges.auth

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import java.security.MessageDigest

private const val PIN_PREFS = "siege_pin_prefs"

private fun pinHash(context: Context): String? =
    context.getSharedPreferences(PIN_PREFS, Context.MODE_PRIVATE).getString("pin_hash", null)

private fun storePin(
    context: Context,
    pin: String,
) {
    context.getSharedPreferences(PIN_PREFS, Context.MODE_PRIVATE)
        .edit().putString("pin_hash", md5(pin)).apply()
}

private fun md5(input: String): String =
    MessageDigest.getInstance("MD5").digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }

class PinLockL1Challenge : TieredChallenge(
    category = "auth",
    slug = "pinlock",
    level = Difficulty.EASY,
    title = "Four Digits",
    brief = "The vault unlocks with a 4-digit PIN compared client-side, with no " +
        "lockout. The brute action is built in for your convenience.",
    owaspRefs = listOf("M3", "MASVS-AUTH-1", "MASTG-TEST-0x53"),
    hints = listOf(
        "10,000 combinations, no rate limit — the brute action sweeps them all.",
        "The insecure comparison is plaintext: the correct PIN prints on match.",
        "Secure builds rate-limit and verify server-side.",
    ),
    flag = AuthFlags.PIN_L1,
    learn = LearnContent(
        theory = "A 4-digit PIN over an unrestricted client-side check is a one-" +
            "keystroke wait: the whole space is 10,000 and nothing throttles the " +
            "attempts.\n\n" +
            "PIN entry needs server-side verification, exponential lockout, and " +
            "secure hardware-backed storage for the factor.",
        mastgRefs = listOf("MASVS-AUTH-1", "MASTG-TEST-0x53"),
        vulnerableSnippet = "if (entered == storedPin) unlock() // no lockout",
        fixSnippet = "// server-verified factor + exponential backoff per attempt",
        takeaway = "Unlimited offline guesses solve any short PIN.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        storePin(context, "4271")
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (secureMode) {
                    "Hardened: server-verified factor with exponential lockout; the " +
                        "insecure brute sweep is disabled."
                } else {
                    "no lockout — sweep 0000..9999 from the app itself"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AuthButton("Brute force the PIN") {
                if (secureMode) {
                    output = "hardened: brute sweep disabled"
                    return@AuthButton
                }
                for (candidate in 0..9999) {
                    val pin = "%04d".format(candidate)
                    if (pinHash(context) == md5(pin)) {
                        output = "PIN found: $pin\nvault code:\n${AuthFlags.PIN_L1}"
                        return@AuthButton
                    }
                }
                output = "no PIN matched"
            }
            AuthConsole(output)
        }
    }
}

class PinLockL2Challenge : TieredChallenge(
    category = "auth",
    slug = "pinlock",
    level = Difficulty.MEDIUM,
    title = "Unsalted MD5",
    brief = "The PIN upgrade stores an unsalted MD5 hash locally. A small wordlist " +
        "and the hash are all you need.",
    owaspRefs = listOf("M3", "MASVS-AUTH-1", "M9", "MASTG-TEST-0x53"),
    hints = listOf(
        "The hash is in siege_pin_prefs.xml: pin_hash (unsalted MD5).",
        "4-digit PINs brute in a second even as MD5 — no wordlist needed.",
        "Secure builds use a Keystore-backed factor with lockout.",
    ),
    flag = AuthFlags.PIN_L2,
    learn = LearnContent(
        theory = "Unsalted MD5 over a 4-digit space is precomputation-friendly: the " +
            "entire table is 10,000 entries and recomputes in milliseconds. Hashing " +
            "does not help when the input space is tiny and the hash is fast.\n\n" +
            "Factors belong in secure hardware; anything client-stored uses a slow " +
            "KDF with a random salt at minimum.",
        mastgRefs = listOf("MASVS-AUTH-1", "MASTG-TEST-0x53"),
        vulnerableSnippet = "prefs.putString(\"pin_hash\", md5(pin)) // unsalted, fast",
        fixSnippet = "// hardware-backed factor (BiometricPrompt + Keystore)",
        takeaway = "Fast hashes over small spaces are plaintext with extra steps.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        storePin(context, "9876")
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "stored pin_hash: ${pinHash(context)}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
            AuthButton("Brute the MD5") {
                if (secureMode) {
                    output = "hardened: Keystore-backed factor"
                    return@AuthButton
                }
                for (candidate in 0..9999) {
                    val pin = "%04d".format(candidate)
                    if (pinHash(context) == md5(pin)) {
                        output = "PIN found: $pin\nvault code:\n${AuthFlags.PIN_L2}"
                        return@AuthButton
                    }
                }
                output = "no PIN matched"
            }
            AuthConsole(output)
        }
    }
}

class PinLockL3Challenge : TieredChallenge(
    category = "auth",
    slug = "pinlock",
    level = Difficulty.HARD,
    title = "Boolean Biometrics",
    brief = "Biometric success is a callback boolean in this build — no crypto " +
        "binding, nothing for the hardware to attest. Press authenticate.",
    owaspRefs = listOf("M3", "MASVS-AUTH-2", "MASTG-TEST-0x54"),
    hints = listOf(
        "The 'biometric' success is a boolean flag — hook or press the sim button.",
        "A real binding requires CryptoObject: the key refuses to decrypt without a " +
            "genuine fingerprint.",
        "Secure builds decrypt through a BiometricPrompt.CryptoObject-gated key.",
    ),
    flag = AuthFlags.PIN_L3,
    learn = LearnContent(
        theory = "BiometricPrompt answers 'the enrolled finger matched' — not 'the " +
            "user is authorized'. Using the success callback as an unlock signal " +
            "reduces the hardware factor to a boolean an attacker sets.\n\n" +
            "Gate a CryptoObject-bound Keystore key so decryption physically requires " +
            "the biometric event.",
        mastgRefs = listOf("MASVS-AUTH-2", "MASTG-TEST-0x54"),
        vulnerableSnippet = "onAuthenticationSucceeded { unlockVault() } // boolean only",
        fixSnippet = "BiometricPrompt.authenticate(CryptoObject(keystoreCipher))\n" +
            "// vault decrypts only inside onAuthenticationSucceeded",
        takeaway = "Biometrics without crypto binding are a UI animation.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var unlocked by remember { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (secureMode) {
                    "Hardened: the vault decrypts through a CryptoObject-bound " +
                        "Keystore key — no success callback to fake."
                } else {
                    "insecure: 'authenticate' just sets the success boolean"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AuthButton("Authenticate (biometric sim)") { unlocked = !secureMode }
            if (unlocked) {
                AuthConsole("vault code:\n${AuthFlags.PIN_L3}")
            } else if (secureMode) {
                AuthConsole(
                    "hardened: the vault decrypts through a CryptoObject-bound " +
                        "Keystore key — the success boolean alone unlocks nothing",
                )
            }
        }
    }
}

class PinLockL4Challenge : TieredChallenge(
    category = "auth",
    slug = "pinlock",
    level = Difficulty.INSANE,
    title = "Recovery Fallback",
    brief = "The vault key is properly biometric-gated — but a 'recovery' path " +
        "decrypts it with a static key when the sensor is 'unavailable'.",
    owaspRefs = listOf("M3", "MASVS-AUTH-2", "M9", "MASTG-TEST-0x54"),
    hints = listOf(
        "Press 'recover with fallback' — the static key decrypts the blob.",
        "The fallback is the whole finding: one path bypasses the biometric gate.",
        "Secure builds have no fallback: the key only ever opens through the prompt.",
    ),
    flag = AuthFlags.PIN_L4,
    learn = LearnContent(
        theory = "Recovery fallbacks are the classic bypass: the strong path uses " +
            "the biometric-bound key, and the 'sensor unavailable' path decrypts with " +
            "a static key shipped in the app. The attacker picks the weak path every " +
            "time.\n\n" +
            "One key, one gate: if the biometric path exists, no alternative decrypt " +
            "path exists.",
        mastgRefs = listOf("MASVS-AUTH-2", "MASTG-TEST-0x54"),
        vulnerableSnippet = "if (sensorUnavailable) {\n" +
            "    blob.decrypt(STATIC_FALLBACK_KEY) // the bypass\n" +
            "}",
        fixSnippet = "// no fallback: the key opens only through the biometric prompt",
        takeaway = "A strong gate with a weak alternative is a weak gate.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthButton("Recover with fallback") {
                output =
                    if (secureMode) {
                        "hardened: no fallback path exists"
                    } else {
                        "fallback unlocked:\n${AuthFlags.PIN_L4}"
                    }
            }
            AuthConsole(output)
        }
    }
}
