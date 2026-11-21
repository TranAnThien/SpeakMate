package com.project.speakmate.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.R
import com.project.speakmate.ui.training.state.UnitResultCriterionType
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
    val explanation = stringResource(when {
        state.isUnavailable -> R.string.unit_result_unavailable
        state.presentation == UnitResultPresentation.PROCESSING -> R.string.unit_result_processing_explanation
        state.presentation == UnitResultPresentation.INSUFFICIENT_EVIDENCE -> R.string.unit_result_insufficient_explanation
        state.presentation == UnitResultPresentation.FAIL_REQUIRED_ACTION -> R.string.unit_result_runtime_required_action
        else -> R.string.unit_result_runtime_criteria
    }, state.criteria.count { it.evaluation == UnitResultEvaluation.PASSED })
    val heading = if (state.presentation == UnitResultPresentation.INSUFFICIENT_EVIDENCE) {
        stringResource(R.string.unit_result_insufficient_criteria_heading)
    } else stringResource(R.string.unit_result_runtime_criteria_heading, state.criteria.count { it.evaluation == UnitResultEvaluation.PASSED })
    val fallbackFeedback = stringResource(when (state.presentation) {
        UnitResultPresentation.PASS -> R.string.unit_result_pass_feedback
        UnitResultPresentation.INSUFFICIENT_EVIDENCE -> R.string.unit_result_insufficient_feedback
        else -> R.string.unit_result_partial_feedback
    })
    val saveFailed = stringResource(R.string.unit_result_save_failed)
    val unknown = stringResource(R.string.unit_result_criterion_unknown)
    val met = stringResource(R.string.placement_criterion_met_feedback)
    val unmet = stringResource(R.string.placement_criterion_unmet_feedback)
    val criteria = state.criteria.map { criterion ->
        criterion.copy(
            name = stringResource(when (criterion.criterion) {
                UnitResultCriterionType.RANGE -> R.string.unit_result_criterion_name_0
                UnitResultCriterionType.ACCURACY -> R.string.unit_result_criterion_name_1
                UnitResultCriterionType.FLUENCY -> R.string.unit_result_criterion_name_2
                UnitResultCriterionType.INTERACTION -> R.string.unit_result_criterion_name_3
                UnitResultCriterionType.COHERENCE -> R.string.unit_result_criterion_name_4
            }),
            description = criterion.description.ifBlank {
                when (criterion.evaluation) {
                    UnitResultEvaluation.PASSED -> met
                    UnitResultEvaluation.NOT_PASSED -> unmet
                    UnitResultEvaluation.NOT_EVALUATED -> unknown
                }
            }
        )
    }
    val resolvedFeedback = when {
        state.saveFailed -> listOf(state.feedback, saveFailed).filter(String::isNotBlank).joinToString("\n")
        state.isUnavailable -> explanation
        else -> state.feedback.ifBlank { fallbackFeedback }
    }
    val displayState = remember(state, explanation, heading, criteria, resolvedFeedback) {
        state.copy(explanation = explanation, criteriaHeading = heading, criteria = criteria, feedback = resolvedFeedback)
    }
    UnitResultScreen(displayState, onBack, onContinuePath, onReplayUnit, onRetryUnit, onReviewLesson,
        modifier, onRetrySave = viewModel::retrySave)
}
