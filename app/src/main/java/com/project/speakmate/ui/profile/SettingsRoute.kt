package com.project.speakmate.ui.profile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.project.speakmate.R
import com.project.speakmate.domain.model.AppLanguage
import com.project.speakmate.ui.profile.state.SettingsEffect

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner, activity) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    SettingsEffect.RecreateActivity -> activity?.recreate()
                }
            }
        }
    }
    BackHandler(enabled = state.isSigningOut) {}
    BackHandler(enabled = state.isLanguageSelectionVisible, onBack = viewModel::hideLanguageSelection)
    SettingsScreen(
        state = state,
        languageLabel = stringResource(if (state.selectedLanguage == AppLanguage.ENGLISH)
            R.string.settings_language_english else R.string.settings_language_vietnamese),
        onBack = { if (!viewModel.uiState.value.isSigningOut) onBack() },
        onSelectLanguage = viewModel::showLanguageNotice,
        onOpenPrivacyPolicy = viewModel::showPrivacyNotice,
        onOpenTerms = viewModel::showTermsNotice,
        onRequestSignOut = viewModel::requestSignOut,
        onConfirmSignOut = viewModel::confirmSignOut,
        onCancelSignOut = viewModel::cancelSignOut,
        onDismissNotice = viewModel::dismissNotice,
        modifier = modifier,
        onChangeLanguage = viewModel::changeLanguage,
        onBackFromLanguage = viewModel::hideLanguageSelection,
        onRetryLanguage = viewModel::retryLanguage
    )
}
