package com.droidsiege.engine

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreboardDao {
    @Upsert
    suspend fun upsert(entry: ScoreboardEntity)

    @Query("SELECT * FROM scoreboard WHERE idKey = :idKey")
    suspend fun byId(idKey: String): ScoreboardEntity?

    @Query("SELECT * FROM scoreboard WHERE idKey = :idKey")
    fun observe(idKey: String): Flow<ScoreboardEntity?>

    @Query("SELECT * FROM scoreboard")
    fun observeAll(): Flow<List<ScoreboardEntity>>

    @Query("SELECT COALESCE(SUM(pointsAwarded), 0) FROM scoreboard")
    fun observeTotalScore(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scoreboard WHERE solved = 1")
    fun observeSolvedCount(): Flow<Int>

    @Query("DELETE FROM scoreboard")
    suspend fun clear()
}
