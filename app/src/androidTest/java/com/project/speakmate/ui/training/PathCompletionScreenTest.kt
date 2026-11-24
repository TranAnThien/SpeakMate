package com.project.speakmate.ui.training

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.project.speakmate.R
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.ui.theme.SpeakMateTheme
import com.project.speakmate.ui.training.state.PathCompletionUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PathCompletionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private var backCount = 0
    private var promotionCount = 0
    private var returnCount = 0
    private var reviewCount = 0
    private var freePracticeCount = 0

    @Test
    fun intermediateLevelOffersPromotionToNextLevel() {
        showScreen(PathCompletionUiState(level = SpeakingLevel.A1, nextLevel = SpeakingLevel.A2))

        composeRule.onNodeWithText(targetContext.getString(R.string.path_completion_promote, "A2")).assertIsEnabled()
        composeRule.onNodeWithText(text(R.string.path_completion_a2_notice_title)).assertExists()
    }

    @Test
    fun promotionButtonInvokesStartPromotion() {
        showScreen(PathCompletionUiState(level = SpeakingLevel.A1, nextLevel = SpeakingLevel.A2))

        composeRule.onNodeWithText(targetContext.getString(R.string.path_completion_promote, "A2")).performClick()

        assertEquals(1, promotionCount)
        assertEquals(0, reviewCount)
    }

    @Test
    fun intermediateLevelSecondaryActionReturnsToPath() {
        showScreen(PathCompletionUiState(level = SpeakingLevel.A2, nextLevel = SpeakingLevel.B1))

        composeRule.onNodeWithText(text(R.string.goal_assessment_return_to_path)).performClick()

        assertEquals(1, returnCount)
        assertEquals(0, freePracticeCount)
    }

    @Test
    fun promotionButtonIsDisabledWhenPromotionIsNotEnabled() {
        showScreen(
            state = PathCompletionUiState(level = SpeakingLevel.A1, nextLevel = SpeakingLevel.A2),
            isPromotionEnabled = false
        )

        composeRule.onNodeWithText(targetContext.getString(R.string.path_completion_promote, "A2"))
            .assertIsNotEnabled()
    }

    @Test
    fun highestLevelOffersReviewAndFreePractice() {
        showScreen(PathCompletionUiState(level = SpeakingLevel.B2))

        composeRule.onNodeWithText(text(R.string.path_completion_b2_notice_title)).assertExists()
        composeRule.onNodeWithText(targetContext.getString(R.string.path_completion_review, "B2")).performClick()
        composeRule.onNodeWithText(text(R.string.home_free_practice)).performClick()

        assertEquals(1, reviewCount)
        assertEquals(1, freePracticeCount)
        assertEquals(0, promotionCount)
    }

    @Test
    fun highestLevelReviewStaysEnabledEvenWhenPromotionIsDisabled() {
        showScreen(state = PathCompletionUiState(level = SpeakingLevel.B2), isPromotionEnabled = false)

        composeRule.onNodeWithText(targetContext.getString(R.string.path_completion_review, "B2")).assertIsEnabled()
    }

    @Test
    fun backButtonInvokesBack() {
        showScreen(PathCompletionUiState(level = SpeakingLevel.A1, nextLevel = SpeakingLevel.A2))

        composeRule.onNodeWithContentDescription(text(R.string.accessibility_back)).performClick()

        assertEquals(1, backCount)
    }

    private fun showScreen(state: PathCompletionUiState, isPromotionEnabled: Boolean = true) {
        composeRule.setContent {
            SpeakMateTheme {
                PathCompletionScreen(
                    state = state,
                    onBack = { backCount++ },
                    onStartPromotion = { promotionCount++ },
                    onReturnToPath = { returnCount++ },
                    onReviewPath = { reviewCount++ },
                    onFreePractice = { freePracticeCount++ },
                    isPromotionEnabled = isPromotionEnabled
                )
            }
        }
    }

    private fun text(resId: Int): String = targetContext.getString(resId)
}
