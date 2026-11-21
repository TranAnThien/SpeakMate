package com.project.speakmate.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.utils.AppConstant

@Composable
fun HistoryRoute(
    onOpenSession: (String) -> Unit,
    onOpenPractice: () -> Unit,
    onOpenTrainingPath: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        onSelectFilter = viewModel::selectFilter,
        onOpenSession = onOpenSession,
        onStartPractice = {
            if (viewModel.uiState.value.selectedFilterId == AppConstant.HISTORY_FILTER_FREE_PRACTICE) onOpenPractice()
            else onOpenTrainingPath()
        },
        onRetry = viewModel::retry,
        onRetrySave = viewModel::retrySave,
        onDismissError = {},
        onReturnToPath = onOpenTrainingPath,
        modifier = modifier
    )
}
