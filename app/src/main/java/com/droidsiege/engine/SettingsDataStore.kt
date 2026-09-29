package com.droidsiege.engine

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.settingsStore by preferencesDataStore(name = "droidsiege_settings")
