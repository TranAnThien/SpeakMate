package com.project.speakmate.ui.components.dialog

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.project.speakmate.R
import com.project.speakmate.ui.theme.SpeakMateTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ContentErrorDialogTest {

    private companion object {
        const val TITLE = "Load failed"
        const val DESCRIPTION = "Could not load the content."
        const val CUSTOM_ACTION = "Try once more"
    }

    @get:Rule
    val composeRule = createComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private var retryCount = 0
    private var dismissCount = 0

    @Test
    fun showsTitleAndDescription() {
        showDialog()

        composeRule.onNodeWithText(TITLE).assertExists()
        composeRule.onNodeWithText(DESCRIPTION).assertExists()
    }

    @Test
    fun defaultPrimaryActionInvokesRetry() {
        showDialog()

        composeRule.onNodeWithText(targetContext.getString(R.string.content_reload)).performClick()

        assertEquals(1, retryCount)
        assertEquals(0, dismissCount)
    }

    @Test
    fun secondaryActionInvokesDismiss() {
        showDialog()

        composeRule.onNodeWithText(targetContext.getString(R.string.content_dismiss_notice)).performClick()

        assertEquals(0, retryCount)
        assertEquals(1, dismissCount)
    }

    @Test
    fun customPrimaryTextReplacesDefaultLabel() {
        showDialog(primaryText = CUSTOM_ACTION)

        composeRule.onNodeWithText(CUSTOM_ACTION).performClick()
        composeRule.onNodeWithText(targetContext.getString(R.string.content_reload)).assertDoesNotExist()

        assertEquals(1, retryCount)
    }

    private fun showDialog(primaryText: String? = null) {
        composeRule.setContent {
            SpeakMateTheme {
                ContentErrorDialog(
                    title = TITLE,
                    description = DESCRIPTION,
                    onRetry = { retryCount++ },
                    onDismiss = { dismissCount++ },
                    primaryText = primaryText
                )
            }
        }
    }
}
