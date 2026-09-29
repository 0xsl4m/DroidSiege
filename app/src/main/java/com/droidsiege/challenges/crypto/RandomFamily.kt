package com.droidsiege.challenges.crypto

import android.content.Context
import androidx.compose.runtime.Composable
import com.droidsiege.challenges.common.GateChallengeScreen
import com.droidsiege.challenges.common.HexCodec
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import java.security.MessageDigest
import java.util.Random

private const val FLAG_L1 = "DS{crypto_random_L1_6b41f8}"
private const val FLAG_L2 = "DS{crypto_random_L2_a7d325}"
private const val FLAG_L3 = "DS{crypto_random_L3_c0e892}"
private const val FLAG_L4 = "DS{crypto_random_L4_34f67a}"

object PredictableTokens {
    /** L1 — the issuance code uses a fixed seed for "reproducible support tickets". */
    fun recoveryToken(seed: Long = 42L): String =
        HexCodec.toHex(java.nio.ByteBuffer.allocate(4).putInt(Random(seed).nextInt()).array())
            .substring(0, 6)

    /** L2 — the seed is the epoch hour. */
    fun hourToken(epochHour: Long): String {
        val prng = Random(epochHour)
        val value = prng.nextInt()
        return HexCodec.toHex(java.nio.ByteBuffer.allocate(4).putInt(value).array()).substring(0, 6)
    }

    /** L3 — a counter hashed with a weak, known tag. */
    fun sessionToken(counter: Int): String {
        val digest = MessageDigest.getInstance("MD5")
            .digest("siege-session-$counter".toByteArray())
        return HexCodec.toHex(digest).substring(0, 6)
    }

    /** L4 — the daily OTP is Random(dayOfMonth)-derived. */
    fun dailyOtp(dayOfMonth: Int): String {
        val prng = Random(dayOfMonth.toLong())
        val value = prng.nextInt(1_000_000)
        return "%06d".format(value)
    }
}

class RandomL1Challenge : TieredChallenge(
    category = "crypto",
    slug = "random",
    level = Difficulty.EASY,
    title = "Fixed Seed",
    brief = "Recovery tokens are issued by the token service. Support documented that " +
        "issuance is fully reproducible — enter a valid recovery token to open the " +
        "session.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-3", "MASTG-TEST-0x29"),
    hints = listOf(
        "The token service uses java.util.Random with a fixed seed — decompile TokenService.",
        "Random(42).nextInt(), hex-encoded, first 6 chars.",
        "Any Java/Python LCG implementation reproduces it exactly.",
    ),
    flag = FLAG_L1,
    learn = LearnContent(
        theory = "java.util.Random is a deterministic LCG: a known seed means every " +
            "token it will ever produce is already written down. 'Reproducible for " +
            "support' is reproducible for everyone holding the decompiled constants.\n\n" +
            "Security tokens come from SecureRandom — always, with no configuration " +
            "that can make them come from anywhere else.",
        mastgRefs = listOf("MASVS-CRYPTO-3", "MASTG-TEST-0x29"),
        vulnerableSnippet = "val prng = Random(42) // reproducible for support",
        fixSnippet = "val token = SecureRandom().generateSeed(3).toHex()",
        takeaway = "A token that can be recomputed was never random.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        GateChallengeScreen(
            secureMode = secureMode,
            note = "Issuance diagnostics: java.util.Random, fixed seed, nextInt().",
            inputLabel = "Recovery token (6 hex)",
            unlockLabel = "Open session",
            lockedMessage = "Invalid recovery token.",
            expectedInput = { secureMode ->
                // secure builds refuse to compute the token from a guessable seed
                if (!secureMode) PredictableTokens.recoveryToken() else null
            },
            unlockedContent = FLAG_L1,
        )
    }
}

class RandomL2Challenge : TieredChallenge(
    category = "crypto",
    slug = "random",
    level = Difficulty.MEDIUM,
    title = "Hour Window",
    brief = "Tokens rotate every hour for freshness. The issuance time of the current " +
        "token is published on the status line. The key space is smaller than it looks.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-3", "MASTG-TEST-0x29"),
    hints = listOf(
        "seed = epoch hours; the status line shows the current minute count.",
        "Brute the 24 seeds of the current day (or a small window around it).",
        "tools/brute_hour_token.py finds the seed whose output matches the token format.",
    ),
    flag = FLAG_L2,
    learn = LearnContent(
        theory = "Seeding randomness with wall-clock time shrinks the key space to the " +
            "precision of the clock. An hour seed leaves 24 candidates a day; a minute " +
            "seed, 1,440. Any offline brute force over a bounded window wins.\n\n" +
            "Entropy cannot be recovered after the fact — the seed must be drawn from " +
            "SecureRandom at generation time.",
        mastgRefs = listOf("MASVS-CRYPTO-3", "MASTG-TEST-0x29"),
        vulnerableSnippet = "val prng = Random(System.currentTimeMillis() / 3_600_000)",
        fixSnippet = "val token = ByteArray(3).also { SecureRandom().nextBytes(it) }",
        takeaway = "Time gives attackers a window; only SecureRandom gives them nothing.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val nowMinutes = System.currentTimeMillis() / 60_000
        GateChallengeScreen(
            secureMode = secureMode,
            note = "Issued at: $nowMinutes minutes since epoch (token rotates hourly).",
            inputLabel = "Current token (6 hex)",
            unlockLabel = "Validate token",
            lockedMessage = "Token rejected — wrong rotation window?",
            expectedInput = { secureMode ->
                if (!secureMode) PredictableTokens.hourToken(nowMinutes / 60) else null
            },
            unlockedContent = FLAG_L2,
        )
    }
}

