package com.droidsiege

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.droidsiege.challenges.demo.DemoCategory
import com.droidsiege.core.ChallengeRegistry
import com.droidsiege.engine.ScoreboardRepository
import com.droidsiege.ui.components.FLAG_INPUT_TAG
import com.droidsiege.ui.components.FLAG_SUBMIT_TAG
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HelloFlagChallengeSubmitTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var scoreboard: ScoreboardRepository

    companion object {
        @BeforeClass
        @JvmStatic
        fun registerChallenges() {
            // HiltTestApplication replaces DroidSiegeApp, whose onCreate() performs the
            // production registration. This runs before the test rules launch the
            // activity, so the first UI frame already sees the registry; registerAll
            // is idempotent in production.
            ChallengeRegistry.registerAll(DemoCategory)
        }
    }

    @Before
    fun setUp() {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(context, TestEntryPoint::class.java)
        scoreboard = entryPoint.scoreboardRepository()
        runBlocking { scoreboard.resetProgress() }
    }

    @Test
    fun submittingCorrectFlagAwardsPoints() {
        composeRule.onNodeWithText("Demo").performClick()
        composeRule.onNodeWithText("Hello, Flag!").performClick()

        composeRule.onNodeWithTag(FLAG_INPUT_TAG).performTextInput("DS{demo_helloflag_L1_f17a2b}")
        composeRule.onNodeWithTag(FLAG_INPUT_TAG).assertTextContains(
            "DS{demo_helloflag_L1_f17a2b}",
            substring = true,
        )
        composeRule.onNodeWithTag(FLAG_SUBMIT_TAG).performClick()

        // Pipeline: the solved row reaches the scoreboard store.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runBlocking { scoreboard.totalScore.first() == 100 }
        }

        // Inline feedback: the flag input switches to its solved state.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithText("Solved — flag accepted")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Scoreboard propagation: back on the category screen, the persistent
        // badge reflects the award.
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithContentDescription("Score: 100")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onAllNodesWithContentDescription("Score: 100").onFirst().assertExists()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TestEntryPoint {
        fun scoreboardRepository(): ScoreboardRepository
    }
}
