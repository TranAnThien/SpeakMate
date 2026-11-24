package com.project.speakmate.ui.training.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.training.state.UnitResultPresentation

@Composable
fun UnitResultOutcome(presentation: UnitResultPresentation, explanation: String, modifier: Modifier = Modifier) {
    val surface =
        when (presentation) {
            UnitResultPresentation.PASS -> DesignColor.SuccessContainer
            UnitResultPresentation.FAIL_CRITERIA,
            UnitResultPresentation.FAIL_REQUIRED_ACTION -> DesignColor.ErrorSurface
            UnitResultPresentation.PARTIAL,
            UnitResultPresentation.INSUFFICIENT_EVIDENCE -> DesignColor.ResultWarningSurface
            else -> DesignColor.Background
        }
    TrainingResultOutcome(
        title =
            stringResource(
                when (presentation) {
                    UnitResultPresentation.PASS -> R.string.placement_passed
                    UnitResultPresentation.PARTIAL -> R.string.unit_result_partial
                    UnitResultPresentation.FAIL_REQUIRED_ACTION -> R.string.unit_result_missing_action
                    UnitResultPresentation.FAIL_CRITERIA -> R.string.placement_not_passed
                    UnitResultPresentation.INSUFFICIENT_EVIDENCE -> R.string.unit_result_insufficient
                    UnitResultPresentation.PROCESSING -> R.string.unit_result_processing
                }
            ),
        description = explanation,
        surface = surface,
        isProcessing = presentation == UnitResultPresentation.PROCESSING,
        isPassed = presentation == UnitResultPresentation.PASS,
        modifier = modifier
    )
}
