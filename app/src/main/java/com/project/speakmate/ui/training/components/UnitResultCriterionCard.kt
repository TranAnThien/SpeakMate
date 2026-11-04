package com.project.speakmate.ui.training.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.ui.components.AssessmentCriterionCard
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.training.state.UnitResultCriterionUiState
import com.project.speakmate.ui.training.state.UnitResultEvaluation

@Composable
fun UnitResultCriterionCard(criterion: UnitResultCriterionUiState, modifier: Modifier = Modifier) {
    AssessmentCriterionCard(
        title = criterion.name,
        description = criterion.description,
        statusLabel = stringResource(when (criterion.evaluation) {
            UnitResultEvaluation.PASSED -> R.string.placement_passed
            UnitResultEvaluation.NOT_PASSED -> R.string.placement_not_passed
            UnitResultEvaluation.NOT_EVALUATED -> R.string.home_unassessed
        }),
        statusColor = when (criterion.evaluation) {
            UnitResultEvaluation.PASSED -> DesignColor.Success
            UnitResultEvaluation.NOT_PASSED -> DesignColor.Error
            UnitResultEvaluation.NOT_EVALUATED -> DesignColor.Muted
        },
        modifier = modifier
    )
}
