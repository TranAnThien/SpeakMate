package com.project.speakmate.ui.history.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.ui.components.DialogActions
import com.project.speakmate.ui.components.MessageDialog

@Composable
fun HistoryPendingDiscardDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    MessageDialog(stringResource(R.string.history_discard_title),
        stringResource(R.string.history_discard_description), onDismiss) {
        DialogActions(stringResource(R.string.history_recovery_discard), stringResource(R.string.history_discard_keep),
            onConfirm, onDismiss, isDestructive = true)
    }
}
