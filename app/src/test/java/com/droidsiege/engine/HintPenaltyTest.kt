package com.droidsiege.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HintPenaltyTest {
    @Test
    fun noHintsKeepsFullPoints() {
        assertThat(HintPenalty.pointsAwarded(100, 0)).isEqualTo(100)
        assertThat(HintPenalty.pointsAwarded(200, 0)).isEqualTo(200)
        assertThat(HintPenalty.pointsAwarded(400, 0)).isEqualTo(400)
        assertThat(HintPenalty.pointsAwarded(800, 0)).isEqualTo(800)
    }

    @Test
    fun penaltyAccumulatesPerRevealedHint() {
        assertThat(HintPenalty.pointsAwarded(100, 1)).isEqualTo(85)
        assertThat(HintPenalty.pointsAwarded(100, 2)).isEqualTo(55)
        assertThat(HintPenalty.pointsAwarded(200, 1)).isEqualTo(170)
        assertThat(HintPenalty.pointsAwarded(200, 2)).isEqualTo(110)
        assertThat(HintPenalty.pointsAwarded(200, 3)).isEqualTo(20)
        assertThat(HintPenalty.pointsAwarded(800, 2)).isEqualTo(440)
    }

    @Test
    fun penaltyIsCappedAtNinetyPercent() {
        assertThat(HintPenalty.pointsAwarded(100, 3)).isEqualTo(10)
        assertThat(HintPenalty.pointsAwarded(800, 4)).isEqualTo(80)
        assertThat(HintPenalty.pointsAwarded(100, 10)).isEqualTo(10)
    }
}
