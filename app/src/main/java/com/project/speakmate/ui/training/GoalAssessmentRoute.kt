package com.project.speakmate.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.R
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
    if (state.status != TrainingContentStatus.CONTENT) {
        TrainingUnavailableContent(
            isLoading = state.status == TrainingContentStatus.LOADING,
            onRetry = if (state.isLocked) onBack else viewModel::retry,
            errorMessage = if (state.isLocked) stringResource(R.string.goal_assessment_locked) else "",
            modifier = modifier
        )
    } else {
        GoalAssessmentScreen(
            state = state.copy(
                description = stringResource(R.string.goal_assessment_runtime_introduction, state.description),
                feedbackTitle = stringResource(R.string.goal_assessment_introduction_feedback_title),
                feedback = stringResource(R.string.goal_assessment_introduction_feedback)
            ),
            onBack = onBack,
            onStartAssessment = onStartAssessment,
            onReviewLessons = onReviewLessons,
            onRetryAssessment = onStartAssessment,
            onOpenNextGoal = onBack,
            onViewCompletedPath = onBack,
            onReturnToPath = onBack,
            modifier = modifier
        )
    }
}
