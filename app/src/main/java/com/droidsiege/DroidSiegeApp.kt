package com.droidsiege

import android.app.Application
import com.droidsiege.challenges.demo.DemoCategory
import com.droidsiege.core.ChallengeRegistry
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DroidSiegeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ChallengeRegistry.registerAll(DemoCategory)
    }
}
