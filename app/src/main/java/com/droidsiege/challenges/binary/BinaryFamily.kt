package com.droidsiege.challenges.binary

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidsiege.challenges.common.ChallengeConsole
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import java.io.File

private const val ROOT_L1 = "DS{binary_rootdetect_L1_91e3a7}"
private const val ROOT_L2 = "DS{binary_rootdetect_L2_46b8d2}"
private const val ROOT_L3 = "DS{binary_rootdetect_L3_c0f159}"
private const val FRIDA_L1 = "DS{binary_fridadetect_L1_82c4a9}"
private const val FRIDA_L2 = "DS{binary_fridadetect_L2_5f71e3}"
private const val FRIDA_L3 = "DS{binary_fridadetect_L3_d90b26}"

object RootDetectLab {
    fun naiveRootCheck(): Boolean = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su").any { File(it).exists() }

    fun emulatorCheck(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.MODEL.contains("emulator", ignoreCase = true) ||
            Build.MODEL.contains("sdk_gphone", ignoreCase = true)

    fun debuggerCheck(): Boolean = android.os.Debug.isDebuggerConnected()
}

class RootDetectL1Challenge : TieredChallenge(
    category = "binary",
    slug = "rootdetect",
    level = Difficulty.EASY,
    title = "Su File Check",
    brief = "The vault refuses to open on rooted devices — if the naive check is to be believed. Defeat it.",
    owaspRefs = listOf("M7", "MASVS-RESILIENCE-1", "MASTG-TEST-0x53"),
    hints = listOf(
        "The check tests three fixed paths for a su binary.",
        "Hide the file (Magisk deny list) — or note the emulator path check and spoof.",
        "Even a defeated client check teaches the lesson: never trust the client.",
    ),
    flag = ROOT_L1,
    learn = LearnContent(
        theory = "A file-existence root check is the weakest form of integrity " +
            "signaling: Magisk hide, mount namespaces and custom ROM paths defeat it " +
            "in seconds, and the check itself is visible in the decompiled code.",
        mastgRefs = listOf("MASVS-RESILIENCE-1", "MASTG-TEST-0x53"),
        vulnerableSnippet = "listOf(\"/system/bin/su\", …).any { File(it).exists() }",
        fixSnippet = "// layered checks + Play Integrity — and STILL verify server-side",
        takeaway = "A client root check is a speed bump the attacker owns the road for.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val rooted = RootDetectLab.naiveRootCheck()
                output = if (!secureMode) {
                    if (!rooted) "device not rooted — vault code: $ROOT_L1" else "rooted — refusing"
                } else {
                    "hardened: layered checks + attestation; the file check alone never decides"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Open vault (root check)") }
            ChallengeConsole(output)
        }
    }
}

class RootDetectL2Challenge : TieredChallenge(
    category = "binary",
    slug = "rootdetect",
    level = Difficulty.MEDIUM,
    title = "Emulator Props",
    brief = "The vault refuses emulators by reading Build properties. Spoof them.",
    owaspRefs = listOf("M7", "MASVS-RESILIENCE-1", "MASTG-TEST-0x53"),
    hints = listOf(
        "The check reads Build.FINGERPRINT and Build.MODEL.",
        "On this emulator the model already contains 'sdk_gphone' — the check fires.",
        "Play Integrity is the platform answer.",
    ),
    flag = ROOT_L2,
    learn = LearnContent(
        theory = "Build-property emulator checks are fingerprintable and spoofable: " +
            "setprop, Magisk modules and patched images rewrite them. Like all client " +
            "checks they only raise the bar by minutes.",
        mastgRefs = listOf("MASVS-RESILIENCE-1", "MASTG-TEST-0x53"),
        vulnerableSnippet = "Build.FINGERPRINT.startsWith(\"generic\")",
        fixSnippet = "// Play Integrity API standardIntegrity verdict",
        takeaway = "The device tells you what it is — and lies on command.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val emu = RootDetectLab.emulatorCheck()
                output = if (!secureMode) {
                    if (!emu) "physical device — vault code: $ROOT_L2" else "emulator — refusing"
                } else {
                    "hardened: Play Integrity verdict required"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Open vault (emulator check)") }
            ChallengeConsole(output)
        }
    }
}

class RootDetectL3Challenge : TieredChallenge(
    category = "binary",
    slug = "rootdetect",
    level = Difficulty.HARD,
    title = "Debugger Check",
    brief = "This tier also detects an attached debugger. Bypass with tooling.",
    owaspRefs = listOf("M7", "MASVS-RESILIENCE-2", "MASTG-TEST-0x53"),
    hints = listOf(
        "Debug.isDebuggerConnected plus a ptrace self-check.",
        "objection/frida patch both with one line.",
        "Server-side attestation replaces client trust.",
    ),
    flag = ROOT_L3,
    learn = LearnContent(
        theory = "Debugger detection (isDebuggerConnected, ptrace self-attach) raises " +
            "the cost of dynamic analysis slightly — frida/objection patch both in one call.",
        mastgRefs = listOf("MASVS-RESILIENCE-2", "MASTG-TEST-0x53"),
        vulnerableSnippet = "if (Debug.isDebuggerConnected()) refuse()",
        fixSnippet = "// Play Integrity + server-side session attestation",
        takeaway = "Anti-debug is a tripwire, not a wall.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val debugged = RootDetectLab.debuggerCheck()
                output = if (!secureMode) {
                    if (!debugged) "no debugger — vault code: $ROOT_L3" else "debugger attached — refusing"
                } else {
                    "hardened: server-side attestation"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Open vault (debugger check)") }
            ChallengeConsole(output)
        }
    }
}

class FridaL1Challenge : TieredChallenge(
    category = "binary",
    slug = "fridadetect",
    level = Difficulty.EASY,
    title = "Frida Port Check",
    brief = "The vault scans for the default frida-server port. Rename or relocate the server and walk in.",
    owaspRefs = listOf("M7", "MASVS-RESILIENCE-2", "MASTG-TEST-0x56"),
    hints = listOf(
        "The check connects to the default frida port 27042.",
        "frida-server -l 0.0.0.0:1337 (any port) defeats the port probe.",
        "Hardened: attestation, not port probes.",
    ),
    flag = FRIDA_L1,
    learn = LearnContent(
        theory = "Probing the well-known frida port catches only the default " +
            "configuration. Renaming the binary or moving the port defeats the probe outright.",
        mastgRefs = listOf("MASVS-RESILIENCE-2", "MASTG-TEST-0x56"),
        vulnerableSnippet = "Socket(\"127.0.0.1\", 27042).connect() // default port",
        fixSnippet = "// Play Integrity + server-side session binding",
        takeaway = "Default-port probes catch only default setups.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val detected = runCatching {
                    java.net.Socket("127.0.0.1", 27042).use { it.isConnected }
                }.getOrDefault(false)
                output = if (!secureMode) {
                    if (!detected) "frida not on default port — vault code: $FRIDA_L1" else "frida detected — refusing"
                } else {
                    "hardened: attestation required"
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Open vault (frida port check)")
            }
            ChallengeConsole(output)
        }
    }
}

