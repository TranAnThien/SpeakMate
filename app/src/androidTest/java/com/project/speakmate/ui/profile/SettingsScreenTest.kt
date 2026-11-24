package com.project.speakmate.ui.profile

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.project.speakmate.R
import com.project.speakmate.ui.profile.state.SettingsDialog
import com.project.speakmate.ui.profile.state.SettingsUiState
import com.project.speakmate.ui.theme.SpeakMateTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    private companion object {
        const val LANGUAGE_LABEL = "English"
    }

    @get:Rule
    val composeRule = createComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private var backCount = 0
    private var languageCount = 0
    private var privacyCount = 0
    private var termsCount = 0
    private var requestSignOutCount = 0
    private var confirmSignOutCount = 0
    private var cancelSignOutCount = 0

    @Test
    fun showsSettingsHeaderAndLanguageLabel() {
        showScreen(SettingsUiState())

        composeRule.onNodeWithText(text(R.string.settings_header)).assertExists()
        composeRule.onNodeWithText(LANGUAGE_LABEL).assertExists()
    }

    @Test
    fun menuRowsInvokeTheirCallbacks() {
        showScreen(SettingsUiState())

        composeRule.onNodeWithText(text(R.string.settings_language)).performClick()
        composeRule.onNodeWithText(text(R.string.auth_privacy_policy)).performClick()
        composeRule.onNodeWithText(text(R.string.auth_terms)).performClick()
        composeRule.onNodeWithText(text(R.string.settings_sign_out)).performClick()

        assertEquals(1, languageCount)
        assertEquals(1, privacyCount)
        assertEquals(1, termsCount)
        assertEquals(1, requestSignOutCount)
    }

    @Test
    fun backButtonInvokesBack() {
        showScreen(SettingsUiState())

        composeRule.onNodeWithContentDescription(text(R.string.accessibility_back)).performClick()

        assertEquals(1, backCount)
    }

    @Test
    fun signingOutDisablesMenuRowsAndShowsProgressText() {
        showScreen(SettingsUiState(isSigningOut = true))

        composeRule.onNodeWithText(text(R.string.settings_signing_out)).assertExists()
        composeRule.onNodeWithText(text(R.string.settings_language)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.settings_sign_out)).assertIsNotEnabled()
    }

    @Test
    fun signOutFailureShowsErrorAndKeepsSignOutEnabled() {
        showScreen(SettingsUiState(isSignOutFailed = true))

        composeRule.onNodeWithText(text(R.string.settings_sign_out_failed)).assertExists()
        composeRule.onNodeWithText(text(R.string.settings_sign_out)).assertIsEnabled()
    }

    @Test
    fun confirmationDialogWithoutPendingSessionsConfirmsAndCancels() {
        showScreen(
            SettingsUiState(activeDialog = SettingsDialog.SignOutConfirmation, pendingCompletionCount = 0)
        )

        composeRule.onNodeWithText(text(R.string.settings_sign_out_confirmation_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.settings_cancel_sign_out)).performClick()

        assertEquals(1, cancelSignOutCount)
        assertEquals(0, confirmSignOutCount)
    }

    @Test
    fun confirmationDialogWithPendingSessionsWarnsAndOffersDiscard() {
        showScreen(
            SettingsUiState(activeDialog = SettingsDialog.SignOutConfirmation, pendingCompletionCount = 2)
        )

        composeRule.onNodeWithText(text(R.string.settings_stay_signed_in)).assertExists()
        composeRule.onNodeWithText(text(R.string.settings_discard_pending_sign_out)).performClick()

        assertEquals(1, confirmSignOutCount)
        assertEquals(0, cancelSignOutCount)
    }

    @Test
    fun confirmationDialogWithUnknownPendingCountShowsUnknownWarning() {
        showScreen(
            SettingsUiState(activeDialog = SettingsDialog.SignOutConfirmation, pendingCompletionCount = null)
        )

        composeRule.onNodeWithText(text(R.string.settings_pending_unknown)).assertExists()
    }

    private fun showScreen(state: SettingsUiState) {
        composeRule.setContent {
            SpeakMateTheme {
                SettingsScreen(
                    state = state,
                    onBack = { backCount++ },
                    onSelectLanguage = { languageCount++ },
                    onOpenPrivacyPolicy = { privacyCount++ },
                    onOpenTerms = { termsCount++ },
                    onRequestSignOut = { requestSignOutCount++ },
                    onConfirmSignOut = { confirmSignOutCount++ },
                    onCancelSignOut = { cancelSignOutCount++ },
                    languageLabel = LANGUAGE_LABEL
                )
            }
        }
    }

    private fun text(resId: Int): String = targetContext.getString(resId)
}
