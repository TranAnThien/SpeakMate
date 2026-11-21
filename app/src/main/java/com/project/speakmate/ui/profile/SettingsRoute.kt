package com.project.speakmate.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.R

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = state.isSigningOut) {}
    SettingsScreen(
        state = state.copy(languageLabel = stringResource(R.string.settings_language_vietnamese)),
        onBack = { if (!viewModel.uiState.value.isSigningOut) onBack() },
        onSelectLanguage = {},
        onOpenPrivacyPolicy = {},
        onOpenTerms = {},
        onRequestSignOut = viewModel::requestSignOut,
        onConfirmSignOut = viewModel::confirmSignOut,
        onCancelSignOut = viewModel::cancelSignOut,
        modifier = modifier
    )
}
