package com.project.speakmate.ui.placement

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.domain.model.ReassessmentTransition
import com.project.speakmate.domain.rules.PlacementRules
import com.project.speakmate.ui.components.assessment.insufficientEvidenceDescriptionRes
import com.project.speakmate.ui.placement.state.PromotionResultUiState

@Composable
fun rememberPromotionResultTitle(state: PromotionResultUiState): String {
    val transition = state.transition
    return stringResource(
        when {
            transition is ReassessmentTransition.Promoted && !state.isResultSaved ->
                R.string.promotion_runtime_save_title
            transition is ReassessmentTransition.Promoted -> R.string.promotion_runtime_promoted_title
            transition is ReassessmentTransition.NotPromoted -> R.string.promotion_runtime_maintained_title
            transition is ReassessmentTransition.Stopped ->
                when (transition.reason) {
                    ReassessmentTransition.StopReason.TECHNICAL_RETRY_LIMIT -> R.string.assessment_retry_limit_title
                    ReassessmentTransition.StopReason.NO_TASK_AVAILABLE -> R.string.assessment_content_missing_title
                    else -> R.string.assessment_state_stopped_title
                }
            transition is ReassessmentTransition.NotEligible ->
                if (transition.reason == ReassessmentTransition.EligibilityReason.HIGHEST_LEVEL)
                    R.string.assessment_b2_terminal_title
                else R.string.promotion_runtime_not_eligible
            else -> R.string.promotion_insufficient_compact_title
        },
        state.nextLevel.name
    )
}

@Composable
fun rememberPromotionResultDescription(state: PromotionResultUiState): String {
    val transition = state.transition
    return stringResource(
        when (transition) {
            is ReassessmentTransition.Promoted ->
                if (state.isResultSaved) R.string.promotion_runtime_promoted_description
                else R.string.promotion_runtime_qualified_description
            is ReassessmentTransition.NotPromoted -> R.string.promotion_runtime_maintained_description
            is ReassessmentTransition.RetryTask ->
                insufficientEvidenceDescriptionRes(
                    reason = transition.reason,
                    fallbackRes = R.string.promotion_runtime_insufficient_description,
                    isSavedUnassessed = false
                )
            is ReassessmentTransition.Stopped ->
                if (transition.reason == ReassessmentTransition.StopReason.TECHNICAL_RETRY_LIMIT) {
                    R.string.promotion_runtime_retry_limit
                } else if (transition.reason == ReassessmentTransition.StopReason.NO_TASK_AVAILABLE)
                    R.string.assessment_content_missing
                else R.string.assessment_state_stopped_description
            is ReassessmentTransition.NotEligible ->
                when (transition.reason) {
                    ReassessmentTransition.EligibilityReason.HIGHEST_LEVEL -> R.string.assessment_b2_terminal
                    ReassessmentTransition.EligibilityReason.LEVEL_NOT_ASSESSED -> R.string.assessment_not_assessed
                    ReassessmentTransition.EligibilityReason.PATH_NOT_COMPLETED -> R.string.assessment_path_incomplete
                }
            else -> R.string.promotion_runtime_not_eligible_description
        },
        state.currentLevel.name,
        state.nextLevel.name,
        PlacementRules.MAX_TECHNICAL_RETRIES
    )
}
