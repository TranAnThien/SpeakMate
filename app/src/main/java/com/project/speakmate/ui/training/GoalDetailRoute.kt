package com.project.speakmate.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.ui.training.components.TrainingUnavailableContent
import com.project.speakmate.ui.training.state.GoalDetailUiState
import com.project.speakmate.ui.training.state.TrainingActivityDestination
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.ui.training.state.TrainingProgressStatus
import com.project.speakmate.utils.extension.default

@Composable
fun GoalDetailRoute(
    goalId: String,
    onBack: () -> Unit,
    onReturnToPath: () -> Unit,
    onOpenActivity: (TrainingActivityDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrainingPathViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.status != TrainingContentStatus.CONTENT) {
        TrainingUnavailableContent(state.status == TrainingContentStatus.LOADING, viewModel::retry, modifier = modifier)
        return
    }
    val goal = remember(state.goals, goalId) { state.goals.firstOrNull { it.id == goalId } }
    val detail = remember(goal) {
        goal?.let {
            GoalDetailUiState(
                title = it.title,
                description = it.summary,
                units = it.units,
                canPrepareCurrentUnit = it.units.any { unit -> unit.status == TrainingProgressStatus.CURRENT && unit.canOpen }
            )
        }.default(GoalDetailUiState())
    }
    GoalDetailScreen(
        state = detail,
        onBack = onBack,
        onOpenUnit = onOpenActivity,
        onPrepareCurrentUnit = {
            goal?.units?.firstOrNull { it.status == TrainingProgressStatus.CURRENT && it.canOpen }
                ?.destination?.let(onOpenActivity)
        },
        onReturnToPath = onReturnToPath,
        modifier = modifier
    )
}
