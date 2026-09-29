package com.droidsiege.challenges.storage

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import com.droidsiege.BuildConfig
import com.droidsiege.challenges.common.ActionChallengeScreen
import com.droidsiege.challenges.common.KitAction
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import java.io.File

private const val TAG = "SiegeWallet"

private const val FLAG_L1 = "DS{storage_logs_L1_d81f6c}"
private const val FLAG_L2 = "DS{storage_logs_L2_e54c81}"
private const val FLAG_L3 = "DS{storage_logs_L3_27f0b8}"
private const val FLAG_L4 = "DS{storage_logs_L4_6a13d9}"

@Suppress("UnusedParameter", "UnusedPrivateProperty")
object LogsVault {
    fun refreshBalance(
        context: Context,
        secureMode: Boolean,
    ): String {
        val balance = "1,284.50"
        if (!secureMode) {
            Log.d(TAG, "Balance refreshed for session=$FLAG_L1 amount=$balance")
        }
        return "Balance: ${'$'}$balance" + if (secureMode) " (session id redacted from logs)" else ""
    }

    fun simulateSyncFailure(
        context: Context,
        secureMode: Boolean,
    ): String {
        return try {
            @Suppress("DIVISION_BY_ZERO")
            val throughput = 1024 / 0
            "Sync throughput: ${'$'}throughput"
        } catch (boom: ArithmeticException) {
            if (!secureMode) {
                Log.e(TAG, "sync failed, session context: $FLAG_L2", boom)
            } else {
                Log.e(TAG, "sync failed, session context: [redacted]", boom)
            }
            "Sync failed (handled). Check the crash trace."
        }
    }

    fun dumpVerboseSession(
        context: Context,
        secureMode: Boolean,
    ): String {
        if (!secureMode && BuildConfig.DEBUG) {
            Log.v(TAG, "verbose: full session record = $FLAG_L3")
        }
        return if (!secureMode && BuildConfig.DEBUG) {
            "Verbose diagnostics enabled in this build."
        } else {
            "Verbose diagnostics are compiled out."
        }
    }

    fun fileCrashBreadcrumb(
        context: Context,
        secureMode: Boolean,
    ): String {
        val reporter = CrashReporterStub.get(context)
        if (!secureMode) {
            reporter.breadcrumb("payment.authorized", "receipt=$FLAG_L4")
        } else {
            reporter.breadcrumb("payment.authorized", "receipt=[redacted]")
        }
        reporter.flush()
        return "Breadcrumb recorded and flushed. Payload: ${reporter.payloadPath()}"
    }
}

/**
 * A third-party crash reporter, the way SDKs like it: a breadcrumb buffer that is
 * flushed to disk so the next launch can attach it to a crash payload.
 */
class CrashReporterStub private constructor(context: Context) {
    private val payloadFile = File(context.filesDir, "crash_breadcrumbs.log")
    private val buffer = mutableListOf<String>()

    fun breadcrumb(
        name: String,
        detail: String,
    ) {
        buffer += "${System.currentTimeMillis()} $name $detail"
    }

    fun flush() {
        payloadFile.appendText(buffer.joinToString(separator = "\n", postfix = "\n"))
        buffer.forEach { Log.i("SiegeReporter", it) }
        buffer.clear()
    }

    fun payloadPath(): String = payloadFile.absolutePath

    companion object {
        private var instance: CrashReporterStub? = null

        fun get(context: Context): CrashReporterStub =
            instance ?: CrashReporterStub(context.applicationContext).also { instance = it }
    }
}

class LogsL1Challenge : TieredChallenge(
    category = "storage",
    slug = "logs",
    level = Difficulty.EASY,
    title = "Balance Refresh",
    brief = "Refreshing the balance talks to the backend and leaves a paper trail. Pull " +
        "the paper trail.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "MASTG-TEST-0x60"),
    hints = listOf(
        "Logcat is system-wide: adb logcat shows every app's output.",
        "The wallet logs under the SiegeWallet tag.",
        "adb logcat -d -s SiegeWallet after pressing the button.",
    ),
    flag = FLAG_L1,
    learn = LearnContent(
        theory = "Logcat is a shared, world-readable-on-debug stream that survives in " +
            "buffers, IDE windows, CI logs and bug reports. Anything an app logs — " +
            "tokens, PII, receipts — is published to every process that can read the " +
            "buffer.\n\n" +
            "The fix is not 'be careful': strip logging of sensitive fields in code " +
            "review and mechanically in release builds.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASTG-TEST-0x60"),
        vulnerableSnippet = "Log.d(TAG, \"Balance refreshed for session=${'$'}sessionToken\")",
        fixSnippet = "// Never log the session; log a short-lived correlation id at most:\n" +
            "Log.d(TAG, \"Balance refreshed\")",
        takeaway = "logcat is a public billboard — treat every Log.* call as a release.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Refresh balance") { ctx, secure -> LogsVault.refreshBalance(ctx, secure) },
            ),
        )
    }
}

