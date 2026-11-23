package com.project.speakmate.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.ui.components.BackAppBar
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.SecondaryButton
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.practiceSaveFailureText
import com.project.speakmate.ui.practice.components.PracticeTextCard
import com.project.speakmate.ui.practice.state.PracticeSummaryPresentation
import com.project.speakmate.ui.practice.state.PracticeSummaryUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeSummaryScreen(
    state: PracticeSummaryUiState,
    onBack: () -> Unit,
    onViewTranscript: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
    onRetrySave: () -> Unit = {},
    onCloseTranscript: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val isSaved = state.presentation == PracticeSummaryPresentation.SAVED
    val isSaving = state.presentation == PracticeSummaryPresentation.SAVING || state.presentation == PracticeSummaryPresentation.FINALIZING
    val isUnavailable = state.presentation == PracticeSummaryPresentation.UNAVAILABLE
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(stringResource(R.string.practice_summary_header), onBack)
        Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(if (isSaved) R.string.practice_summary_saved_status else R.string.practice_summary_pending_status),
                    style = DesignTypography.Label, color = DesignColor.Primary)
                Text(stringResource(if (isUnavailable) R.string.unit_result_unavailable else R.string.practice_summary_title),
                    style = DesignTypography.Heading, color = DesignColor.Text)
                Text(if (state.presentation == PracticeSummaryPresentation.SAVE_FAILED) practiceSaveFailureText(state.saveFailure) else stringResource(when {
                    isSaved -> R.string.practice_summary_description
                    isUnavailable -> if (!state.canReload) R.string.practice_summary_cannot_open else R.string.practice_summary_unavailable
                    state.presentation == PracticeSummaryPresentation.FINALIZING -> R.string.practice_summary_finalizing
                    state.presentation == PracticeSummaryPresentation.SAVE_FAILED -> R.string.unit_result_save_failed
                    else -> R.string.practice_summary_saving
                }), style = DesignTypography.Body, color = DesignColor.Muted)
                if (isSaved) Text(stringResource(R.string.session_unassessed_feedback),
                    style = DesignTypography.Body, color = DesignColor.Text)
                if (state.scenarioTitle.isNotBlank()) PracticeTextCard(state.scenarioTitle, state.sessionDescription, true)
                if (isSaving) SpeakMateLoadingIndicator()
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.presentation == PracticeSummaryPresentation.SAVE_FAILED) PrimaryButton(stringResource(R.string.unit_result_retry_save), onRetrySave,
                enabled = state.canRetrySave)
            if (isUnavailable) {
                if (state.canReload) PrimaryButton(stringResource(R.string.content_reload), onRetry)
            }
            else SecondaryButton(stringResource(if (isSaved) R.string.practice_view_transcript else R.string.practice_summary_temporary_transcript), onViewTranscript,
                enabled = isSaved || state.messages.isNotEmpty())
            PrimaryButton(stringResource(R.string.practice_summary_done), onGoHome)
        }
    }
    if (state.isTranscriptVisible) {
        ModalBottomSheet(onDismissRequest = onCloseTranscript) {
            Text(stringResource(if (isSaved) R.string.practice_summary_saved_status else R.string.practice_summary_temporary_notice),
                Modifier.padding(24.dp), style = DesignTypography.Title, color = DesignColor.Primary)
            LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.messages, key = { it.id }, contentType = { it.sender }) { message ->
                    PracticeTextCard(stringResource(if (message.sender == SenderType.USER) R.string.call_transcript_user else R.string.call_transcript_ai), message.text, true)
                }
            }
            SecondaryButton(stringResource(R.string.call_dictionary_close), onCloseTranscript, Modifier.padding(24.dp))
        }
    }
}
