package com.project.speakmate.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.project.speakmate.ui.training.state.TrainingActivityDestination
import com.project.speakmate.ui.training.state.TrainingPathEffect

@Composable
fun TrainingPathRoute(
    onOpenActivity: (TrainingActivityDestination) -> Unit,
    onStartPlacement: () -> Unit,
    onFreePractice: () -> Unit,
    onViewCompletedPath: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrainingPathViewModel = hiltViewModel(),
    goalId: String? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel, goalId, state.goals.isNotEmpty()) {
        if (goalId != null && state.goals.isNotEmpty()) viewModel.showRequestedGoal(goalId)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val openActivity by rememberUpdatedState(onOpenActivity)
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) { is TrainingPathEffect.OpenActivity -> openActivity(effect.destination) }
            }
        }
    }
    TrainingPathScreen(
        state = state,
        onToggleGoal = viewModel::toggleGoalExpansion,
        onOpenGoal = viewModel::showGoal,
        onOpenUnit = viewModel::openActivity,
        onContinuePath = viewModel::continuePath,
        onDismissGoal = viewModel::hideGoal,
        onPrepareGoal = viewModel::prepareSelectedGoal,
        onDismissNotice = viewModel::dismissNotice,
        onShowLevelInformation = viewModel::showLevelInformation,
        onDismissLevelInformation = viewModel::hideLevelInformation,
        onStartPlacement = onStartPlacement,
        onFreePractice = onFreePractice,
        onViewCompletedPath = onViewCompletedPath,
        onRetry = viewModel::retry,
        modifier = modifier
    )
}
