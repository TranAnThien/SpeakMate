package com.project.speakmate.ui.placement

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.project.speakmate.R
import com.project.speakmate.ui.theme.SpeakMateTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlacementIntroductionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private var backCount = 0
    private var startCount = 0
    private var skipCount = 0

    @Test
    fun showsTitleAndStartAndLaterActions() {
        showScreen()

        composeRule.onNodeWithText(text(R.string.placement_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.placement_start)).assertIsEnabled()
        composeRule.onNodeWithText(text(R.string.placement_later)).assertIsEnabled()
    }

    @Test
    fun startButtonInvokesStartAssessment() {
        showScreen()

        composeRule.onNodeWithText(text(R.string.placement_start)).performClick()

        assertEquals(1, startCount)
        assertEquals(0, skipCount)
    }

    @Test
    fun laterButtonInvokesSkipAssessment() {
        showScreen()

        composeRule.onNodeWithText(text(R.string.placement_later)).performClick()

        assertEquals(1, skipCount)
        assertEquals(0, startCount)
    }

    @Test
    fun backButtonInvokesBack() {
        showScreen()

        composeRule.onNodeWithContentDescription(text(R.string.accessibility_back)).performClick()

        assertEquals(1, backCount)
    }

    @Test
    fun savingDisablesActionsAndShowsSavingLabel() {
        showScreen(isSaving = true)

        composeRule.onNodeWithText(text(R.string.placement_start)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.placement_saving)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.placement_later)).assertDoesNotExist()
    }

    @Test
    fun saveFailureShowsErrorMessageAndKeepsActionsEnabled() {
        showScreen(hasSaveFailed = true)

        composeRule.onNodeWithText(text(R.string.placement_save_failed)).assertExists()
        composeRule.onNodeWithText(text(R.string.placement_start)).assertIsEnabled()
    }

    @Test
    fun saveFailureMessageIsHiddenByDefault() {
        showScreen()

        composeRule.onNodeWithText(text(R.string.placement_save_failed)).assertDoesNotExist()
    }

    private fun showScreen(hasSaveFailed: Boolean = false, isSaving: Boolean = false) {
        composeRule.setContent {
            SpeakMateTheme {
                PlacementIntroductionScreen(
                    onBack = { backCount++ },
                    onStartAssessment = { startCount++ },
                    onSkipAssessment = { skipCount++ },
                    hasSaveFailed = hasSaveFailed,
                    isSaving = isSaving
                )
            }
        }
    }

    private fun text(resId: Int): String = targetContext.getString(resId)
}
