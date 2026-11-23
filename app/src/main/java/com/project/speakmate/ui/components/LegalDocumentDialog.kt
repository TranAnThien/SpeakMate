package com.project.speakmate.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R

@Composable
fun LegalDocumentDialog(document: LegalDocument, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    MessageDialog(stringResource(when (document) {
        LegalDocument.TERMS -> R.string.auth_terms
        LegalDocument.PRIVACY -> R.string.auth_privacy_policy
    }), stringResource(R.string.auth_legal_unavailable), onDismiss, modifier) {
        PrimaryButton(stringResource(R.string.content_close), onDismiss)
    }
}
