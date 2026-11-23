package com.project.speakmate.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.HistoryReadFailure
import com.project.speakmate.domain.model.PracticeCompletionStatus
import com.project.speakmate.ui.components.AssessmentCriterionCard
import com.project.speakmate.ui.components.BackAppBar
import com.project.speakmate.ui.components.ContentStatusPanel
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.SecondaryButton
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.TranscriptMessageUiState
import com.project.speakmate.ui.components.assessmentCriterionTitle
import com.project.speakmate.ui.components.practiceSaveFailureText
import com.project.speakmate.ui.history.components.SessionRequiredActions
import com.project.speakmate.ui.history.components.SessionTranscriptMessage
import com.project.speakmate.ui.history.state.SessionDetailPresentation
import com.project.speakmate.ui.history.state.SessionDetailUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun SessionDetailScreen(
    state: SessionDetailUiState,
    onBack: () -> Unit,
    onRetrySession: () -> Unit,
    onRetryTranscript: () -> Unit,
    onDismissError: () -> Unit,
    onReturnToHistory: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onRetryGrading: () -> Unit = {},
    onSaveUnassessed: () -> Unit = {},
    onRetrySave: () -> Unit = {}
) {
    val metadata = listOf(rememberHistoryDateTime(state.startedAtMillis), state.duration).filter(String::isNotBlank).joinToString(" • ")
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            BackAppBar(stringResource(R.string.session_detail_header), onBack)
            if (state.presentation == SessionDetailPresentation.LOAD_ERROR) {
                val isBlocked = state.loadFailure == HistoryReadFailure.NOT_FOUND || state.loadFailure == HistoryReadFailure.ACCESS_DENIED
                Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    ContentStatusPanel(R.drawable.ic_load_error, stringResource(R.string.session_detail_error_title),
                        stringResource(when {
                            state.loadFailure == HistoryReadFailure.NOT_FOUND -> R.string.session_detail_not_found
                            state.loadFailure == HistoryReadFailure.ACCESS_DENIED -> R.string.session_detail_access_denied
                            else -> R.string.session_detail_error_description
                        }), stringResource(if (isBlocked) R.string.session_detail_return_history else R.string.content_retry),
                        if (isBlocked) "" else stringResource(R.string.session_detail_return_history),
                        if (isBlocked) onReturnToHistory else onRetrySession, onReturnToHistory, Modifier.verticalScroll(rememberScrollState()))
                }
            } else LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState,
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (state.presentation) {
                    SessionDetailPresentation.COMPLETED, SessionDetailPresentation.TRANSCRIPT_ERROR -> {
                        item(key = "session-summary", contentType = SessionDetailUiState::class) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(stringResource(when {
                                    state.isSaved && state.isTemporary -> R.string.session_detail_saved_temporary
                                    state.isTemporary -> R.string.practice_summary_pending_status
                                    else -> R.string.practice_summary_saved_status
                                }), style = DesignTypography.Label, color = DesignColor.Primary)
                                Text(metadata, style = DesignTypography.Detail, color = DesignColor.Muted)
                                Text(state.title.ifBlank { stringResource(R.string.history_content_unavailable) }, style = DesignTypography.Heading, color = DesignColor.Text)
                                state.result?.let { result -> Text(historyResultText(result), style = DesignTypography.Title, color = DesignColor.Primary) }
                                if (state.pendingStatus == PracticeCompletionStatus.AWAITING_GRADING) {
                                    Text(stringResource(R.string.assessment_pending_title), style = DesignTypography.Title, color = DesignColor.Primary)
                                    PrimaryButton(stringResource(R.string.assessment_retry_grading), onRetryGrading)
                                    SecondaryButton(stringResource(R.string.assessment_save_unassessed), onSaveUnassessed)
                                }
                                if (state.pendingStatus == PracticeCompletionStatus.SAVE_FAILED) {
                                    Text(practiceSaveFailureText(state.saveFailure), style = DesignTypography.Detail, color = DesignColor.Error)
                                    if (state.canRetrySave) PrimaryButton(stringResource(R.string.unit_result_retry_save), onRetrySave)
                                }
                                if (state.pendingStatus == PracticeCompletionStatus.SAVING && !state.isSaved) SpeakMateLoadingIndicator()
                                if (state.hasSavedRefreshError) {
                                    Text(stringResource(R.string.session_detail_saved_refresh_error), style = DesignTypography.Detail, color = DesignColor.Error)
                                    SecondaryButton(stringResource(R.string.content_reload), onRetrySession)
                                }
                                if (!state.isAssessed) Text(stringResource(R.string.session_unassessed_feedback),
                                    style = DesignTypography.Body, color = DesignColor.Text)
                                state.feedbackSummary?.takeIf(String::isNotBlank)?.let { Text(it, style = DesignTypography.Body, color = DesignColor.Text) }
                                if (state.requiredActions.isNotEmpty()) SessionRequiredActions(state.requiredActions)
                                if (state.criteria.isNotEmpty()) {
                                    Text(stringResource(R.string.session_runtime_criteria_heading), style = DesignTypography.Title, color = DesignColor.Text)
                                    state.criteria.forEach { criterion -> key(criterion.id) {
                                        val title = if (criterion.criterion == null) criterion.title else assessmentCriterionTitle(checkNotNull(criterion.criterion))
                                        AssessmentCriterionCard(title,
                                            criterion.description.ifBlank { stringResource(if (criterion
                                                .isPassed) R.string.placement_criterion_met_feedback else R.string.placement_criterion_unmet_feedback) },
                                            stringResource(if (criterion.isPassed) R.string.placement_passed else R.string.placement_not_passed),
                                                if (criterion.isPassed) DesignColor.Success else DesignColor.Error)
                                    } }
                                }
                            }
                        }
                        item(key = "transcript-heading", contentType = String::class) {
                            Text(stringResource(R.string.session_detail_transcript_heading), style = DesignTypography.Title, color = DesignColor.Text)
                            if (state.isTranscriptTemporary) Text(stringResource(R.string.session_detail_temporary_transcript),
                                style = DesignTypography.Label, color = DesignColor.Muted)
                        }
                        if (state.isTranscriptLoading) item(key = "transcript-loading", contentType = SessionDetailPresentation.LOADING) { SpeakMateLoadingIndicator() }
                        items(state.messages.distinctBy { it.id }, key = { "message:" + it.id }, contentType = { TranscriptMessageUiState::class }) { SessionTranscriptMessage(it) }
                        if (state.presentation == SessionDetailPresentation.TRANSCRIPT_ERROR && !state.isTranscriptLoading) item(key = "transcript-error",
                            contentType = SessionDetailPresentation.TRANSCRIPT_ERROR) {
                            val isBlocked = state.transcriptFailure == HistoryReadFailure.ACCESS_DENIED
                            ContentStatusPanel(R.drawable.ic_load_error, stringResource(R.string.session_detail_transcript_error_title),
                                stringResource(if (isBlocked) R.string.session_detail_access_denied else R.string.session_detail_transcript_error_description),
                                stringResource(if (isBlocked) R.string.session_detail_return_history else R.string.session_detail_retry_transcript), "",
                                    if (isBlocked) onReturnToHistory else onRetryTranscript, onReturnToHistory)
                        }
                        else if (!state.hasPendingContent) item(key = "pending-content", contentType = PracticeCompletionStatus.AWAITING_GRADING) {
                            Text(stringResource(R.string.session_detail_pending_content), style = DesignTypography.Body, color = DesignColor.Muted)
                        }
                        else if (!state.isTranscriptLoading && state.messages.isEmpty()) item(key = "transcript-empty", contentType = TranscriptMessageUiState::class) {
                            Text(stringResource(R.string.session_detail_transcript_empty), style = DesignTypography.Body, color = DesignColor.Muted)
                        }
                    }
                    SessionDetailPresentation.LOADING -> item(key = "session-loading", contentType = SessionDetailPresentation.LOADING) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            SpeakMateLoadingIndicator()
                            Text(stringResource(R.string.session_detail_loading_title), style = DesignTypography.Title, color = DesignColor.Primary)
                        }
                    }
                    SessionDetailPresentation.LOAD_ERROR -> Unit
                }
            }
        }
    }
}
