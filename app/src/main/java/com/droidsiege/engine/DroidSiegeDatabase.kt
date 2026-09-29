package com.droidsiege.engine

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ScoreboardEntity::class], version = 1, exportSchema = false)
abstract class DroidSiegeDatabase : RoomDatabase() {
    abstract fun scoreboardDao(): ScoreboardDao
}
