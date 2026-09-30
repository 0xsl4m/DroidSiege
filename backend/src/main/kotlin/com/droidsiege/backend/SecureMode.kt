package com.droidsiege.backend

/**
 * Mirrors the app's Secure/Insecure toggle, driven by the SECURE_MODE environment
 * variable ("on" hardens every family, anything else keeps the vulnerable paths).
 * Mutable so the test host can flip it per scenario.
 */
object SecureMode {
    @Volatile
    var enabled: Boolean = System.getenv("SECURE_MODE")?.equals("on", ignoreCase = true) == true
}
