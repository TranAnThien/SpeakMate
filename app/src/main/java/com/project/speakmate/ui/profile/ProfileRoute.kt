package com.project.speakmate.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileRoute(
    onOpenSettings: () -> Unit,
    onOpenVocabulary: () -> Unit,
    onStartPlacement: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        state = state,
        onOpenSettings = onOpenSettings,
        onOpenVocabulary = onOpenVocabulary,
        onRetryProfile = viewModel::retryProfile,
        onStartPlacement = onStartPlacement,
        modifier = modifier
    )
}
