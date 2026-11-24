package com.project.speakmate.ui.call

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.project.speakmate.R
import com.project.speakmate.ui.call.state.MicrophonePermissionPresentation
import com.project.speakmate.ui.call.state.MicrophonePermissionUiState
import com.project.speakmate.ui.theme.SpeakMateTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MicrophonePermissionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private var requestCount = 0
    private var settingsCount = 0
    private var recheckCount = 0
    private var backCount = 0
    private var exitCount = 0

    @Test
    fun rationaleShowsExplanationAndAllowButtonRequestsPermission() {
        showScreen(MicrophonePermissionPresentation.RATIONALE)

        composeRule.onNodeWithText(text(R.string.call_permission_rationale_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.call_allow_microphone)).performClick()

        assertEquals(1, requestCount)
    }

    @Test
    fun rationaleBackButtonInvokesBack() {
        showScreen(MicrophonePermissionPresentation.RATIONALE)

        composeRule.onNodeWithText(text(R.string.call_back)).performClick()

        assertEquals(1, backCount)
        assertEquals(0, exitCount)
    }

    @Test
    fun deniedShowsOpenSettingsAndRecheckActions() {
        showScreen(MicrophonePermissionPresentation.DENIED)

        composeRule.onNodeWithText(text(R.string.call_permission_denied_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.call_open_settings)).performClick()
        composeRule.onNodeWithText(text(R.string.call_recheck_permission)).performClick()

        assertEquals(1, settingsCount)
        assertEquals(1, recheckCount)
    }

    @Test
    fun deniedDoesNotShowAllowMicrophoneButton() {
        showScreen(MicrophonePermissionPresentation.DENIED)

        composeRule.onNodeWithText(text(R.string.call_allow_microphone)).assertDoesNotExist()
    }

    @Test
    fun revokedShowsExitActionInsteadOfRecheck() {
        showScreen(MicrophonePermissionPresentation.REVOKED)

        composeRule.onNodeWithText(text(R.string.call_permission_revoked_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.call_recheck_permission)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.call_exit)).performClick()

        assertEquals(1, exitCount)
        assertEquals(0, recheckCount)
    }

    private fun showScreen(presentation: MicrophonePermissionPresentation) {
        composeRule.setContent {
            SpeakMateTheme {
                MicrophonePermissionScreen(
                    state = MicrophonePermissionUiState(presentation),
                    onRequestPermission = { requestCount++ },
                    onOpenSettings = { settingsCount++ },
                    onRecheckPermission = { recheckCount++ },
                    onBack = { backCount++ },
                    onExit = { exitCount++ }
                )
            }
        }
    }

    private fun text(resId: Int): String = targetContext.getString(resId)
}
