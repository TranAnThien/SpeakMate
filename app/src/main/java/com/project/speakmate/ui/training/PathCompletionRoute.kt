package com.project.speakmate.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.ui.training.components.TrainingUnavailableContent

@Composable
fun PathCompletionRoute(
    onBack: () -> Unit,
    onReturnToPath: () -> Unit,
    onFreePractice: () -> Unit,
    onStartPromotion: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PathCompletionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.isLoading || state.hasLoadError || !state.isCompleted) {
        TrainingUnavailableContent(
            isLoading = state.isLoading,
            onRetry = if (state.hasLoadError) viewModel::retry else onReturnToPath,
            modifier = modifier
        )
        return
    }
    PathCompletionScreen(
        state = state,
        onBack = onBack,
        onStartPromotion = onStartPromotion,
        onReturnToPath = onReturnToPath,
        onReviewPath = onReturnToPath,
        onFreePractice = onFreePractice,
        modifier = modifier,
        isPromotionEnabled = state.nextLevel != null
    )
}
