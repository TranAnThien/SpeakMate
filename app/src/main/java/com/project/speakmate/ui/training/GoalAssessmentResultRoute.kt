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
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.ui.components.AssessmentPendingContent
import com.project.speakmate.ui.components.BackAppBar
import com.project.speakmate.ui.components.practiceSaveFailureText
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
    if (state.isAwaitingGrading) {
        AssessmentPendingContent(onBack, viewModel::retryGrading, viewModel::saveUnassessed, stringResource(R.string.goal_assessment_header), modifier)
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
    val presentation = state.presentation
    val title = stringResource(when (presentation) {
        GoalAssessmentPresentation.PROCESSING -> R.string.unit_result_processing
        GoalAssessmentPresentation.NEXT_GOAL -> R.string.goal_assessment_next_goal_title
        GoalAssessmentPresentation.PATH_COMPLETED -> R.string.goal_assessment_path_completed_title
        GoalAssessmentPresentation.REPLAY_COMPLETED -> if (state.isSaved && state.isReplay) R.string.goal_assessment_replay_title else R.string.goal_assessment_next_goal_title
        GoalAssessmentPresentation.PARTIAL -> R.string.unit_result_partial
        GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE -> R.string.unit_result_insufficient
        else -> R.string.placement_not_passed
    })
    val description = if (state.isSaveFailed) practiceSaveFailureText(state.saveFailure) else stringResource(when {
        state.isSaving -> R.string.assessment_result_saving
        state.isSaveFailed -> R.string.unit_result_save_failed
        else -> when (presentation) {
            GoalAssessmentPresentation.PROCESSING -> R.string.unit_result_processing_explanation
            GoalAssessmentPresentation.NEXT_GOAL -> R.string.goal_assessment_next_goal_description
            GoalAssessmentPresentation.PATH_COMPLETED -> R.string.goal_assessment_path_completed_description
            GoalAssessmentPresentation.REPLAY_COMPLETED ->
                if (state.isReplay) R.string.goal_assessment_replay_description else if (!state
                    .isSaved) R.string.assessment_graded_pending_feedback else R.string.goal_assessment_runtime_feedback
            GoalAssessmentPresentation.PARTIAL -> R.string.goal_assessment_partial_description
            GoalAssessmentPresentation.FAIL_CRITERIA -> R.string.goal_assessment_criteria_failure_description
            GoalAssessmentPresentation.FAIL_REQUIRED_ACTION -> R.string.goal_assessment_required_action_failure_description
            else -> when (state.insufficientReason) {
                InsufficientEvidenceReason.AUDIO_UNAVAILABLE -> R.string.assessment_audio_unavailable_reason
                InsufficientEvidenceReason.EVALUATION_FAILED, InsufficientEvidenceReason.EVALUATION_INVALID -> R.string.assessment_saved_unassessed_reason
                else -> R.string.goal_assessment_insufficient_description
            }
        }
    })
    val feedbackTitle = stringResource(when {
        !state.isSaved && !state.isSaveFailed -> R.string.assessment_graded_title
        state.isSaveFailed -> if (state.canRetrySave) R.string.unit_result_retry_save else R.string.completion_save_blocked_title
        else -> when (presentation) {
            GoalAssessmentPresentation.NEXT_GOAL -> R.string.goal_assessment_next_goal_feedback_title
            GoalAssessmentPresentation.PATH_COMPLETED -> R.string.goal_assessment_path_completed_feedback_title
            GoalAssessmentPresentation.REPLAY_COMPLETED -> if (state.isReplay) R.string.goal_assessment_replay_feedback_title else R.string.assessment_graded_title
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
            feedback = if (presentation == GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE) description else state.feedback
                .ifBlank { stringResource(R.string.goal_assessment_runtime_feedback) },
            isSaved = state.isSaved,
            hasProgressError = state.hasLoadError,
            resultActions = state.resultActions,
            criteria = state.criteria,
            isSaveFailed = state.isSaveFailed,
            isSaving = state.isSaving,
            isPrimaryEnabled = !state.hasLoadError && !state.isSaving && (state.isSaved || (state.isSaveFailed && state.canRetrySave))
        ),
        onBack = onBack,
        onStartAssessment = onRetryAssessment,
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
