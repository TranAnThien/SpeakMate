package com.project.speakmate.ui.profile.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.project.speakmate.R
import com.project.speakmate.ui.components.DialogActions
import com.project.speakmate.ui.components.MessageDialog
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun SignOutConfirmationDialog(
    onConfirmSignOut: () -> Unit,
    onCancelSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    pendingCompletionCount: Int? = null
) {
    MessageDialog(
        stringResource(R.string.settings_sign_out_confirmation_title),
        stringResource(R.string.settings_sign_out_confirmation_description), onCancelSignOut, modifier,
        content = if (pendingCompletionCount == null || pendingCompletionCount > 0) {
            {
                if (pendingCompletionCount == null) {
                    Text(stringResource(R.string.settings_pending_unknown), style = DesignTypography.Detail,
                        color = DesignColor.Error, textAlign = TextAlign.Center)
                } else {
                    Text(pluralStringResource(R.plurals.settings_pending_sessions, pendingCompletionCount, pendingCompletionCount),
                        style = DesignTypography.Detail, color = DesignColor.Error, textAlign = TextAlign.Center)
                }
            }
        } else null
    ) {
        DialogActions(stringResource(if (pendingCompletionCount == 0) R.string.settings_sign_out else R.string.settings_discard_pending_sign_out),
            stringResource(if (pendingCompletionCount == 0) R.string.settings_cancel_sign_out else R.string.settings_stay_signed_in),
            onConfirmSignOut, onCancelSignOut, isDestructive = true)
    }
}
