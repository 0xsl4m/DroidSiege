package com.droidsiege.engine

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scoreboard")
data class ScoreboardEntity(
    @PrimaryKey val idKey: String,
    val category: String,
    val slug: String,
    val level: String,
    val solved: Boolean,
    val pointsAwarded: Int,
    val hintsUsed: Int,
    val solvedAt: Long?,
)
