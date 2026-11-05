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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.BackAppBar
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
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        Column(Modifier.fillMaxSize()) {
            BackAppBar(stringResource(R.string.settings_header), onBack)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(stringResource(R.string.settings_application_heading), style = DesignTypography.Label, color = DesignColor.Muted)
                Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsMenuRow(R.drawable.ic_profile_language, stringResource(R.string.settings_language), state.languageLabel, false, onSelectLanguage)
                    SettingsMenuRow(R.drawable.ic_profile_privacy, stringResource(R.string.auth_privacy_policy), "", false, onOpenPrivacyPolicy)
                    SettingsMenuRow(R.drawable.ic_profile_terms, stringResource(R.string.auth_terms), "", false, onOpenTerms)
                }
                Text(stringResource(R.string.settings_account_heading), style = DesignTypography.Label, color = DesignColor.Muted)
                Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    SettingsMenuRow(R.drawable.ic_profile_sign_out, stringResource(R.string.settings_sign_out),
                        stringResource(R.string.settings_sign_out_description), true, onRequestSignOut)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (state.isSignOutConfirmationVisible) SignOutConfirmationDialog(onConfirmSignOut, onCancelSignOut)
    }
}
