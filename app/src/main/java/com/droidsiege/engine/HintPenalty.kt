package com.droidsiege.engine

import kotlin.math.ceil

object HintPenalty {
    private const val PENALTY_PER_HINT = 0.15
    private const val MINIMUM_AWARD_FRACTION = 0.1

    fun pointsAwarded(
        levelPoints: Int,
        hintsUsed: Int,
    ): Int {
        var penalty = 0
        for (index in 0 until hintsUsed) {
            penalty += ceil(levelPoints * PENALTY_PER_HINT * (index + 1)).toInt()
        }
        val minimumAward = ceil(levelPoints * MINIMUM_AWARD_FRACTION).toInt()
        return (levelPoints - penalty).coerceAtLeast(minimumAward)
    }
}
