package com.droidsiege.core

import com.droidsiege.challenges.demo.HelloFlagChallenge
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FlagValidationTest {
    private val challenge = HelloFlagChallenge()

    @Test
    fun exactFlagValidates() {
        assertThat(challenge.validateFlag("DS{demo_helloflag_L1_f17a2b}")).isTrue()
    }

    @Test
    fun surroundingWhitespaceIsTrimmed() {
        assertThat(challenge.validateFlag("  DS{demo_helloflag_L1_f17a2b}  ")).isTrue()
        assertThat(challenge.validateFlag("\tDS{demo_helloflag_L1_f17a2b}\n")).isTrue()
    }

    @Test
    fun caseMustMatchExactly() {
        assertThat(challenge.validateFlag("ds{demo_helloflag_L1_f17a2b}")).isFalse()
        assertThat(challenge.validateFlag("DS{DEMO_HELLOFLAG_L1_F17A2B}")).isFalse()
    }

    @Test
    fun anyOtherInputIsRejected() {
        assertThat(challenge.validateFlag("")).isFalse()
        assertThat(challenge.validateFlag("DS{demo_helloflag_L1_ffffff}")).isFalse()
        assertThat(challenge.validateFlag("flag")).isFalse()
    }

    @Test
    fun registryFlagFollowsTheDocumentedFormat() {
        assertThat(Flag.matchesFormat(challenge.flag)).isTrue()
        assertThat(Flag.format("demo", "helloflag", 1, "f17a2b")).isEqualTo(challenge.flag)
    }
}
