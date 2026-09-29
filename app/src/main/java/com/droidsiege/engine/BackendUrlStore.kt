package com.droidsiege.engine

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8080"

class BackendUrlStore(private val context: Context) {
    val backendUrl: Flow<String>
        get() =
            context.settingsStore.data.map { preferences ->
                preferences[BACKEND_URL_KEY] ?: DEFAULT_BACKEND_URL
            }

    suspend fun setBackendUrl(url: String) {
        context.settingsStore.edit { preferences -> preferences[BACKEND_URL_KEY] = url }
    }

    companion object {
        private val BACKEND_URL_KEY = stringPreferencesKey("backendBaseUrl")
    }
}