class FridaL2Challenge : TieredChallenge(
    category = "binary",
    slug = "fridadetect",
    level = Difficulty.MEDIUM,
    title = "Loaded Lib Scan",
    brief = "This tier scans the loaded libraries for frida artifacts. Hide them and walk through.",
    owaspRefs = listOf("M7", "MASVS-RESILIENCE-2", "MASTG-TEST-0x56"),
    hints = listOf(
        "The scan greps /proc/self/maps for frida strings.",
        "Frida's gadget/server can rename its mapped libs.",
        "Hardened: attestation, not map greps.",
    ),
    flag = FRIDA_L2,
    learn = LearnContent(
        theory = "Scanning /proc/self/maps for 'frida' catches stock setups only. " +
            "Renamed gadget builds and Magisk-hide equivalents clear the scan.",
        mastgRefs = listOf("MASVS-RESILIENCE-2", "MASTG-TEST-0x56"),
        vulnerableSnippet = "File(\"/proc/self/maps\").readText().contains(\"frida\")",
        fixSnippet = "// remote attestation is the trust anchor",
        takeaway = "Map scans catch defaults, renamed builds walk past.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val maps = runCatching { File("/proc/self/maps").readText() }.getOrDefault("")
                val detected = maps.contains("frida", ignoreCase = true)
                output = if (!secureMode) {
                    if (!detected) "no frida in maps — vault code: $FRIDA_L2" else "frida detected — refusing"
                } else {
                    "hardened: attestation required"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Open vault (lib scan)") }
            ChallengeConsole(output)
        }
    }
}

class FridaL3Challenge : TieredChallenge(
    category = "binary",
    slug = "fridadetect",
    level = Difficulty.HARD,
    title = "Native Anti-Hook",
    brief = "The native bridge verifies its own memory integrity before " +
        "answering. Patch the native check (lands with the Phase 5 NDK build).",
    owaspRefs = listOf("M7", "MASVS-RESILIENCE-3", "MASTG-TEST-0x56"),
    hints = listOf(
        "The bridge computes a self-hash and compares to a baked constant.",
        "Frida's Memory.patch or an Interceptor on the check function defeats it.",
        "Hardened: the check adds nothing against a determined attacker; attestation wins.",
    ),
    flag = FRIDA_L3,
    learn = LearnContent(
        theory = "Native self-integrity checks raise the bar for static patches but " +
            "fall to runtime hooks that skip the comparison entirely.",
        mastgRefs = listOf("MASVS-RESILIENCE-3", "MASTG-TEST-0x56"),
        vulnerableSnippet = "if (self_hash != BAKED) refuse();",
        fixSnippet = "// remote attestation; the client is never the trust anchor",
        takeaway = "A self-check runs inside the process it is guarding.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                output = if (!secureMode) {
                    "bridge integrity check present (native tier lands with the NDK build) — vault code: $FRIDA_L3"
                } else {
                    "hardened: server-side attestation"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Open vault (integrity check)") }
            ChallengeConsole(output)
        }
    }
}
