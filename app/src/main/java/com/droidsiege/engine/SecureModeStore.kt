package com.droidsiege.engine

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SecureModeStore(private val context: Context) {
    val secureMode: Flow<Boolean>
        get() = context.settingsStore.data.map { preferences -> preferences[SECURE_MODE_KEY] ?: false }

    suspend fun setSecureMode(enabled: Boolean) {
        context.settingsStore.edit { preferences -> preferences[SECURE_MODE_KEY] = enabled }
    }

    companion object {
        private val SECURE_MODE_KEY = booleanPreferencesKey("secureMode")
    }
}
