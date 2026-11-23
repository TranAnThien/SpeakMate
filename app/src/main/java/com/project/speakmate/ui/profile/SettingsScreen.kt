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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.AppLanguage
import com.project.speakmate.ui.components.BackAppBar
import com.project.speakmate.ui.components.LegalDocumentDialog
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
import com.project.speakmate.ui.profile.components.SettingsMenuRow
import com.project.speakmate.ui.profile.components.SignOutConfirmationDialog
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
    if (state.isLanguageSelectionVisible) {
        LanguageScreen(state.selectedLanguage, state.isLanguageLoading, state.isLanguageSaving, state.isLanguageSaveFailed,
            state.hasLanguageLoadError, onBackFromLanguage, onChangeLanguage, onRetryLanguage, modifier)
        return
    }
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            BackAppBar(stringResource(R.string.settings_header), onBack)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(stringResource(R.string.settings_application_heading), style = DesignTypography.Label, color = DesignColor.Muted)
                Column(Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsMenuRow(R.drawable.ic_profile_language, stringResource(R.string.settings_language), languageLabel, false, onSelectLanguage,
                        enabled = !state.isSigningOut)
                    SettingsMenuRow(R.drawable.ic_profile_privacy, stringResource(R.string.auth_privacy_policy), "", false, onOpenPrivacyPolicy, enabled = !state.isSigningOut)
                    SettingsMenuRow(R.drawable.ic_profile_terms, stringResource(R.string.auth_terms), "", false, onOpenTerms, enabled = !state.isSigningOut)
                }
                Text(stringResource(R.string.settings_account_heading), style = DesignTypography.Label, color = DesignColor.Muted)
                if (state.isSigningOut) {
                    SpeakMateLoadingIndicator()
                    Text(stringResource(R.string.settings_signing_out), style = DesignTypography.Detail, color = DesignColor.Muted)
                }
                if (state.isSignOutFailed) {
                    Text(stringResource(R.string.settings_sign_out_failed), style = DesignTypography.Detail, color = DesignColor.Error)
                }
                Column(Modifier.fillMaxWidth()) {
                    SettingsMenuRow(R.drawable.ic_profile_sign_out, stringResource(R.string.settings_sign_out),
                        stringResource(R.string.settings_sign_out_description), true, onRequestSignOut, enabled = !state.isSigningOut)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (state.isSignOutConfirmationVisible) SignOutConfirmationDialog(onConfirmSignOut, onCancelSignOut,
            pendingCompletionCount = state.pendingCompletionCount)
        state.notice?.let { LegalDocumentDialog(it, onDismissNotice) }
    }
}
