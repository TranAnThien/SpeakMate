package com.project.speakmate.ui.training

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.R
import com.project.speakmate.ui.components.header.BackAppBar
import com.project.speakmate.ui.components.state.AssessmentPendingContent
import com.project.speakmate.ui.training.components.TrainingUnavailableContent

@Composable
fun GoalAssessmentResultRoute(
    onBack: () -> Unit,
    onRetryAssessment: () -> Unit,
    onOpenNextGoal: (String) -> Unit,
    onViewCompletedPath: () -> Unit,
    onReturnToPath: () -> Unit,
    onReviewLessons: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalAssessmentResultViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.isAwaitingGrading) {
        AssessmentPendingContent(
            onBack = onBack,
            onRetryGrading = viewModel::retryGrading,
            onSaveUnassessed = viewModel::saveUnassessed,
            title = stringResource(R.string.goal_assessment_header),
            modifier = modifier
        )
        return
    }
    if (state.isUnavailable || state.hasLoadError && !state.isSaved) {
        Column(modifier.fillMaxSize()) {
            BackAppBar(stringResource(R.string.goal_assessment_header), onBack)
            TrainingUnavailableContent(
                isLoading = false,
                onRetry = if (state.isUnavailable) onReturnToPath else viewModel::retry,
                errorMessage = if (state.isUnavailable) stringResource(R.string.unit_result_unavailable) else "",
                modifier = Modifier.weight(1f)
            )
        }
        return
    }
    GoalAssessmentResultScreen(
        state = state,
        onBack = onBack,
        onReviewLessons = onReviewLessons,
        onRetryAssessment = onRetryAssessment,
        onOpenNextGoal = { state.nextGoalId?.let(onOpenNextGoal) },
        onViewCompletedPath = onViewCompletedPath,
        onReturnToPath = onReturnToPath,
        modifier = modifier,
        onRetrySave = viewModel::retrySave,
        onRetryProgress = viewModel::retry
    )
}
