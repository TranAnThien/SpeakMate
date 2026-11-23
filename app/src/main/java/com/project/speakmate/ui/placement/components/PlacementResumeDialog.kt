package com.project.speakmate.ui.placement.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.ui.components.DialogActions
import com.project.speakmate.ui.components.MessageDialog

@Composable
fun PlacementResumeDialog(onResume: () -> Unit, onRestart: () -> Unit, modifier: Modifier = Modifier, onDismiss: () -> Unit = {}) {
    MessageDialog(
        stringResource(R.string.placement_resume_title),
        stringResource(R.string.placement_resume_description),
        onDismiss,
        modifier,
        shouldDismissOnClickOutside = true
    ) {
        DialogActions(stringResource(R.string.placement_resume), stringResource(R.string.placement_restart), onResume, onRestart)
    }
}
