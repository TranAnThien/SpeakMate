package com.project.speakmate.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.AppLanguage
import com.project.speakmate.ui.components.dialog.LegalDocumentDialog
import com.project.speakmate.ui.components.header.BackAppBar
import com.project.speakmate.ui.components.state.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.profile.components.SettingsMenuRow
import com.project.speakmate.ui.profile.components.SignOutConfirmationDialog
import com.project.speakmate.ui.profile.state.SettingsDialog
import com.project.speakmate.ui.profile.state.SettingsUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onSelectLanguage: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    onRequestSignOut: () -> Unit,
    onConfirmSignOut: () -> Unit,
    onCancelSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissNotice: () -> Unit = {},
    onChangeLanguage: (AppLanguage) -> Unit = {},
    onBackFromLanguage: () -> Unit = {},
    onRetryLanguage: () -> Unit = {},
    languageLabel: String = ""
) {
    when (val dialog = state.activeDialog) {
        SettingsDialog.LanguageSelection -> {
            LanguageScreen(
                selectedLanguage = state.selectedLanguage,
                isLoading = state.isLanguageLoading,
                isSaving = state.isLanguageSaving,
                isSaveFailed = state.isLanguageSaveFailed,
                hasLoadError = state.hasLanguageLoadError,
                onBack = onBackFromLanguage,
                onSelectLanguage = onChangeLanguage,
                onRetry = onRetryLanguage,
                modifier = modifier
            )
            return
        }
        SettingsDialog.SignOutConfirmation ->
            SignOutConfirmationDialog(
                onConfirmSignOut = onConfirmSignOut,
                onCancelSignOut = onCancelSignOut,
                pendingCompletionCount = state.pendingCompletionCount
            )
        is SettingsDialog.Notice -> LegalDocumentDialog(dialog.document, onDismissNotice)
        null -> Unit
    }
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            BackAppBar(stringResource(R.string.settings_header), onBack)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                CaptionText(
                    text = stringResource(R.string.settings_application_heading),
                    style = DesignTypography.Label
                )
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsMenuRow(
                        iconRes = R.drawable.ic_profile_language,
                        title = stringResource(R.string.settings_language),
                        subtitle = languageLabel,
                        isDestructive = false,
                        onClick = onSelectLanguage,
                        enabled = !state.isSigningOut
                    )
                    SettingsMenuRow(
                        iconRes = R.drawable.ic_profile_privacy,
                        title = stringResource(R.string.auth_privacy_policy),
                        subtitle = "",
                        isDestructive = false,
                        onClick = onOpenPrivacyPolicy,
                        enabled = !state.isSigningOut
                    )
                    SettingsMenuRow(
                        iconRes = R.drawable.ic_profile_terms,
                        title = stringResource(R.string.auth_terms),
                        subtitle = "",
                        isDestructive = false,
                        onClick = onOpenTerms,
                        enabled = !state.isSigningOut
                    )
                }
                CaptionText(
                    text = stringResource(R.string.settings_account_heading),
                    style = DesignTypography.Label
                )
                if (state.isSigningOut) {
                    SpeakMateLoadingIndicator()
                    DetailText(text = stringResource(R.string.settings_signing_out))
                }
                if (state.isSignOutFailed) {
                    DetailText(text = stringResource(R.string.settings_sign_out_failed), color = DesignColor.Error)
                }
                Column(Modifier.fillMaxWidth()) {
                    SettingsMenuRow(
                        iconRes = R.drawable.ic_profile_sign_out,
                        title = stringResource(R.string.settings_sign_out),
                        subtitle = stringResource(R.string.settings_sign_out_description),
                        isDestructive = true,
                        onClick = onRequestSignOut,
                        enabled = !state.isSigningOut
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
