package com.project.speakmate.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.ui.components.assessment.insufficientEvidenceDescriptionRes
import com.project.speakmate.ui.components.assessment.practiceSaveFailureText
import com.project.speakmate.ui.training.state.GoalAssessmentPresentation
import com.project.speakmate.ui.training.state.GoalAssessmentResultUiState

@Composable
fun rememberGoalAssessmentResultTitle(state: GoalAssessmentResultUiState): String {
    val presentation = state.presentation
    return stringResource(
        when (presentation) {
            GoalAssessmentPresentation.PROCESSING -> R.string.unit_result_processing
            GoalAssessmentPresentation.NEXT_GOAL -> R.string.goal_assessment_next_goal_title
            GoalAssessmentPresentation.PATH_COMPLETED -> R.string.goal_assessment_path_completed_title
            GoalAssessmentPresentation.REPLAY_COMPLETED ->
                if (state.isSaved && state.isReplay) R.string.goal_assessment_replay_title
                else R.string.goal_assessment_next_goal_title
            GoalAssessmentPresentation.PARTIAL -> R.string.unit_result_partial
            GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE -> R.string.unit_result_insufficient
            else -> R.string.placement_not_passed
        }
    )
}

@Composable
fun rememberGoalAssessmentResultDescription(state: GoalAssessmentResultUiState): String {
    val presentation = state.presentation
    return if (state.isSaveFailed) practiceSaveFailureText(state.saveFailure)
    else
        stringResource(
            when {
                state.isSaving -> R.string.assessment_result_saving
                state.isSaveFailed -> R.string.unit_result_save_failed
                else ->
                    when (presentation) {
                        GoalAssessmentPresentation.PROCESSING -> R.string.unit_result_processing_explanation
                        GoalAssessmentPresentation.NEXT_GOAL -> R.string.goal_assessment_next_goal_description
                        GoalAssessmentPresentation.PATH_COMPLETED -> R.string.goal_assessment_path_completed_description
                        GoalAssessmentPresentation.REPLAY_COMPLETED ->
                            if (state.isReplay) R.string.goal_assessment_replay_description
                            else if (!state.isSaved) R.string.assessment_graded_pending_feedback
                            else R.string.goal_assessment_runtime_feedback
                        GoalAssessmentPresentation.PARTIAL -> R.string.goal_assessment_partial_description
                        GoalAssessmentPresentation.FAIL_CRITERIA ->
                            R.string.goal_assessment_criteria_failure_description
                        GoalAssessmentPresentation.FAIL_REQUIRED_ACTION ->
                            R.string.goal_assessment_required_action_failure_description
                        else ->
                            insufficientEvidenceDescriptionRes(
                                reason = state.insufficientReason,
                                fallbackRes = R.string.goal_assessment_insufficient_description,
                                isSavedUnassessed = true
                            )
                    }
            }
        )
}

@Composable
fun rememberGoalAssessmentResultFeedbackTitle(state: GoalAssessmentResultUiState): String {
    val presentation = state.presentation
    return stringResource(
        when {
            !state.isSaved && !state.isSaveFailed -> R.string.assessment_graded_title
            state.isSaveFailed ->
                if (state.canRetrySave) R.string.unit_result_retry_save else R.string.completion_save_blocked_title
            else ->
                when (presentation) {
                    GoalAssessmentPresentation.NEXT_GOAL -> R.string.goal_assessment_next_goal_feedback_title
                    GoalAssessmentPresentation.PATH_COMPLETED -> R.string.goal_assessment_path_completed_feedback_title
                    GoalAssessmentPresentation.REPLAY_COMPLETED ->
                        if (state.isReplay) R.string.goal_assessment_replay_feedback_title
                        else R.string.assessment_graded_title
                    GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE ->
                        R.string.goal_assessment_insufficient_feedback_title
                    else -> R.string.goal_assessment_evaluated_heading
                }
        }
    )
}
