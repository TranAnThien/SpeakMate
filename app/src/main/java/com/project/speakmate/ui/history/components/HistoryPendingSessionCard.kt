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
import com.project.speakmate.ui.components.SecondaryButton
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.practiceSaveFailureText
import com.project.speakmate.ui.history.rememberHistoryDate
import com.project.speakmate.ui.history.rememberHistoryTime
import com.project.speakmate.ui.history.state.HistoryPendingSessionUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun HistoryPendingSessionCard(
    session: HistoryPendingSessionUiState,
    onRetrySave: () -> Unit,
    onSaveUnassessed: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = {}
) {
    val date = rememberHistoryDate(session.startedAtMillis)
    val time = rememberHistoryTime(session.startedAtMillis)
    val typeLabel = stringResource(when (session.practiceType) {
        PracticeType.FREE_PRACTICE -> R.string.history_type_free_practice
        PracticeType.GOAL_ASSESSMENT -> R.string.goal_assessment_header
        PracticeType.TRAINING_UNIT -> R.string.history_type_unit
        null -> R.string.history_recovery_title
    })
    val statusLabel = if (session.status == PracticeCompletionStatus.SAVE_FAILED) practiceSaveFailureText(session.saveFailure) else stringResource(when (session.status) {
        PracticeCompletionStatus.GRADING -> R.string.history_pending_grading
        PracticeCompletionStatus.AWAITING_GRADING -> R.string.assessment_pending_title
        PracticeCompletionStatus.SAVING -> R.string.history_pending_saving
        PracticeCompletionStatus.SAVE_FAILED -> R.string.history_pending_save_failed
    })
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DesignColor.Surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(if (session.startedAtMillis == null) typeLabel else stringResource(R.string.history_pending_metadata, typeLabel, date, time),
            style = DesignTypography.Detail, color = DesignColor.Muted)
        Text(session.title.ifBlank { stringResource(R.string.history_recovery_title) }, style = DesignTypography.Title, color = DesignColor.Text)
        Text(statusLabel, style = DesignTypography.Detail, color = if (session.status == PracticeCompletionStatus.SAVE_FAILED) DesignColor.Error else DesignColor.Muted)
        if (session.status == PracticeCompletionStatus.AWAITING_GRADING || session.status == PracticeCompletionStatus.SAVE_FAILED && session.canRetrySave) {
            PrimaryButton(
                text = stringResource(if (session.status == PracticeCompletionStatus.AWAITING_GRADING) R.string.assessment_retry_grading else R.string.unit_result_retry_save),
                onClick = onRetrySave,
                enabled = (session.status == PracticeCompletionStatus.SAVE_FAILED && session.canRetrySave) || session.status == PracticeCompletionStatus.AWAITING_GRADING
            )
        }
        if (session.status == PracticeCompletionStatus.GRADING || session.status == PracticeCompletionStatus.SAVING) SpeakMateLoadingIndicator()
        if (session.status == PracticeCompletionStatus.SAVING ||
            session.status == PracticeCompletionStatus.SAVE_FAILED) SecondaryButton(stringResource(R.string.history_view_temporary), onOpen)
        if (session.status == PracticeCompletionStatus.AWAITING_GRADING) {
            SecondaryButton(stringResource(R.string.assessment_save_unassessed), onSaveUnassessed)
        }
        if (session.status == PracticeCompletionStatus.AWAITING_GRADING ||
            session.status == PracticeCompletionStatus.SAVE_FAILED) SecondaryButton(stringResource(R.string.history_recovery_discard), onDiscard)
    }
}
