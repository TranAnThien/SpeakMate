package com.project.speakmate.ui.practice

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PracticeSummaryRoute(
    onDone: () -> Unit,
    onViewSavedTranscript: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PracticeSummaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler { if (state.isTranscriptVisible) viewModel.hideTranscript() else onDone() }
    PracticeSummaryScreen(state, onDone, {
        viewModel.openTranscript()?.let(onViewSavedTranscript)
    }, onDone, modifier, onRetrySave = viewModel::retrySave, onCloseTranscript = viewModel::hideTranscript, onRetry = viewModel::retry)
}
