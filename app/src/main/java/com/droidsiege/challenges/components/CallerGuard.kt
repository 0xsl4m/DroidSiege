package com.droidsiege.challenges.components

import android.content.Context
import android.content.pm.PackageManager

/**
 * Runtime caller validation for the hardened paths of the exported-component tiers.
 * The manifest keeps the components exported (that is the M8 seed and cannot change at
 * runtime); the hardened behavior is the guard refusing callers the app does not trust.
 */
object CallerGuard {
    /** Pure decision over a PackageManager.checkSignatures result. */
    fun isTrusted(signaturesMatch: Int): Boolean = signaturesMatch == PackageManager.SIGNATURE_MATCH

    /** The calling package is trusted when it is the app itself or signed with its key. */
    fun isTrustedCaller(
        context: Context,
        callingPackage: String?,
    ): Boolean {
        if (callingPackage == null) return false
        if (callingPackage == context.packageName) return true
        return isTrusted(context.packageManager.checkSignatures(context.packageName, callingPackage))
    }
}
