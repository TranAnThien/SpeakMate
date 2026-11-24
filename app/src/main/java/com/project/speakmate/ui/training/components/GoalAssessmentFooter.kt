package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.button.SecondaryButton
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun GoalAssessmentFooter(
    primaryText: String,
    secondaryText: String,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimaryEnabled: Boolean = true,
    isSecondaryEnabled: Boolean = true
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(DesignColor.Surface)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PrimaryButton(text = primaryText, onClick = onPrimaryAction, enabled = isPrimaryEnabled)
        SecondaryButton(
            text = secondaryText,
            onClick = onSecondaryAction,
            enabled = isSecondaryEnabled,
            isTonal = true
        )
    }
}
