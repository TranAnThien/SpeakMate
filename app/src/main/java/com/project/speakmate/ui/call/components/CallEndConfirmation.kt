package com.project.speakmate.ui.call.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.ui.components.button.DialogActions
import com.project.speakmate.ui.components.dialog.MessageDialog

@Composable
fun CallEndConfirmation(
    description: String,
    dismissLabel: String,
    onConfirmEndCall: () -> Unit,
    onContinueCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    MessageDialog(
        title = stringResource(R.string.call_end_confirmation_title),
        description = description,
        onDismiss = onContinueCall,
        modifier = modifier
    ) {
        DialogActions(
            primaryText = stringResource(R.string.call_confirm_end),
            secondaryText = dismissLabel,
            onPrimaryAction = onConfirmEndCall,
            onSecondaryAction = onContinueCall,
            isDestructive = true
        )
    }
}
