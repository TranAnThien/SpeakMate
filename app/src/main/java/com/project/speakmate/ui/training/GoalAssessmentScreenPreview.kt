package com.project.speakmate.ui.training

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.SpeakMateTheme
import com.project.speakmate.ui.training.state.GoalAssessmentActionUiState
import com.project.speakmate.ui.training.state.GoalAssessmentPresentation
import com.project.speakmate.ui.training.state.GoalAssessmentUiState

@Preview(name = "Goal Assessment / Intro / Default / Audited / 34:3306", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun GoalAssessmentIntroductionScreenPreview() {
    SpeakMateTheme {
        GoalAssessmentScreen(
            state = rememberGoalAssessmentPreviewState(GoalAssessmentPresentation.INTRODUCTION),
            onBack = {},
            onStartAssessment = {},
            onReviewLessons = {},
            onRetryAssessment = {},
            onOpenNextGoal = {},
            onViewCompletedPath = {},
            onReturnToPath = {}
        )
    }
}

@Preview(name = "Goal Assessment / Intro / Scroll / Bottom / Audited / 102:14", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun GoalAssessmentIntroductionBottomScreenPreview() {
    SpeakMateTheme {
        GoalAssessmentScreen(
            state = rememberGoalAssessmentPreviewState(GoalAssessmentPresentation.INTRODUCTION),
            onBack = {},
            onStartAssessment = {},
            onReviewLessons = {},
            onRetryAssessment = {},
            onOpenNextGoal = {},
            onViewCompletedPath = {},
            onReturnToPath = {},
            scrollState = rememberScrollState(with(LocalDensity.current) { 175.dp.roundToPx() })
        )
    }
}

@Preview(name = "Goal Assessment / Evaluation / Processing / Added • Prompt-SRS / 34:3331", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun GoalAssessmentProcessingScreenPreview() {
    SpeakMateTheme {
        GoalAssessmentScreen(
            state = rememberGoalAssessmentPreviewState(GoalAssessmentPresentation.PROCESSING),
            onBack = {},
            onStartAssessment = {},
            onReviewLessons = {},
            onRetryAssessment = {},
            onOpenNextGoal = {},
            onViewCompletedPath = {},
            onReturnToPath = {}
        )
    }
}

@Preview(name = "Goal Assessment / Result / PASS → Next Goal / Added • Prompt-SRS / 34:3353", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun GoalAssessmentNextGoalScreenPreview() {
    SpeakMateTheme {
        GoalAssessmentScreen(
            state = rememberGoalAssessmentPreviewState(GoalAssessmentPresentation.NEXT_GOAL),
            onBack = {},
            onStartAssessment = {},
            onReviewLessons = {},
            onRetryAssessment = {},
            onOpenNextGoal = {},
            onViewCompletedPath = {},
            onReturnToPath = {}
        )
    }
}

@Preview(name = "Goal Assessment / Result / PASS → Path Complete / Added • Prompt-SRS / 34:3385", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun GoalAssessmentPathCompletedScreenPreview() {
    SpeakMateTheme {
        GoalAssessmentScreen(
            state = rememberGoalAssessmentPreviewState(GoalAssessmentPresentation.PATH_COMPLETED),
            onBack = {},
            onStartAssessment = {},
            onReviewLessons = {},
            onRetryAssessment = {},
            onOpenNextGoal = {},
            onViewCompletedPath = {},
            onReturnToPath = {}
        )
    }
}

@Preview(name = "Goal Assessment / Result / PARTIAL / Added • Prompt-SRS / 34:3418", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun GoalAssessmentPartialScreenPreview() {
    SpeakMateTheme {
        GoalAssessmentScreen(
            state = rememberGoalAssessmentPreviewState(GoalAssessmentPresentation.PARTIAL),
            onBack = {},
            onStartAssessment = {},
            onReviewLessons = {},
            onRetryAssessment = {},
            onOpenNextGoal = {},
            onViewCompletedPath = {},
            onReturnToPath = {}
        )
    }
}

@Preview(name = "Goal Assessment / Result / FAIL • Speaking Criteria / Added • Prompt-SRS / 34:3451", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun GoalAssessmentCriteriaFailureScreenPreview() {
    SpeakMateTheme {
        GoalAssessmentScreen(
            state = rememberGoalAssessmentPreviewState(GoalAssessmentPresentation.FAIL_CRITERIA),
            onBack = {},
            onStartAssessment = {},
            onReviewLessons = {},
            onRetryAssessment = {},
            onOpenNextGoal = {},
            onViewCompletedPath = {},
            onReturnToPath = {}
        )
    }
}

@Preview(name = "Goal Assessment / Result / FAIL • Required Action Gate / Added • Prompt-SRS / 34:3484", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun GoalAssessmentRequiredActionFailureScreenPreview() {
    SpeakMateTheme {
        GoalAssessmentScreen(
            state = rememberGoalAssessmentPreviewState(GoalAssessmentPresentation.FAIL_REQUIRED_ACTION),
            onBack = {},
            onStartAssessment = {},
            onReviewLessons = {},
            onRetryAssessment = {},
            onOpenNextGoal = {},
            onViewCompletedPath = {},
            onReturnToPath = {}
        )
    }
}

@Preview(name = "Goal Assessment / Result / INSUFFICIENT_EVIDENCE / Added • Prompt-SRS / 34:3517", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun GoalAssessmentInsufficientEvidenceScreenPreview() {
    SpeakMateTheme {
        GoalAssessmentScreen(
            state = rememberGoalAssessmentPreviewState(GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE),
            onBack = {},
            onStartAssessment = {},
            onReviewLessons = {},
            onRetryAssessment = {},
            onOpenNextGoal = {},
            onViewCompletedPath = {},
            onReturnToPath = {}
        )
    }
}

@Preview(name = "Goal Assessment / Replay / PASS • Progress Preserved / Added • Prompt-SRS / 34:3551", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun GoalAssessmentReplayCompletedScreenPreview() {
    SpeakMateTheme {
        GoalAssessmentScreen(
            state = rememberGoalAssessmentPreviewState(GoalAssessmentPresentation.REPLAY_COMPLETED),
            onBack = {},
            onStartAssessment = {},
            onReviewLessons = {},
            onRetryAssessment = {},
            onOpenNextGoal = {},
            onViewCompletedPath = {},
            onReturnToPath = {}
        )
    }
}

@Composable
private fun rememberGoalAssessmentPreviewState(presentation: GoalAssessmentPresentation): GoalAssessmentUiState {
    val title = stringResource(when (presentation) {
        GoalAssessmentPresentation.INTRODUCTION -> R.string.goal_assessment_introduction_title
        GoalAssessmentPresentation.PROCESSING -> R.string.unit_result_processing
        GoalAssessmentPresentation.NEXT_GOAL -> R.string.goal_assessment_next_goal_title
        GoalAssessmentPresentation.PATH_COMPLETED -> R.string.goal_assessment_path_completed_title
        GoalAssessmentPresentation.PARTIAL -> R.string.unit_result_partial
        GoalAssessmentPresentation.FAIL_CRITERIA -> R.string.placement_not_passed
        GoalAssessmentPresentation.FAIL_REQUIRED_ACTION -> R.string.unit_result_missing_action
        GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE -> R.string.unit_result_insufficient
        GoalAssessmentPresentation.REPLAY_COMPLETED -> R.string.goal_assessment_replay_title
    })
    val description = stringResource(when (presentation) {
        GoalAssessmentPresentation.INTRODUCTION -> R.string.goal_assessment_introduction_description
        GoalAssessmentPresentation.PROCESSING -> R.string.unit_result_processing_explanation
        GoalAssessmentPresentation.NEXT_GOAL -> R.string.goal_assessment_next_goal_description
        GoalAssessmentPresentation.PATH_COMPLETED -> R.string.goal_assessment_path_completed_description
        GoalAssessmentPresentation.PARTIAL -> R.string.goal_assessment_partial_description
        GoalAssessmentPresentation.FAIL_CRITERIA -> R.string.goal_assessment_criteria_failure_description
        GoalAssessmentPresentation.FAIL_REQUIRED_ACTION -> R.string.goal_assessment_required_action_failure_description
        GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE -> R.string.goal_assessment_insufficient_description
        GoalAssessmentPresentation.REPLAY_COMPLETED -> R.string.goal_assessment_replay_description
    })
    if (presentation == GoalAssessmentPresentation.PROCESSING) {
        return GoalAssessmentUiState(presentation = presentation, title = title, description = description)
    }
    val feedbackTitle = stringResource(when (presentation) {
        GoalAssessmentPresentation.INTRODUCTION -> R.string.goal_assessment_introduction_feedback_title
        GoalAssessmentPresentation.NEXT_GOAL -> R.string.goal_assessment_next_goal_feedback_title
        GoalAssessmentPresentation.PATH_COMPLETED -> R.string.goal_assessment_path_completed_feedback_title
        GoalAssessmentPresentation.PARTIAL -> R.string.goal_assessment_partial_feedback_title
        GoalAssessmentPresentation.FAIL_CRITERIA -> R.string.goal_assessment_criteria_failure_feedback_title
        GoalAssessmentPresentation.FAIL_REQUIRED_ACTION -> R.string.goal_assessment_required_action_failure_feedback_title
        GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE -> R.string.goal_assessment_insufficient_feedback_title
        GoalAssessmentPresentation.REPLAY_COMPLETED -> R.string.goal_assessment_replay_feedback_title
        GoalAssessmentPresentation.PROCESSING -> R.string.unit_result_processing
    })
    val feedback = stringResource(when (presentation) {
        GoalAssessmentPresentation.INTRODUCTION -> R.string.goal_assessment_introduction_feedback
        GoalAssessmentPresentation.NEXT_GOAL -> R.string.goal_assessment_next_goal_feedback
        GoalAssessmentPresentation.PATH_COMPLETED -> R.string.goal_assessment_path_completed_feedback
        GoalAssessmentPresentation.PARTIAL -> R.string.goal_assessment_partial_feedback
        GoalAssessmentPresentation.FAIL_CRITERIA -> R.string.goal_assessment_criteria_failure_feedback
        GoalAssessmentPresentation.FAIL_REQUIRED_ACTION -> R.string.goal_assessment_required_action_failure_feedback
        GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE -> R.string.goal_assessment_insufficient_feedback
        GoalAssessmentPresentation.REPLAY_COMPLETED -> R.string.goal_assessment_replay_feedback
        GoalAssessmentPresentation.PROCESSING -> R.string.unit_result_processing_explanation
    })
    return GoalAssessmentUiState(
        presentation = presentation,
        title = title,
        description = description,
        feedbackTitle = feedbackTitle,
        feedback = feedback,
        requiredActions = if (presentation == GoalAssessmentPresentation.INTRODUCTION) listOf(
            GoalAssessmentActionUiState("order", stringResource(R.string.goal_assessment_preview_action_0)),
            GoalAssessmentActionUiState("clarify", stringResource(R.string.goal_assessment_preview_action_1)),
            GoalAssessmentActionUiState("confirm", stringResource(R.string.goal_assessment_preview_action_2))
        ) else emptyList()
    )
}
