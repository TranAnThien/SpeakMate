package com.project.speakmate.ui.placement.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.ui.components.button.DialogActions
import com.project.speakmate.ui.components.dialog.MessageDialog

@Composable
fun PlacementResumeDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    MessageDialog(
        title = stringResource(R.string.placement_resume_title),
        description = stringResource(R.string.placement_resume_description),
        onDismiss = onDismiss,
        modifier = modifier,
        shouldDismissOnClickOutside = true
    ) {
        DialogActions(
            primaryText = stringResource(R.string.placement_resume),
            secondaryText = stringResource(R.string.placement_restart),
            onPrimaryAction = onResume,
            onSecondaryAction = onRestart
        )
    }
}