class RandomL3Challenge : TieredChallenge(
    category = "crypto",
    slug = "random",
    level = Difficulty.HARD,
    title = "Counter Sessions",
    brief = "Session ids are compact and sortable: a counter passed through the house " +
        "hash. Your current session number is shown. Your neighbor's is next to it.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-3", "MASTG-TEST-0x29"),
    hints = listOf(
        "token(n) = md5(\"siege-session-<n>\")[:6]. Counters are adjacent by construction.",
        "Your session number is in prefs: shared_prefs/siege_session_prefs.xml (key session_counter).",
        "Compute the token for your counter — or the neighbor's.",
    ),
    flag = FLAG_L3,
    learn = LearnContent(
        theory = "Identifiers made of counters and weak hashes are enumeration fuel: " +
            "one observation reveals the scheme, and every past and future id is a " +
            "one-liner away. 'Compact and sortable' are the opposite of what a session " +
            "id needs.\n\n" +
            "Session tokens are opaque random values from SecureRandom — no structure, " +
            "no sequence, no meaning.",
        mastgRefs = listOf("MASVS-CRYPTO-3", "MASTG-TEST-0x29"),
        vulnerableSnippet = "val id = md5(\"siege-session-${'$'}counter\").take(6)",
        fixSnippet = "val id = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()",
        takeaway = "If you can compute one id, you can compute them all.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val prefs = context.getSharedPreferences("siege_session_prefs", Context.MODE_PRIVATE)
        val counter = prefs.getInt("session_counter", 0) + 1
        prefs.edit().putInt("session_counter", counter).apply()
        GateChallengeScreen(
            secureMode = secureMode,
            note = "Your session number: $counter (id = md5(\"siege-session-<n>\")[:6]).",
            inputLabel = "Session id (6 hex)",
            unlockLabel = "Authenticate",
            lockedMessage = "Session id invalid.",
            expectedInput = { secureMode ->
                if (!secureMode) PredictableTokens.sessionToken(counter) else null
            },
            unlockedContent = FLAG_L3,
        )
    }
}

class RandomL4Challenge : TieredChallenge(
    category = "crypto",
    slug = "random",
    level = Difficulty.INSANE,
    title = "Daily OTP",
    brief = "The vault rotates its one-time password daily for operability. Today's OTP " +
        "was issued at midnight. The vault is one OTP away.",
    owaspRefs = listOf("M10", "MASVS-CRYPTO-3", "M3", "MASTG-TEST-0x29"),
    hints = listOf(
        "Daily rotation does not add entropy — the OTP is Random(dayOfMonth).",
        "At most 31 candidates; today's day-of-month narrows it to one.",
        "tools/predict_daily_otp.py computes it from the date.",
    ),
    flag = FLAG_L4,
    learn = LearnContent(
        theory = "An OTP gains nothing from rotating if each value is derivable: " +
            "Random(dayOfMonth) has 31 possible outputs and the date is public. The " +
            "'chain to auth bypass' is literal — predicting the OTP is bypassing the " +
            "second factor entirely.\n\n" +
            "OTP values are SecureRandom per challenge, single-use, with server-side " +
            "verification and rate limits.",
        mastgRefs = listOf("MASVS-CRYPTO-3", "MASTG-TEST-0x29"),
        vulnerableSnippet = "val otp = Random(dayOfMonth.toLong()).nextInt(1_000_000)",
        fixSnippet = "val otp = SecureRandom().nextInt(1_000_000) // per challenge, single use",
        takeaway = "Rotation without entropy is theater.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
        GateChallengeScreen(
            secureMode = secureMode,
            note = "Today's OTP was issued at 00:00 (rotation #, day-of-month seed).",
            inputLabel = "OTP (6 digits)",
            unlockLabel = "Decrypt vault",
            lockedMessage = "Invalid OTP. Try tomorrow — or predict better.",
            expectedInput = { secureMode ->
                if (!secureMode) PredictableTokens.dailyOtp(today) else null
            },
            unlockedContent = FLAG_L4,
        )
    }
}
