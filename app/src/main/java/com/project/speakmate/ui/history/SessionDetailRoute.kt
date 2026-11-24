package com.project.speakmate.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SessionDetailRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SessionDetailScreen(
        state = state,
        onBack = onBack,
        onRetrySession = viewModel::retry,
        onRetryTranscript = viewModel::retryTranscript,
        onReturnToHistory = onBack,
        onRetryGrading = viewModel::retryGrading,
        onSaveUnassessed = viewModel::saveUnassessed,
        onRetrySave = viewModel::retrySave,
        modifier = modifier
    )
}
