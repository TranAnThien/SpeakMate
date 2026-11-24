package com.project.speakmate.ui.components.header

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

class BackAppBarTest {

    private companion object {
        const val TITLE = "Screen title"
    }

    @get:Rule
    val composeRule = createComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private var backCount = 0

    @Test
    fun showsTitleAndBackActionThatInvokesCallback() {
        composeRule.setContent { SpeakMateTheme { BackAppBar(title = TITLE, onBack = { backCount++ }) } }

        composeRule.onNodeWithText(TITLE).assertExists()
        composeRule.onNodeWithContentDescription(targetContext.getString(R.string.accessibility_back))
            .assertIsEnabled()
            .performClick()

        assertEquals(1, backCount)
    }

    @Test
    fun closeVariantShowsCloseLabelInsteadOfBack() {
        composeRule.setContent {
            SpeakMateTheme { BackAppBar(title = TITLE, onBack = { backCount++ }, isClose = true) }
        }

        composeRule.onNodeWithContentDescription(targetContext.getString(R.string.accessibility_close))
            .performClick()
        composeRule.onNodeWithContentDescription(targetContext.getString(R.string.accessibility_back))
            .assertDoesNotExist()

        assertEquals(1, backCount)
    }

    @Test
    fun backActionIsDisabledWhenBackIsNotEnabled() {
        composeRule.setContent {
            SpeakMateTheme { BackAppBar(title = TITLE, onBack = { backCount++ }, isBackEnabled = false) }
        }

        composeRule.onNodeWithContentDescription(targetContext.getString(R.string.accessibility_back))
            .assertIsNotEnabled()
    }
}
