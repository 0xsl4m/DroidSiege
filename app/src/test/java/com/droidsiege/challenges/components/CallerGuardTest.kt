package com.droidsiege.challenges.components

import android.content.pm.PackageManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CallerGuardTest {
    @Test
    fun signatureMatchIsTrusted() {
        assertThat(CallerGuard.isTrusted(PackageManager.SIGNATURE_MATCH)).isTrue()
    }

    @Test
    fun unknownOrMismatchedSignaturesAreUntrusted() {
        assertThat(CallerGuard.isTrusted(-1)).isFalse() // SIGNATURE_NOT_MATCH
        assertThat(CallerGuard.isTrusted(-2)).isFalse() // SIGNATURE_UNKNOWN_PACKAGE
        assertThat(CallerGuard.isTrusted(-999)).isFalse()
    }
}
