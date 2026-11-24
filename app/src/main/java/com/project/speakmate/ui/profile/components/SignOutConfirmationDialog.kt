package com.project.speakmate.ui.profile.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.project.speakmate.R
import com.project.speakmate.ui.components.button.DialogActions
import com.project.speakmate.ui.components.dialog.MessageDialog
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun SignOutConfirmationDialog(
    onConfirmSignOut: () -> Unit,
    onCancelSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    pendingCompletionCount: Int? = null
) {
    MessageDialog(
        title = stringResource(R.string.settings_sign_out_confirmation_title),
        description = stringResource(R.string.settings_sign_out_confirmation_description),
        onDismiss = onCancelSignOut,
        modifier = modifier,
        content =
            if (pendingCompletionCount == null || pendingCompletionCount > 0) {
                {
                    if (pendingCompletionCount == null) {
                        DetailText(
                            text = stringResource(R.string.settings_pending_unknown),
                            color = DesignColor.Error,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        DetailText(
                            text =
                                pluralStringResource(
                                    R.plurals.settings_pending_sessions,
                                    pendingCompletionCount,
                                    pendingCompletionCount
                                ),
                            color = DesignColor.Error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else null
    ) {
        DialogActions(
            primaryText =
                stringResource(
                    if (pendingCompletionCount == 0) R.string.settings_sign_out
                    else R.string.settings_discard_pending_sign_out
                ),
            secondaryText =
                stringResource(
                    if (pendingCompletionCount == 0) R.string.settings_cancel_sign_out
                    else R.string.settings_stay_signed_in
                ),
            onPrimaryAction = onConfirmSignOut,
            onSecondaryAction = onCancelSignOut,
            isDestructive = true
        )
    }
}
