package com.project.speakmate.ui.training

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.SpeakMateTheme
import com.project.speakmate.ui.training.state.UnitResultActionUiState
import com.project.speakmate.ui.training.state.UnitResultCriterionUiState
import com.project.speakmate.ui.training.state.UnitResultEvaluation
import com.project.speakmate.ui.training.state.UnitResultPresentation
import com.project.speakmate.ui.training.state.UnitResultUiState

@Preview(name = "Unit Result / PASS / Top • SRS / 17:472", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun UnitResultPassScreenPreview() {
    SpeakMateTheme {
        UnitResultScreen(
            state = rememberUnitResultPreviewState(UnitResultPresentation.PASS),
            onBack = {},
            onContinuePath = {},
            onReplayUnit = {},
            onRetryUnit = {},
            onReviewLesson = {}
        )
    }
}

@Preview(name = "Unit Result / FAIL / Required Action • SRS BR-03 / 17:473", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun UnitResultRequiredActionFailureScreenPreview() {
    SpeakMateTheme {
        UnitResultScreen(
            state = rememberUnitResultPreviewState(UnitResultPresentation.FAIL_REQUIRED_ACTION),
            onBack = {},
            onContinuePath = {},
            onReplayUnit = {},
            onRetryUnit = {},
            onReviewLesson = {}
        )
    }
}

@Preview(name = "Unit Result / FAIL / Criteria Threshold / 17:474", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun UnitResultCriteriaFailureScreenPreview() {
    SpeakMateTheme {
        UnitResultScreen(
            state = rememberUnitResultPreviewState(UnitResultPresentation.FAIL_CRITERIA),
            onBack = {},
            onContinuePath = {},
            onReplayUnit = {},
            onRetryUnit = {},
            onReviewLesson = {}
        )
    }
}

@Preview(name = "Unit Result / INSUFFICIENT_EVIDENCE / Top • SRS / 17:475", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun UnitResultInsufficientEvidenceScreenPreview() {
    SpeakMateTheme {
        UnitResultScreen(
            state = rememberUnitResultPreviewState(UnitResultPresentation.INSUFFICIENT_EVIDENCE),
            onBack = {},
            onContinuePath = {},
            onReplayUnit = {},
            onRetryUnit = {},
            onReviewLesson = {}
        )
    }
}

@Preview(name = "Unit Result / PARTIAL / SRS-compliant / 32:2194", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun UnitResultPartialScreenPreview() {
    SpeakMateTheme {
        UnitResultScreen(
            state = rememberUnitResultPreviewState(UnitResultPresentation.PARTIAL),
            onBack = {},
            onContinuePath = {},
            onReplayUnit = {},
            onRetryUnit = {},
            onReviewLesson = {}
        )
    }
}

@Preview(name = "Training Unit / Result Processing / Added • UX gap P1 / 36:4111", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun UnitResultProcessingScreenPreview() {
    SpeakMateTheme {
        UnitResultScreen(
            state = rememberUnitResultPreviewState(UnitResultPresentation.PROCESSING),
            onBack = {},
            onContinuePath = {},
            onReplayUnit = {},
            onRetryUnit = {},
            onReviewLesson = {}
        )
    }
}

@Preview(name = "Unit Result / PASS / Scroll / Bottom / Audited / 102:69", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun UnitResultPassBottomScreenPreview() {
    SpeakMateTheme {
        UnitResultScreen(
            state = rememberUnitResultPreviewState(UnitResultPresentation.PASS),
            onBack = {},
            onContinuePath = {},
            onReplayUnit = {},
            onRetryUnit = {},
            onReviewLesson = {},
            scrollState = rememberScrollState(with(LocalDensity.current) { 389.dp.roundToPx() })
        )
    }
}

@Preview(name = "Unit Result / FAIL Required Action / Scroll / Bottom / Audited / 103:145", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun UnitResultRequiredActionFailureBottomScreenPreview() {
    SpeakMateTheme {
        UnitResultScreen(
            state = rememberUnitResultPreviewState(UnitResultPresentation.FAIL_REQUIRED_ACTION),
            onBack = {},
            onContinuePath = {},
            onReplayUnit = {},
            onRetryUnit = {},
            onReviewLesson = {},
            scrollState = rememberScrollState(with(LocalDensity.current) { 389.dp.roundToPx() })
        )
    }
}

@Preview(name = "Unit Result / FAIL Criteria / Scroll / Bottom / Audited / 103:223", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun UnitResultCriteriaFailureBottomScreenPreview() {
    SpeakMateTheme {
        UnitResultScreen(
            state = rememberUnitResultPreviewState(UnitResultPresentation.FAIL_CRITERIA),
            onBack = {},
            onContinuePath = {},
            onReplayUnit = {},
            onRetryUnit = {},
            onReviewLesson = {},
            scrollState = rememberScrollState(with(LocalDensity.current) { 389.dp.roundToPx() })
        )
    }
}

@Preview(name = "Unit Result / INSUFFICIENT_EVIDENCE / Scroll / Bottom / Audited / 103:306", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun UnitResultInsufficientEvidenceBottomScreenPreview() {
    SpeakMateTheme {
        UnitResultScreen(
            state = rememberUnitResultPreviewState(UnitResultPresentation.INSUFFICIENT_EVIDENCE),
            onBack = {},
            onContinuePath = {},
            onReplayUnit = {},
            onRetryUnit = {},
            onReviewLesson = {},
            scrollState = rememberScrollState(with(LocalDensity.current) { 409.dp.roundToPx() })
        )
    }
}

@Preview(name = "Unit Result / PARTIAL / Scroll / Bottom / 127:4445", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun UnitResultPartialBottomScreenPreview() {
    SpeakMateTheme {
        UnitResultScreen(
            state = rememberUnitResultPreviewState(UnitResultPresentation.PARTIAL),
            onBack = {},
            onContinuePath = {},
            onReplayUnit = {},
            onRetryUnit = {},
            onReviewLesson = {},
            scrollState = rememberScrollState(with(LocalDensity.current) { 389.dp.roundToPx() })
        )
    }
}

@Composable
private fun rememberUnitResultPreviewState(presentation: UnitResultPresentation): UnitResultUiState {
    val explanation = stringResource(when (presentation) {
        UnitResultPresentation.PASS -> R.string.unit_result_pass_explanation
        UnitResultPresentation.FAIL_REQUIRED_ACTION -> R.string.unit_result_required_action_explanation
        UnitResultPresentation.FAIL_CRITERIA -> R.string.unit_result_criteria_explanation
        UnitResultPresentation.INSUFFICIENT_EVIDENCE -> R.string.unit_result_insufficient_explanation
        UnitResultPresentation.PARTIAL -> R.string.unit_result_partial_explanation
        UnitResultPresentation.PROCESSING -> R.string.unit_result_processing_explanation
    })
    if (presentation == UnitResultPresentation.PROCESSING) {
        return UnitResultUiState(presentation = presentation, explanation = explanation)
    }
    val criteriaHeading = stringResource(when (presentation) {
        UnitResultPresentation.PASS -> R.string.unit_result_pass_criteria_heading
        UnitResultPresentation.FAIL_REQUIRED_ACTION -> R.string.unit_result_required_action_criteria_heading
        UnitResultPresentation.FAIL_CRITERIA -> R.string.unit_result_criteria_criteria_heading
        UnitResultPresentation.INSUFFICIENT_EVIDENCE -> R.string.unit_result_insufficient_criteria_heading
        UnitResultPresentation.PARTIAL -> R.string.unit_result_partial_criteria_heading
        UnitResultPresentation.PROCESSING -> R.string.unit_result_header
    })
    val feedback = stringResource(when (presentation) {
        UnitResultPresentation.PASS -> R.string.unit_result_pass_feedback
        UnitResultPresentation.FAIL_REQUIRED_ACTION -> R.string.unit_result_required_action_feedback
        UnitResultPresentation.FAIL_CRITERIA -> R.string.unit_result_criteria_feedback
        UnitResultPresentation.INSUFFICIENT_EVIDENCE -> R.string.unit_result_insufficient_feedback
        UnitResultPresentation.PARTIAL -> R.string.unit_result_partial_feedback
        UnitResultPresentation.PROCESSING -> R.string.unit_result_processing_explanation
    })
    val evaluations = when (presentation) {
        UnitResultPresentation.PASS -> listOf(UnitResultEvaluation.PASSED, UnitResultEvaluation.NOT_PASSED,
            UnitResultEvaluation.PASSED, UnitResultEvaluation.PASSED, UnitResultEvaluation.PASSED)
        UnitResultPresentation.PARTIAL, UnitResultPresentation.FAIL_REQUIRED_ACTION ->
            listOf(UnitResultEvaluation.PASSED, UnitResultEvaluation.NOT_PASSED,
                UnitResultEvaluation.PASSED, UnitResultEvaluation.NOT_PASSED, UnitResultEvaluation.PASSED)
        UnitResultPresentation.FAIL_CRITERIA -> List(5) { UnitResultEvaluation.NOT_PASSED }
        UnitResultPresentation.INSUFFICIENT_EVIDENCE, UnitResultPresentation.PROCESSING ->
            List(5) { UnitResultEvaluation.NOT_EVALUATED }
    }
    return UnitResultUiState(
        presentation = presentation,
        title = stringResource(R.string.unit_result_preview_unit),
        explanation = explanation,
        criteriaHeading = criteriaHeading,
        feedback = feedback,
        requiredActions = listOf(
            UnitResultActionUiState("resize", stringResource(R.string.unit_result_preview_action_0),
                if (presentation == UnitResultPresentation.INSUFFICIENT_EVIDENCE) UnitResultEvaluation.NOT_EVALUATED
                else UnitResultEvaluation.PASSED),
            UnitResultActionUiState("confirm", stringResource(R.string.unit_result_preview_action_1),
                when (presentation) {
                    UnitResultPresentation.FAIL_REQUIRED_ACTION -> UnitResultEvaluation.NOT_PASSED
                    UnitResultPresentation.INSUFFICIENT_EVIDENCE -> UnitResultEvaluation.NOT_EVALUATED
                    else -> UnitResultEvaluation.PASSED
                })
        ),
        criteria = listOf(
            UnitResultCriterionUiState("range", stringResource(R.string.unit_result_criterion_name_0),
                stringResource(R.string.unit_result_criterion_description_0), evaluations[0]),
            UnitResultCriterionUiState("accuracy", stringResource(R.string.unit_result_criterion_name_1),
                stringResource(R.string.unit_result_criterion_description_1), evaluations[1]),
            UnitResultCriterionUiState("fluency", stringResource(R.string.unit_result_criterion_name_2),
                stringResource(R.string.unit_result_criterion_description_2), evaluations[2]),
            UnitResultCriterionUiState("interaction", stringResource(R.string.unit_result_criterion_name_3),
                stringResource(R.string.unit_result_criterion_description_3), evaluations[3]),
            UnitResultCriterionUiState("coherence", stringResource(R.string.unit_result_criterion_name_4),
                stringResource(R.string.unit_result_criterion_description_4), evaluations[4])
        )
    )
}
