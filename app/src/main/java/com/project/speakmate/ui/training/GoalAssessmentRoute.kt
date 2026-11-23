package com.project.speakmate.ui.training

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.R
import com.project.speakmate.ui.components.BackAppBar
import com.project.speakmate.ui.training.components.TrainingUnavailableContent
import com.project.speakmate.ui.training.state.TrainingContentStatus

@Composable
fun GoalAssessmentRoute(
    onBack: () -> Unit,
    onStartAssessment: () -> Unit,
    onReviewLessons: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalAssessmentViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    val startAssessment by rememberUpdatedState(onStartAssessment)
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.cancelStart() }
    LaunchedEffect(state.isStartReady, viewModel, owner) {
        if (state.isStartReady) owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (viewModel.consumeStartReady()) startAssessment()
        }
    }
    if (state.status != TrainingContentStatus.CONTENT) {
        Column(modifier.fillMaxSize()) {
            BackAppBar(stringResource(R.string.goal_assessment_header), onBack)
            TrainingUnavailableContent(
                isLoading = state.status == TrainingContentStatus.LOADING,
                onRetry = if (state.isLocked) onBack else viewModel::retry,
                errorMessage = if (state.isLocked) stringResource(R.string.goal_assessment_locked) else "",
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        GoalAssessmentScreen(
            state = state.copy(
                description = stringResource(R.string.goal_assessment_runtime_introduction, state.description),
                isPrimaryEnabled = !state.isStarting,
                feedbackTitle = stringResource(R.string.goal_assessment_introduction_feedback_title),
                feedback = stringResource(R.string.goal_assessment_introduction_feedback)
            ),
            onBack = onBack,
            onStartAssessment = viewModel::prepareStart,
            onReviewLessons = onReviewLessons,
            onRetryAssessment = viewModel::prepareStart,
            onOpenNextGoal = onBack,
            onViewCompletedPath = onBack,
            onReturnToPath = onBack,
            modifier = modifier
        )
    }
}
