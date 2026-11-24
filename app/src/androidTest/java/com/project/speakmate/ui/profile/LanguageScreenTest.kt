package com.project.speakmate.ui.profile

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.project.speakmate.R
import com.project.speakmate.domain.model.AppLanguage
import com.project.speakmate.ui.theme.SpeakMateTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class LanguageScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private var selectedLanguage: AppLanguage? = null
    private var backCount = 0
    private var retryCount = 0

    @Test
    fun showsBothLanguageOptionsWithCurrentSelectionMarked() {
        showScreen(current = AppLanguage.ENGLISH)

        composeRule.onNodeWithText(text(R.string.settings_language_english)).assertIsSelected()
        composeRule.onNodeWithText(text(R.string.settings_language_vietnamese)).assertExists()
    }

    @Test
    fun selectingAnotherLanguageInvokesCallbackWithThatLanguage() {
        showScreen(current = AppLanguage.VIETNAMESE)

        composeRule.onNodeWithText(text(R.string.settings_language_english)).assertIsEnabled().performClick()

        assertEquals(AppLanguage.ENGLISH, selectedLanguage)
    }

    @Test
    fun loadingDisablesOptionsAndShowsLoadingText() {
        showScreen(isLoading = true)

        composeRule.onNodeWithText(text(R.string.language_loading)).assertExists()
        composeRule.onNodeWithText(text(R.string.settings_language_english)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.settings_language_vietnamese)).assertIsNotEnabled()
    }

    @Test
    fun savingDisablesOptionsAndShowsSavingText() {
        showScreen(isSaving = true)

        composeRule.onNodeWithText(text(R.string.language_saving)).assertExists()
        composeRule.onNodeWithText(text(R.string.settings_language_english)).assertIsNotEnabled()
        assertNull(selectedLanguage)
    }

    @Test
    fun saveFailureShowsMessageAndKeepsOptionsEnabled() {
        showScreen(isSaveFailed = true)

        composeRule.onNodeWithText(text(R.string.language_save_failed)).assertExists()
        composeRule.onNodeWithText(text(R.string.settings_language_english)).assertIsEnabled()
    }

    @Test
    fun loadErrorShowsMessageDisablesOptionsAndRetryInvokesCallback() {
        showScreen(hasLoadError = true)

        composeRule.onNodeWithText(text(R.string.language_load_failed)).assertExists()
        composeRule.onNodeWithText(text(R.string.settings_language_english)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.content_retry)).performClick()

        assertEquals(1, retryCount)
    }

    private fun showScreen(
        current: AppLanguage = AppLanguage.VIETNAMESE,
        isLoading: Boolean = false,
        isSaving: Boolean = false,
        isSaveFailed: Boolean = false,
        hasLoadError: Boolean = false
    ) {
        composeRule.setContent {
            SpeakMateTheme {
                LanguageScreen(
                    selectedLanguage = current,
                    isLoading = isLoading,
                    isSaving = isSaving,
                    isSaveFailed = isSaveFailed,
                    hasLoadError = hasLoadError,
                    onBack = { backCount++ },
                    onSelectLanguage = { selectedLanguage = it },
                    onRetry = { retryCount++ }
                )
            }
        }
    }

    private fun text(resId: Int): String = targetContext.getString(resId)
}
