package com.project.speakmate.ui.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.PracticeCompletionStatus
import com.project.speakmate.ui.components.assessment.practiceSaveFailureText
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.button.SecondaryButton
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.state.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.history.rememberHistoryDate
import com.project.speakmate.ui.history.rememberHistoryPracticeTypeLabel
import com.project.speakmate.ui.history.rememberHistoryTime
import com.project.speakmate.ui.history.state.HistoryPendingSessionUiState
import com.project.speakmate.ui.theme.DesignColor

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
    val typeLabel = rememberHistoryPracticeTypeLabel(session.practiceType)
    val statusLabel =
        if (session.status == PracticeCompletionStatus.SAVE_FAILED) practiceSaveFailureText(session.saveFailure)
        else
            stringResource(
                when (session.status) {
                    PracticeCompletionStatus.GRADING -> R.string.history_pending_grading
                    PracticeCompletionStatus.AWAITING_GRADING -> R.string.assessment_pending_title
                    PracticeCompletionStatus.SAVING -> R.string.history_pending_saving
                    PracticeCompletionStatus.SAVE_FAILED -> R.string.history_pending_save_failed
                }
            )
    SpeakMateCard(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailText(
            text =
                if (session.startedAtMillis == null) typeLabel
                else stringResource(R.string.history_pending_metadata, typeLabel, date, time)
        )
        TitleText(text = session.title.ifBlank { stringResource(R.string.history_recovery_title) })
        DetailText(
            text = statusLabel,
            color =
                if (session.status == PracticeCompletionStatus.SAVE_FAILED) DesignColor.Error
                else DesignColor.Muted
        )
        if (session.status == PracticeCompletionStatus.AWAITING_GRADING ||
            session.status == PracticeCompletionStatus.SAVE_FAILED && session.canRetrySave) {
            PrimaryButton(
                text =
                    stringResource(
                        if (session.status == PracticeCompletionStatus.AWAITING_GRADING)
                            R.string.assessment_retry_grading
                        else R.string.unit_result_retry_save
                    ),
                onClick = onRetrySave,
                enabled =
                    (session.status == PracticeCompletionStatus.SAVE_FAILED && session.canRetrySave) ||
                        session.status == PracticeCompletionStatus.AWAITING_GRADING
            )
        }
        if (session.status == PracticeCompletionStatus.GRADING || session.status == PracticeCompletionStatus.SAVING)
            SpeakMateLoadingIndicator()
        if (session.status == PracticeCompletionStatus.SAVING ||
            session.status == PracticeCompletionStatus.SAVE_FAILED)
            SecondaryButton(stringResource(R.string.history_view_temporary), onOpen)
        if (session.status == PracticeCompletionStatus.AWAITING_GRADING) {
            SecondaryButton(stringResource(R.string.assessment_save_unassessed), onSaveUnassessed)
        }
        if (session.status == PracticeCompletionStatus.AWAITING_GRADING ||
            session.status == PracticeCompletionStatus.SAVE_FAILED)
            SecondaryButton(stringResource(R.string.history_recovery_discard), onDiscard)
    }
}
