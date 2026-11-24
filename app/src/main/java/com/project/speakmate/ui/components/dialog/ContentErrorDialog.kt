package com.project.speakmate.ui.components.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.ui.components.button.DialogActions
import com.project.speakmate.ui.components.button.SecondaryButton
import com.project.speakmate.ui.components.image.CircleIcon
import com.project.speakmate.utils.extension.default

@Composable
fun ContentErrorDialog(
    title: String,
    description: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    primaryText: String? = null
) {
    MessageDialog(
        title = title,
        description = description,
        onDismiss = onDismiss,
        modifier = modifier,
        icon = {
            CircleIcon(iconRes = R.drawable.ic_load_error)
        }
    ) {
        DialogActions(
            primaryText = primaryText.default(stringResource(R.string.content_reload)),
            secondaryText = stringResource(R.string.content_dismiss_notice),
            onPrimaryAction = onRetry,
            onSecondaryAction = onDismiss
        )
    }
}
