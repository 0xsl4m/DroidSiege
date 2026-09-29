package com.droidsiege.challenges.auth

import android.app.Activity
import android.os.Bundle
import com.droidsiege.challenges.components.renderScreen

/** L2 hidden screen — reachable by direct launch; no role check in the insecure mode. */
class AdminScreenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val secure = com.droidsiege.challenges.components.secureMode(this)
        if (secure) {
            renderScreen(this, "Admin screen", "role check failed — finishing", secret = null, denied = true)
            finish()
            return
        }
        renderScreen(
            this,
            "Admin screen",
            "Hidden admin screen. Admin code:",
            secret = AuthFlags.CHECK_L2,
        )
    }
}
