package com.project.speakmate.ui.auth

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.project.speakmate.R
import com.project.speakmate.ui.auth.state.GoogleSignInStatus
import com.project.speakmate.ui.auth.state.GoogleSignInUiState
import com.project.speakmate.ui.theme.SpeakMateTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GoogleSignInScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private var signInCount = 0
    private var termsCount = 0
    private var privacyCount = 0
    private var retryProfileCount = 0
    private var signOutCount = 0

    @Test
    fun normalStateShowsContinueButtonAndInvokesSignIn() {
        showScreen(readyState(GoogleSignInStatus.NORMAL))

        composeRule.onNodeWithText(text(R.string.auth_continue_with_google)).assertIsEnabled().performClick()

        assertEquals(1, signInCount)
    }

    @Test
    fun loadingStateDisablesButtonAndShowsProgressText() {
        showScreen(readyState(GoogleSignInStatus.LOADING))

        composeRule.onNodeWithText(text(R.string.auth_signing_in)).assertIsNotEnabled()
    }

    @Test
    fun signingOutStateDisablesButton() {
        showScreen(readyState(GoogleSignInStatus.SIGNING_OUT))

        composeRule.onNodeWithText(text(R.string.auth_signing_out)).assertIsNotEnabled()
    }

    @Test
    fun errorStateShowsMessageAndRetryButton() {
        showScreen(readyState(GoogleSignInStatus.ERROR))

        composeRule.onNodeWithText(text(R.string.auth_sign_in_error)).assertExists()
        composeRule.onNodeWithText(text(R.string.content_retry)).assertIsEnabled().performClick()

        assertEquals(1, signInCount)
    }

    @Test
    fun offlineStateShowsOfflineMessage() {
        showScreen(readyState(GoogleSignInStatus.OFFLINE))

        composeRule.onNodeWithText(text(R.string.auth_offline)).assertExists()
    }

    @Test
    fun cancelledStateShowsCancelledMessage() {
        showScreen(readyState(GoogleSignInStatus.CANCELLED))

        composeRule.onNodeWithText(text(R.string.auth_cancelled)).assertExists()
    }

    @Test
    fun termsAndPrivacyButtonsInvokeTheirCallbacks() {
        showScreen(readyState(GoogleSignInStatus.NORMAL))

        composeRule.onNodeWithText(text(R.string.auth_terms)).performClick()
        composeRule.onNodeWithText(text(R.string.auth_privacy_policy)).performClick()

        assertEquals(1, termsCount)
        assertEquals(1, privacyCount)
    }

    @Test
    fun checkingProfileShowsLoadingContentWithoutActions() {
        showScreen(GoogleSignInUiState(isCheckingProfile = true))

        composeRule.onNodeWithText(text(R.string.auth_profile_loading_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.content_retry)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.auth_continue_with_google)).assertDoesNotExist()
    }

    @Test
    fun profileErrorShowsRetryAndSignOutActions() {
        showScreen(GoogleSignInUiState(status = GoogleSignInStatus.PROFILE_ERROR, isCheckingProfile = false))

        composeRule.onNodeWithText(text(R.string.auth_profile_error_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.content_retry)).performClick()
        composeRule.onNodeWithText(text(R.string.settings_sign_out)).performClick()

        assertEquals(1, retryProfileCount)
        assertEquals(1, signOutCount)
    }

    private fun showScreen(state: GoogleSignInUiState) {
        composeRule.setContent {
            SpeakMateTheme {
                GoogleSignInScreen(
                    state = state,
                    onSignIn = { signInCount++ },
                    onOpenTerms = { termsCount++ },
                    onOpenPrivacyPolicy = { privacyCount++ },
                    onRetryProfile = { retryProfileCount++ },
                    onSignOut = { signOutCount++ },
                    onDismissLegalInformation = {}
                )
            }
        }
    }

    private fun readyState(status: GoogleSignInStatus): GoogleSignInUiState =
        GoogleSignInUiState(status = status, isCheckingProfile = false)

    private fun text(resId: Int): String = targetContext.getString(resId)
}
