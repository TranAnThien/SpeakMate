package com.project.speakmate.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.ui.training.state.TrainingActivityDestination
import com.project.speakmate.ui.training.state.TrainingProgressStatus

@Composable
fun TrainingPathRoute(
    onOpenGoal: (String) -> Unit,
    onOpenActivity: (TrainingActivityDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrainingPathViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TrainingPathScreen(
        state = state,
        onToggleGoal = viewModel::toggleGoalExpansion,
        onOpenGoal = onOpenGoal,
        onOpenUnit = onOpenActivity,
        onContinuePath = {
            viewModel.uiState.value.goals.asSequence().flatMap { it.units.asSequence() }
                .firstOrNull { it.status == TrainingProgressStatus.CURRENT && it.canOpen }
                ?.destination?.let(onOpenActivity)
        },
        onShowLevelInformation = {},
        onRetry = viewModel::retry,
        modifier = modifier
    )
}
