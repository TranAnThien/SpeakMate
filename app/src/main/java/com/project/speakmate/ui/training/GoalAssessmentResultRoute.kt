package com.project.speakmate.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.R
import com.project.speakmate.ui.training.components.TrainingUnavailableContent
import com.project.speakmate.ui.training.state.GoalAssessmentPresentation
import com.project.speakmate.ui.training.state.GoalAssessmentUiState

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
    if (state.isUnavailable || state.hasLoadError) {
        TrainingUnavailableContent(
            isLoading = false,
            onRetry = if (state.isUnavailable) onReturnToPath else viewModel::retry,
            errorMessage = if (state.isUnavailable) stringResource(R.string.unit_result_unavailable) else "",
            modifier = modifier
        )
        return
    }
    val presentation = state.presentation
    val title = stringResource(when (presentation) {
        GoalAssessmentPresentation.PROCESSING -> R.string.unit_result_processing
        GoalAssessmentPresentation.NEXT_GOAL -> R.string.goal_assessment_next_goal_title
        GoalAssessmentPresentation.PATH_COMPLETED -> R.string.goal_assessment_path_completed_title
        GoalAssessmentPresentation.REPLAY_COMPLETED -> if (state.isSaved) R.string.goal_assessment_replay_title else R.string.goal_assessment_next_goal_title
        GoalAssessmentPresentation.PARTIAL -> R.string.unit_result_partial
        GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE -> R.string.unit_result_insufficient
        else -> R.string.placement_not_passed
    })
    val description = stringResource(when {
        state.isSaveFailed -> R.string.unit_result_save_failed
        else -> when (presentation) {
            GoalAssessmentPresentation.PROCESSING -> R.string.unit_result_processing_explanation
            GoalAssessmentPresentation.NEXT_GOAL -> R.string.goal_assessment_next_goal_description
            GoalAssessmentPresentation.PATH_COMPLETED -> R.string.goal_assessment_path_completed_description
            GoalAssessmentPresentation.REPLAY_COMPLETED -> R.string.goal_assessment_replay_description
            GoalAssessmentPresentation.PARTIAL -> R.string.goal_assessment_partial_description
            GoalAssessmentPresentation.FAIL_CRITERIA -> R.string.goal_assessment_criteria_failure_description
            GoalAssessmentPresentation.FAIL_REQUIRED_ACTION -> R.string.goal_assessment_required_action_failure_description
            else -> R.string.goal_assessment_insufficient_description
        }
    })
    val feedbackTitle = stringResource(when {
        state.isSaveFailed -> R.string.unit_result_retry_save
        else -> when (presentation) {
            GoalAssessmentPresentation.NEXT_GOAL -> R.string.goal_assessment_next_goal_feedback_title
            GoalAssessmentPresentation.PATH_COMPLETED -> R.string.goal_assessment_path_completed_feedback_title
            GoalAssessmentPresentation.REPLAY_COMPLETED -> R.string.goal_assessment_replay_feedback_title
            GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE -> R.string.goal_assessment_insufficient_feedback_title
            else -> R.string.goal_assessment_evaluated_heading
        }
    })
    GoalAssessmentScreen(
        state = GoalAssessmentUiState(
            presentation = presentation,
            title = title,
            description = description,
            feedbackTitle = feedbackTitle,
            feedback = state.feedback.ifBlank { stringResource(R.string.goal_assessment_runtime_feedback) },
            isSaveFailed = state.isSaveFailed,
            isSaving = state.isSaving,
            isPrimaryEnabled = state.isSaved || state.isSaveFailed
        ),
        onBack = onBack,
        onStartAssessment = onRetryAssessment,
        onReviewLessons = onReviewLessons,
        onRetryAssessment = onRetryAssessment,
        onOpenNextGoal = { state.nextGoalId?.let(onOpenNextGoal) },
        onViewCompletedPath = onViewCompletedPath,
        onReturnToPath = onReturnToPath,
        modifier = modifier,
        onRetrySave = viewModel::retrySave
    )
}
