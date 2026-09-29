package com.droidsiege.engine

import com.droidsiege.challenges.demo.HelloFlagChallenge
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ScoreboardRepositoryTest {
    private val challenge = HelloFlagChallenge()
    private lateinit var repository: ScoreboardRepository

    @Before
    fun setUp() {
        repository = ScoreboardRepository(FakeScoreboardDao())
    }

    @Test
    fun firstSolveAwardsFullPointsWithNoHints() =
        runTest {
            val awarded = repository.markSolved(challenge, hintsUsed = 0)

            assertThat(awarded).isTrue()
            val entry = repository.observe(challenge.id.key).first()
            assertThat(entry).isNotNull()
            assertThat(entry!!.solved).isTrue()
            assertThat(entry.pointsAwarded).isEqualTo(100)
            assertThat(entry.hintsUsed).isEqualTo(0)
            assertThat(entry.solvedAt).isNotNull()
            assertThat(repository.totalScore.first()).isEqualTo(100)
        }

    @Test
    fun secondSolveDoesNotAwardTwice() =
        runTest {
            repository.markSolved(challenge, hintsUsed = 0)

            val second = repository.markSolved(challenge, hintsUsed = 0)

            assertThat(second).isFalse()
            val entry = repository.observe(challenge.id.key).first()
            assertThat(entry!!.pointsAwarded).isEqualTo(100)
            assertThat(repository.totalScore.first()).isEqualTo(100)
        }

    @Test
    fun hintPenaltiesReduceTheAward() =
        runTest {
            repository.markSolved(challenge, hintsUsed = 2)

            val entry = repository.observe(challenge.id.key).first()
            // 100 - (ceil(100*0.15*1) + ceil(100*0.15*2)) = 100 - 45
            assertThat(entry!!.pointsAwarded).isEqualTo(55)
        }

    @Test
    fun awardNeverDropsBelowTenPercentOfLevelPoints() =
        runTest {
            repository.markSolved(challenge, hintsUsed = 3)

            val entry = repository.observe(challenge.id.key).first()
            assertThat(entry!!.pointsAwarded).isEqualTo(10)
        }

    @Test
    fun hintStatePersistsWithoutMarkingSolved() =
        runTest {
            repository.setHintsUsed(challenge, 1)

            val entry = repository.observe(challenge.id.key).first()
            assertThat(entry).isNotNull()
            assertThat(entry!!.hintsUsed).isEqualTo(1)
            assertThat(entry.solved).isFalse()
            assertThat(entry.pointsAwarded).isEqualTo(0)
            assertThat(repository.totalScore.first()).isEqualTo(0)
        }

    @Test
    fun resetProgressClearsTheScoreboard() =
        runTest {
            repository.markSolved(challenge, hintsUsed = 0)

            repository.resetProgress()

            assertThat(repository.observe(challenge.id.key).first()).isNull()
            assertThat(repository.totalScore.first()).isEqualTo(0)
        }
}