class LogsL2Challenge : TieredChallenge(
    category = "storage",
    slug = "logs",
    level = Difficulty.MEDIUM,
    title = "Handled Failure",
    brief = "The happy path logs nothing interesting anymore. But when the sync engine " +
        "chokes, the handler wants to be helpful.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "MASTG-TEST-0x60"),
    hints = listOf(
        "Force the failure path — the button exists for it.",
        "Catch blocks often log the richest context 'for debugging'.",
        "The exception message carries the session record with it.",
    ),
    flag = FLAG_L2,
    learn = LearnContent(
        theory = "Error handlers log context to make bugs diagnosable — and the context " +
            "they choose is the sensitive one: session ids, tokens, full payloads. The " +
            "rare, exceptional path is the one nobody reviews and the one support " +
            "tickets reproduce.\n\n" +
            "Error logging needs the same redaction review as happy-path logging, or more.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASTG-TEST-0x60"),
        vulnerableSnippet = "catch (boom: ArithmeticException) {\n" +
            "    Log.e(TAG, \"sync failed, session context: ${'$'}session\", boom)\n" +
            "}",
        fixSnippet = "catch (boom: ArithmeticException) {\n" +
            "    Log.e(TAG, \"sync failed\", boom) // no session material",
        takeaway = "Audit catch blocks first — that is where secrets go to logcat.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Run sync (it will fail)") { ctx, secure -> LogsVault.simulateSyncFailure(ctx, secure) },
            ),
        )
    }
}

class LogsL3Challenge : TieredChallenge(
    category = "storage",
    slug = "logs",
    level = Difficulty.HARD,
    title = "Debug Verbose",
    brief = "Verbose session dumps are guarded by a debug flag, so production users are " +
        "safe. This device runs a debug build — does that protection still mean anything?",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "M8", "MASTG-TEST-0x60"),
    hints = listOf(
        "BuildConfig.DEBUG guards nothing when the artifact you hold is the debug build.",
        "Press the dump button, then read the verbose buffer: adb logcat -d -s SiegeWallet.",
        "Sideloading a debug build is a standard bug-bounty scenario — the guard must survive it.",
    ),
    flag = FLAG_L3,
    learn = LearnContent(
        theory = "Gating sensitive logs on BuildConfig.DEBUG protects the release " +
            "artifact only. Debug builds ship to testers, bug-bounty programs, internal " +
            "app stores and (via deobfuscation) to anyone. The guard is real but the " +
            "boundary it guards is not the attacker's boundary.\n\n" +
            "Defense in depth: never assemble the sensitive string at all, and strip " +
            "Log calls in release through R8/Timber trees.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASVS-RESILIENCE-2", "MASTG-TEST-0x60"),
        vulnerableSnippet = "if (BuildConfig.DEBUG) {\n" +
            "    Log.v(TAG, \"verbose: full session record = ${'$'}session\")\n" +
            "}",
        fixSnippet = "// The string is never built, guard or no guard:\n" +
            "Log.v(TAG, \"verbose session dump disabled\")\n" +
            "// + Timber release tree that drops DEBUG priority",
        takeaway = "Build flags gate compilation, not trust — the build you hand out is the build that leaks.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Dump verbose session") { ctx, secure -> LogsVault.dumpVerboseSession(ctx, secure) },
            ),
        )
    }
}

class LogsL4Challenge : TieredChallenge(
    category = "storage",
    slug = "logs",
    level = Difficulty.INSANE,
    title = "Reporter Breadcrumbs",
    brief = "The crash reporter SDK records breadcrumbs so support can replay a payment " +
        "failure. The SDK is trusted with everything the app does — check what it keeps.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-1", "M2", "MASTG-TEST-0x60"),
    hints = listOf(
        "Breadcrumbs are flushed to a local payload file ready for upload.",
        "The file lives in filesDir: crash_breadcrumbs.log.",
        "run-as com.droidsiege cat files/crash_breadcrumbs.log — the SDK even logs each entry.",
    ),
    flag = FLAG_L4,
    learn = LearnContent(
        theory = "Third-party SDKs become secret-keepers by accident: whatever flows " +
            "through their API (breadcrumbs, custom events, user attributes) is buffered " +
            "and shipped to their endpoints. A payment receipt in a breadcrumb is a " +
            "receipt in a vendor's bucket.\n\n" +
            "Gate SDK calls behind an allowlist of fields, and treat the SDK's on-disk " +
            "payload as part of your attack surface — because it is.",
        mastgRefs = listOf("MASVS-STORAGE-1", "MASVS-RESILIENCE-3", "MASTG-TEST-0x60"),
        vulnerableSnippet = "reporter.breadcrumb(\"payment.authorized\", \"receipt=${'$'}receipt\")\n" +
            "reporter.flush() // writes files/crash_breadcrumbs.log",
        fixSnippet = "reporter.breadcrumb(\"payment.authorized\", \"receipt=[redacted]\")\n" +
            "// + allowlist: only whitelisted fields may enter a breadcrumb",
        takeaway = "Every SDK call is an exfiltration channel — allowlist what may flow into it.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Authorize test payment") { ctx, secure -> LogsVault.fileCrashBreadcrumb(ctx, secure) },
            ),
        )
    }
}
