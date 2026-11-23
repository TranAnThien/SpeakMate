package com.project.speakmate.ui.training.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.ui.components.MessageDialog
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.training.state.TrainingPathNotice
import com.project.speakmate.ui.training.state.TrainingUnitUiState
import com.project.speakmate.utils.extension.default

@Composable
fun TrainingPathNoticeDialog(unit: TrainingUnitUiState?, notice: TrainingPathNotice?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    if (notice == null) return
    MessageDialog(
        unit?.title?.takeIf { it.isNotBlank() }.default(stringResource(when (notice) {
            TrainingPathNotice.LOCKED -> R.string.training_locked
            TrainingPathNotice.STALE -> R.string.training_stale_title
        })),
        listOfNotNull(unit?.description?.takeIf(String::isNotBlank), stringResource(when (notice) {
            TrainingPathNotice.LOCKED -> R.string.training_locked_explanation
            TrainingPathNotice.STALE -> R.string.training_stale_description
        })).joinToString("\n"), onDismiss, modifier
    ) {
        PrimaryButton(stringResource(R.string.content_close), onDismiss)
    }
}
