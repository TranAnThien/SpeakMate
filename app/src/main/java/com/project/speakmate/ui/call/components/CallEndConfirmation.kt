package com.project.speakmate.ui.call.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.ui.components.DialogActions
import com.project.speakmate.ui.components.MessageDialog

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
        DialogActions(stringResource(R.string.call_confirm_end), dismissLabel, onConfirmEndCall, onContinueCall, isDestructive = true)
    }
}
