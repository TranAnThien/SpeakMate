package com.project.speakmate.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.AssessmentActionRow
import com.project.speakmate.ui.history.state.SessionActionUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun SessionRequiredActions(actions: List<SessionActionUiState>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.unit_result_required_actions), style = DesignTypography.Title, color = DesignColor.Text)
        actions.forEachIndexed { index, action ->
            key(index, action.id) {
                AssessmentActionRow(action.text,
                    stringResource(if (action.isCompleted) R.string.unit_result_action_completed else R.string.unit_result_action_incomplete),
                    if (action.isCompleted) R.drawable.ic_placement_passed else R.drawable.ic_placement_failed,
                    if (action.isCompleted) DesignColor.Success else DesignColor.Error,
                    if (action.isCompleted) DesignColor.ResultSuccessSurface else DesignColor.ErrorSurface)
            }
        }
    }
}
