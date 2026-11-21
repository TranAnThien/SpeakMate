package com.project.speakmate.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.PracticeCompletionStatus
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.history.state.HistoryPendingSessionUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun HistoryPendingSessionCard(
    session: HistoryPendingSessionUiState,
    onRetrySave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeLabel = stringResource(when (session.practiceType) {
        PracticeType.FREE_PRACTICE -> R.string.history_type_free_practice
        PracticeType.GOAL_ASSESSMENT -> R.string.goal_assessment_header
        PracticeType.TRAINING_UNIT -> R.string.history_type_unit
    })
    val statusLabel = stringResource(when (session.status) {
        PracticeCompletionStatus.GRADING -> R.string.history_pending_grading
        PracticeCompletionStatus.SAVING -> R.string.history_pending_saving
        PracticeCompletionStatus.SAVE_FAILED -> R.string.history_pending_save_failed
    })
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DesignColor.Surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.history_pending_metadata, typeLabel, session.performedDate, session.performedAt), style = DesignTypography.Detail, color = DesignColor.Muted)
        Text(session.title, style = DesignTypography.Title, color = DesignColor.Text)
        Text(statusLabel, style = DesignTypography.Detail, color = if (session.status == PracticeCompletionStatus.SAVE_FAILED) DesignColor.Error else DesignColor.Muted)
        if (session.status != PracticeCompletionStatus.GRADING) {
            PrimaryButton(
                text = stringResource(R.string.unit_result_retry_save),
                onClick = onRetrySave,
                enabled = session.status == PracticeCompletionStatus.SAVE_FAILED
            )
        }
    }
}
