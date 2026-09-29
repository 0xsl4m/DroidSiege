package com.droidsiege.engine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class FakeScoreboardDao : ScoreboardDao {
    private val state = MutableStateFlow<Map<String, ScoreboardEntity>>(emptyMap())

    override suspend fun upsert(entry: ScoreboardEntity) {
        state.update { current -> current + (entry.idKey to entry) }
    }

    override suspend fun byId(idKey: String): ScoreboardEntity? = state.value[idKey]

    override fun observe(idKey: String): Flow<ScoreboardEntity?> = state.map { it[idKey] }

    override fun observeAll(): Flow<List<ScoreboardEntity>> = state.map { it.values.toList() }

    override fun observeTotalScore(): Flow<Int> =
        state.map { entries ->
            entries.values.sumOf { it.pointsAwarded }
        }

    override fun observeSolvedCount(): Flow<Int> =
        state.map { entries ->
            entries.values.count { it.solved }
        }

    override suspend fun clear() {
        state.update { emptyMap() }
    }
}
