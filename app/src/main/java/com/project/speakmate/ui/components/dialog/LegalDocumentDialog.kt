package com.project.speakmate.ui.components.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.ui.components.button.DialogActions

@Composable
fun LegalDocumentDialog(document: LegalDocument, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    MessageDialog(
        title =
            stringResource(
                when (document) {
                    LegalDocument.TERMS -> R.string.auth_terms
                    LegalDocument.PRIVACY -> R.string.auth_privacy_policy
                }
            ),
        description = stringResource(R.string.auth_legal_unavailable),
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        DialogActions(primaryText = stringResource(R.string.content_close), onPrimaryAction = onDismiss)
    }
}
