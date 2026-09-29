package com.droidsiege

import android.app.Application
import com.droidsiege.challenges.auth.AuthCategory
import com.droidsiege.challenges.components.ComponentsCategory
import com.droidsiege.challenges.crypto.CryptoCategory
import com.droidsiege.challenges.demo.DemoCategory
import com.droidsiege.challenges.network.NetworkCategory
import com.droidsiege.challenges.storage.StorageCategory
import com.droidsiege.challenges.webview.WebViewCategory
import com.droidsiege.core.ChallengeRegistry
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DroidSiegeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ChallengeRegistry.registerAll(
            DemoCategory,
            StorageCategory,
            CryptoCategory,
            ComponentsCategory,
            NetworkCategory,
            WebViewCategory,
            AuthCategory,
        )
    }
}
