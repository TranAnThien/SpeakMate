package com.project.speakmate.ui.history.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.ui.components.button.DialogActions
import com.project.speakmate.ui.components.dialog.MessageDialog

@Composable
fun HistoryPendingDiscardDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    MessageDialog(
        title = stringResource(R.string.history_discard_title),
        description = stringResource(R.string.history_discard_description),
        onDismiss = onDismiss
    ) {
        DialogActions(
            primaryText = stringResource(R.string.history_recovery_discard),
            secondaryText = stringResource(R.string.history_discard_keep),
            onPrimaryAction = onConfirm,
            onSecondaryAction = onDismiss,
            isDestructive = true
        )
    }
}
