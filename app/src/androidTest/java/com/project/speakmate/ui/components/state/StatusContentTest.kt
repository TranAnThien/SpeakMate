package com.project.speakmate.ui.components.state

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.project.speakmate.ui.theme.SpeakMateTheme
import org.junit.Rule
import org.junit.Test

class StatusContentTest {

    private companion object {
        const val TITLE = "Status title"
        const val DESCRIPTION = "Status description"
        const val ACTION = "Status action"
        const val ILLUSTRATION = "Status illustration"
    }

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun titleAndDescriptionAreDisplayed() {
        composeRule.setContent {
            SpeakMateTheme {
                StatusContent(title = TITLE, description = DESCRIPTION)
            }
        }

        composeRule.onNodeWithText(TITLE).assertExists()
        composeRule.onNodeWithText(DESCRIPTION).assertExists()
    }

    @Test
    fun emptyTitleAndDescriptionAreNotRendered() {
        composeRule.setContent {
            SpeakMateTheme {
                StatusContent(
                    title = "",
                    description = "",
                    actions = { Text(ACTION) }
                )
            }
        }

        composeRule.onNodeWithText(ACTION).assertExists()
        composeRule.onNodeWithText(TITLE).assertDoesNotExist()
        composeRule.onNodeWithText(DESCRIPTION).assertDoesNotExist()
    }

    @Test
    fun illustrationAndActionsAreRendered() {
        composeRule.setContent {
            SpeakMateTheme {
                Column {
                    StatusContent(
                        title = TITLE,
                        description = DESCRIPTION,
                        illustration = { Text(ILLUSTRATION) },
                        actions = { Text(ACTION) }
                    )
                }
            }
        }

        composeRule.onNodeWithText(ILLUSTRATION).assertExists()
        composeRule.onNodeWithText(ACTION).assertExists()
    }
}
