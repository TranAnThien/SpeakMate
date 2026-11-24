package com.project.speakmate.ui.placement

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.project.speakmate.R
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.ui.placement.state.PromotionIntroductionUiState
import com.project.speakmate.ui.theme.SpeakMateTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PromotionIntroductionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private var backCount = 0
    private var startCount = 0
    private var returnCount = 0

    @Test
    fun titleNamesCurrentAndNextLevel() {
        showScreen(PromotionIntroductionUiState(SpeakingLevel.A1, SpeakingLevel.A2))

        composeRule
            .onNodeWithText(targetContext.getString(R.string.promotion_runtime_introduction_title, "A1", "A2"))
            .assertExists()
    }

    @Test
    fun titleChangesWithTheLevels() {
        showScreen(PromotionIntroductionUiState(SpeakingLevel.B1, SpeakingLevel.B2))

        composeRule
            .onNodeWithText(targetContext.getString(R.string.promotion_runtime_introduction_title, "B1", "B2"))
            .assertExists()
    }

    @Test
    fun startButtonInvokesStartAssessment() {
        showScreen(PromotionIntroductionUiState())

        composeRule.onNodeWithText(text(R.string.placement_start)).performClick()

        assertEquals(1, startCount)
        assertEquals(0, returnCount)
    }

    @Test
    fun returnToPathButtonInvokesReturnToPath() {
        showScreen(PromotionIntroductionUiState())

        composeRule.onNodeWithText(text(R.string.goal_assessment_return_to_path)).performClick()

        assertEquals(1, returnCount)
        assertEquals(0, startCount)
    }

    @Test
    fun backButtonInvokesBack() {
        showScreen(PromotionIntroductionUiState())

        composeRule.onNodeWithContentDescription(text(R.string.accessibility_back)).performClick()

        assertEquals(1, backCount)
    }

    @Test
    fun showsPromotionHeader() {
        showScreen(PromotionIntroductionUiState())

        composeRule.onNodeWithText(text(R.string.promotion_header)).assertExists()
    }

    private fun showScreen(state: PromotionIntroductionUiState) {
        composeRule.setContent {
            SpeakMateTheme {
                PromotionIntroductionScreen(
                    state = state,
                    onBack = { backCount++ },
                    onStartAssessment = { startCount++ },
                    onReturnToPath = { returnCount++ }
                )
            }
        }
    }

    private fun text(resId: Int): String = targetContext.getString(resId)
}
