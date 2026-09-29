package com.droidsiege.di

import android.content.Context
import androidx.room.Room
import com.droidsiege.engine.DroidSiegeDatabase
import com.droidsiege.engine.ScoreboardDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): DroidSiegeDatabase = Room.databaseBuilder(context, DroidSiegeDatabase::class.java, "droidsiege.db").build()

    @Provides
    fun provideScoreboardDao(database: DroidSiegeDatabase): ScoreboardDao = database.scoreboardDao()
}
