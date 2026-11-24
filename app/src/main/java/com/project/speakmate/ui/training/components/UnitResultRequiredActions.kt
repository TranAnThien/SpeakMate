package com.project.speakmate.ui.training.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.assessment.AssessmentActionRow
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.training.state.UnitResultActionUiState
import com.project.speakmate.ui.training.state.UnitResultEvaluation

@Composable
fun UnitResultRequiredActions(actions: List<UnitResultActionUiState>, modifier: Modifier = Modifier) {
    SpeakMateCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 16.dp,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TitleText(text = stringResource(R.string.unit_result_required_actions))
        actions.forEachIndexed { index, action ->
            key(index, action.id) {
                AssessmentActionRow(
                    text = action.text,
                    statusLabel =
                        stringResource(
                            when (action.evaluation) {
                                UnitResultEvaluation.PASSED -> R.string.unit_result_action_completed
                                UnitResultEvaluation.NOT_PASSED -> R.string.unit_result_action_incomplete
                                UnitResultEvaluation.NOT_EVALUATED -> R.string.home_unassessed
                            }
                        ),
                    iconRes =
                        when (action.evaluation) {
                            UnitResultEvaluation.PASSED -> R.drawable.ic_placement_passed
                            UnitResultEvaluation.NOT_PASSED -> R.drawable.ic_placement_failed
                            UnitResultEvaluation.NOT_EVALUATED -> R.drawable.ic_training_result_unknown
                        },
                    statusColor =
                        when (action.evaluation) {
                            UnitResultEvaluation.PASSED -> DesignColor.Success
                            UnitResultEvaluation.NOT_PASSED -> DesignColor.Error
                            UnitResultEvaluation.NOT_EVALUATED -> DesignColor.Muted
                        },
                    iconBackground =
                        when (action.evaluation) {
                            UnitResultEvaluation.PASSED -> DesignColor.SuccessContainer
                            UnitResultEvaluation.NOT_PASSED -> DesignColor.ErrorSurface
                            UnitResultEvaluation.NOT_EVALUATED -> DesignColor.NeutralSurface
                        }
                )
            }
        }
    }
}
