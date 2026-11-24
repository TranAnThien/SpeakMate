package com.project.speakmate.ui.history.components

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
import com.project.speakmate.ui.history.state.SessionActionUiState
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun SessionRequiredActions(actions: List<SessionActionUiState>, modifier: Modifier = Modifier) {
    SpeakMateCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 16.dp,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TitleText(text = stringResource(R.string.unit_result_required_actions))
        actions.forEachIndexed { index, action ->
            key(index, action.id) {
                AssessmentActionRow(
                    text = action.text.ifBlank { stringResource(R.string.history_action_unavailable) },
                    statusLabel =
                        stringResource(
                            if (action.isCompleted) R.string.unit_result_action_completed
                            else R.string.unit_result_action_incomplete
                        ),
                    iconRes =
                        if (action.isCompleted) R.drawable.ic_placement_passed else R.drawable.ic_placement_failed,
                    statusColor = if (action.isCompleted) DesignColor.Success else DesignColor.Error,
                    iconBackground =
                        if (action.isCompleted) DesignColor.SuccessContainer else DesignColor.ErrorSurface
                )
            }
        }
    }
}
