package com.project.speakmate.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.R
import com.project.speakmate.ui.components.assessment.insufficientEvidenceDescriptionRes
import com.project.speakmate.ui.components.assessment.practiceSaveFailureText
import com.project.speakmate.ui.components.state.AssessmentPendingContent
import com.project.speakmate.ui.training.state.UnitResultEvaluation
import com.project.speakmate.ui.training.state.UnitResultPresentation

@Composable
fun UnitResultRoute(
    onBack: () -> Unit,
    onContinuePath: () -> Unit,
    onReplayUnit: () -> Unit,
    onRetryUnit: () -> Unit,
    onReviewLesson: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UnitResultViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.isAwaitingGrading) {
        AssessmentPendingContent(
            onBack = onBack,
            onRetryGrading = viewModel::retryGrading,
            onSaveUnassessed = viewModel::saveUnassessed,
            title = stringResource(R.string.unit_result_header),
            modifier = modifier
        )
        return
    }
    val explanation =
        stringResource(
            when {
                state.isSaving -> R.string.assessment_result_saving
                state.isUnavailable -> R.string.unit_result_unavailable
                state.presentation == UnitResultPresentation.PROCESSING -> R.string.unit_result_processing_explanation
                state.presentation == UnitResultPresentation.INSUFFICIENT_EVIDENCE ->
                    insufficientEvidenceDescriptionRes(
                        reason = state.insufficientReason,
                        fallbackRes = R.string.unit_result_insufficient_explanation,
                        isSavedUnassessed = true
                    )
                state.presentation == UnitResultPresentation.FAIL_REQUIRED_ACTION ->
                    R.string.unit_result_runtime_required_action
                else -> R.string.unit_result_runtime_criteria
            },
            state.criteria.count { it.evaluation == UnitResultEvaluation.PASSED }
        )
    val heading =
        if (state.presentation == UnitResultPresentation.INSUFFICIENT_EVIDENCE) {
            stringResource(R.string.unit_result_insufficient_criteria_heading)
        } else
            stringResource(
                R.string.unit_result_runtime_criteria_heading,
                state.criteria.count { it.evaluation == UnitResultEvaluation.PASSED }
            )
    val fallbackFeedback =
        stringResource(
            when (state.presentation) {
                UnitResultPresentation.PASS ->
                    if (!state.isSaved) R.string.assessment_graded_pending_feedback
                    else if (state.isReplay) R.string.unit_result_replay_notice else R.string.unit_result_pass_feedback
                UnitResultPresentation.INSUFFICIENT_EVIDENCE -> R.string.unit_result_insufficient_feedback
                else -> R.string.unit_result_partial_feedback
            }
        )
    val saveFailed = practiceSaveFailureText(state.saveFailure)
    val resolvedFeedback =
        when {
            state.saveFailed -> listOf(state.feedback, saveFailed).filter(String::isNotBlank).joinToString("\n")
            state.isUnavailable || state.presentation == UnitResultPresentation.INSUFFICIENT_EVIDENCE -> explanation
            else -> state.feedback.ifBlank { fallbackFeedback }
        }
    UnitResultScreen(
        state = state,
        onBack = onBack,
        onContinuePath = onContinuePath,
        onReplayUnit = onReplayUnit,
        onRetryUnit = onRetryUnit,
        onReviewLesson = onReviewLesson,
        modifier = modifier,
        onRetrySave = viewModel::retrySave,
        explanation = explanation,
        criteriaHeading = heading,
        feedback = resolvedFeedback
    )
}
