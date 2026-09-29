package com.droidsiege.engine

import com.droidsiege.core.Challenge
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScoreboardRepository
    @Inject
    constructor(private val dao: ScoreboardDao) {
        val totalScore: Flow<Int> = dao.observeTotalScore()
        val solvedCount: Flow<Int> = dao.observeSolvedCount()

        fun observeAll(): Flow<List<ScoreboardEntity>> = dao.observeAll()

        fun observe(idKey: String): Flow<ScoreboardEntity?> = dao.observe(idKey)

        suspend fun hintsUsed(idKey: String): Int = dao.byId(idKey)?.hintsUsed ?: 0

        suspend fun markSolved(
            challenge: Challenge,
            hintsUsed: Int,
        ): Boolean {
            val existing = dao.byId(challenge.id.key)
            if (existing?.solved == true) return false
            val award = HintPenalty.pointsAwarded(challenge.id.level.points, hintsUsed)
            dao.upsert(
                ScoreboardEntity(
                    idKey = challenge.id.key,
                    category = challenge.id.category,
                    slug = challenge.id.slug,
                    level = challenge.id.level.name,
                    solved = true,
                    pointsAwarded = award,
                    hintsUsed = hintsUsed,
                    solvedAt = System.currentTimeMillis(),
                ),
            )
            return true
        }

        suspend fun setHintsUsed(
            challenge: Challenge,
            hintsUsed: Int,
        ) {
            val existing = dao.byId(challenge.id.key)
            dao.upsert(
                ScoreboardEntity(
                    idKey = challenge.id.key,
                    category = challenge.id.category,
                    slug = challenge.id.slug,
                    level = challenge.id.level.name,
                    solved = existing?.solved ?: false,
                    pointsAwarded = existing?.pointsAwarded ?: 0,
                    hintsUsed = hintsUsed,
                    solvedAt = existing?.solvedAt,
                ),
            )
        }

        suspend fun resetProgress() {
            dao.clear()
        }
    }
