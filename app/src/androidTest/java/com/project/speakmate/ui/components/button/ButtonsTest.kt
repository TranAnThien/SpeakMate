package com.project.speakmate.ui.components.button

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.project.speakmate.ui.theme.SpeakMateTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ButtonsTest {

    private companion object {
        const val PRIMARY = "Primary action"
        const val SECONDARY = "Secondary action"
        const val DESTRUCTIVE = "Destructive action"
    }

    @get:Rule
    val composeRule = createComposeRule()

    private var primaryCount = 0
    private var secondaryCount = 0

    @Test
    fun primaryButtonInvokesCallbackWhenEnabled() {
        composeRule.setContent { SpeakMateTheme { PrimaryButton(PRIMARY, { primaryCount++ }) } }

        composeRule.onNodeWithText(PRIMARY).assertIsEnabled().performClick()

        assertEquals(1, primaryCount)
    }

    @Test
    fun primaryButtonIsDisabledWhenEnabledIsFalse() {
        composeRule.setContent { SpeakMateTheme { PrimaryButton(PRIMARY, { primaryCount++ }, enabled = false) } }

        composeRule.onNodeWithText(PRIMARY).assertIsNotEnabled()
    }

    @Test
    fun primaryButtonIsDisabledWhileLoading() {
        composeRule.setContent { SpeakMateTheme { PrimaryButton(PRIMARY, { primaryCount++ }, isLoading = true) } }

        composeRule.onNodeWithContentDescription(PRIMARY).assertIsNotEnabled()
        composeRule.onNodeWithText(PRIMARY).assertDoesNotExist()
    }

    @Test
    fun secondaryButtonInvokesCallbackWhenEnabled() {
        composeRule.setContent { SpeakMateTheme { SecondaryButton(SECONDARY, { secondaryCount++ }) } }

        composeRule.onNodeWithText(SECONDARY).assertIsEnabled().performClick()

        assertEquals(1, secondaryCount)
    }

    @Test
    fun secondaryButtonIsDisabledWhenEnabledIsFalse() {
        composeRule.setContent { SpeakMateTheme { SecondaryButton(SECONDARY, { secondaryCount++ }, enabled = false) } }

        composeRule.onNodeWithText(SECONDARY).assertIsNotEnabled()
    }

    @Test
    fun destructiveButtonInvokesCallbackWhenEnabled() {
        composeRule.setContent { SpeakMateTheme { DestructiveButton(DESTRUCTIVE, { primaryCount++ }) } }

        composeRule.onNodeWithText(DESTRUCTIVE).assertIsEnabled().performClick()

        assertEquals(1, primaryCount)
    }

    @Test
    fun dialogActionsInvokePrimaryAndSecondaryCallbacksSeparately() {
        composeRule.setContent {
            SpeakMateTheme {
                DialogActions(
                    primaryText = PRIMARY,
                    secondaryText = SECONDARY,
                    onPrimaryAction = { primaryCount++ },
                    onSecondaryAction = { secondaryCount++ }
                )
            }
        }

        composeRule.onNodeWithText(PRIMARY).performClick()
        composeRule.onNodeWithText(SECONDARY).performClick()
        composeRule.onNodeWithText(SECONDARY).performClick()

        assertEquals(1, primaryCount)
        assertEquals(2, secondaryCount)
    }

    @Test
    fun destructiveDialogActionsUseDestructivePrimaryButton() {
        composeRule.setContent {
            SpeakMateTheme {
                DialogActions(
                    primaryText = DESTRUCTIVE,
                    secondaryText = SECONDARY,
                    onPrimaryAction = { primaryCount++ },
                    onSecondaryAction = { secondaryCount++ },
                    isDestructive = true
                )
            }
        }

        composeRule.onNodeWithText(DESTRUCTIVE).performClick()

        assertEquals(1, primaryCount)
        assertEquals(0, secondaryCount)
    }
}
