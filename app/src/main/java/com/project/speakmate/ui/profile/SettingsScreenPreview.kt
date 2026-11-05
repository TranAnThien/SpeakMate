package com.project.speakmate.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.project.speakmate.R
import com.project.speakmate.ui.profile.state.SettingsUiState
import com.project.speakmate.ui.theme.SpeakMateTheme

@Preview(name = "Settings / Source / 17:811", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    SpeakMateTheme {
        SettingsScreen(
            state = SettingsUiState(
                languageLabel = stringResource(R.string.settings_preview_language),
                isSignOutConfirmationVisible = false
            ),
            onBack = {},
            onSelectLanguage = {},
            onOpenPrivacyPolicy = {},
            onOpenTerms = {},
            onRequestSignOut = {},
            onConfirmSignOut = {},
            onCancelSignOut = {}
        )
    }
}

@Preview(name = "UX State Additions • Settings • Sign-out confirmation • UX Proposal / 33:2582", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun SettingsSignOutConfirmationScreenPreview() {
    SpeakMateTheme {
        SettingsScreen(
            state = SettingsUiState(
                languageLabel = stringResource(R.string.settings_preview_language),
                isSignOutConfirmationVisible = true
            ),
            onBack = {},
            onSelectLanguage = {},
            onOpenPrivacyPolicy = {},
            onOpenTerms = {},
            onRequestSignOut = {},
            onConfirmSignOut = {},
            onCancelSignOut = {}
        )
    }
}
