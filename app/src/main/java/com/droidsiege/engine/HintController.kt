package com.droidsiege.engine

import com.droidsiege.core.Challenge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HintController
    @Inject
    constructor(private val scoreboardRepository: ScoreboardRepository) {
        fun hintsUsed(idKey: String): Flow<Int> =
            scoreboardRepository.observe(idKey).map { entry -> entry?.hintsUsed ?: 0 }

        suspend fun revealNext(challenge: Challenge): Int? {
            val current = scoreboardRepository.hintsUsed(challenge.id.key)
            if (current >= challenge.hints.size) return null
            val next = current + 1
            scoreboardRepository.setHintsUsed(challenge, next)
            return next
        }

        fun awardFor(
            challenge: Challenge,
            hintsUsed: Int,
        ): Int =
            HintPenalty.pointsAwarded(
                levelPoints = challenge.id.level.points,
                hintsUsed = hintsUsed.coerceAtMost(challenge.hints.size),
            )
    }
