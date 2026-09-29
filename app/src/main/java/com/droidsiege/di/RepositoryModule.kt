package com.droidsiege.di

import android.content.Context
import com.droidsiege.engine.BackendUrlStore
import com.droidsiege.engine.SecureModeStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideSecureModeStore(
        @ApplicationContext context: Context,
    ): SecureModeStore = SecureModeStore(context)

    @Provides
    @Singleton
    fun provideBackendUrlStore(
        @ApplicationContext context: Context,
    ): BackendUrlStore = BackendUrlStore(context)
}
