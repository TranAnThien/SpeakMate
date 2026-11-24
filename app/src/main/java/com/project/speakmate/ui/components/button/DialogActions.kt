package com.project.speakmate.ui.components.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun DialogActions(
    primaryText: String,
    secondaryText: String,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    if (LocalDensity.current.fontScale > 1f) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isDestructive) DestructiveButton(primaryText, onPrimaryAction)
            else PrimaryButton(primaryText, onPrimaryAction)
            SecondaryButton(secondaryText, onSecondaryAction)
        }
    } else {
        Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(
                text = secondaryText,
                onClick = onSecondaryAction,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            if (isDestructive) {
                DestructiveButton(
                    text = primaryText,
                    onClick = onPrimaryAction,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            } else {
                PrimaryButton(
                    text = primaryText,
                    onClick = onPrimaryAction,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun DialogActions(primaryText: String, onPrimaryAction: () -> Unit, modifier: Modifier = Modifier) {
    PrimaryButton(text = primaryText, onClick = onPrimaryAction, modifier = modifier)
}
