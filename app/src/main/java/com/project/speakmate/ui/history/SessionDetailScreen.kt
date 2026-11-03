package com.project.speakmate.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.AssessmentCriterionCard
import com.project.speakmate.ui.components.BackAppBar
import com.project.speakmate.ui.components.ContentErrorDialog
import com.project.speakmate.ui.components.ContentSkeletonCards
import com.project.speakmate.ui.components.ContentStatusPanel
import com.project.speakmate.ui.history.components.SessionRequiredActions
import com.project.speakmate.ui.history.components.SessionTranscriptMessage
import com.project.speakmate.ui.history.state.SessionDetailPresentation
import com.project.speakmate.ui.history.state.SessionDetailUiState
import com.project.speakmate.ui.history.state.SessionMessageUiState
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
    listState: LazyListState = rememberLazyListState()
) {
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        Column(Modifier.fillMaxSize()) {
            BackAppBar(stringResource(R.string.session_detail_header), onBack)
            if (state.presentation == SessionDetailPresentation.LOAD_ERROR) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    ContentStatusPanel(R.drawable.ic_load_error, stringResource(R.string.session_detail_error_title),
                        stringResource(R.string.session_detail_error_description), stringResource(R.string.content_retry),
                        stringResource(R.string.session_detail_return_history), onRetrySession, onReturnToHistory,
                        Modifier.verticalScroll(rememberScrollState()))
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState,
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (state.presentation) {
                        SessionDetailPresentation.COMPLETED, SessionDetailPresentation.TRANSCRIPT_ERROR -> {
                            item(key = "session-summary", contentType = SessionDetailUiState::class) {
                                Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(state.title, style = DesignTypography.Heading, color = DesignColor.Text, textAlign = TextAlign.Center)
                                        Text(state.metadata, style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center)
                                    }
                                    SessionRequiredActions(state.requiredActions)
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(state.criteriaHeading, style = DesignTypography.Title, color = DesignColor.Text)
                                        state.criteria.forEachIndexed { index, criterion ->
                                            key(index, criterion.id) {
                                                AssessmentCriterionCard(criterion.title, criterion.description,
                                                    stringResource(if (criterion.isPassed) R.string.placement_passed else R.string.placement_not_passed),
                                                    if (criterion.isPassed) DesignColor.Success else DesignColor.Error)
                                            }
                                        }
                                    }
                                }
                            }
                            item(key = "transcript-heading", contentType = String::class) {
                                Text(stringResource(R.string.session_detail_transcript_heading), style = DesignTypography.Title, color = DesignColor.Text,
                                    modifier = Modifier.padding(bottom = 8.dp))
                            }
                            if (state.presentation == SessionDetailPresentation.COMPLETED) {
                                itemsIndexed(state.messages, key = { index, message -> "message:$index:${message.id}" },
                                    contentType = { _, _ -> SessionMessageUiState::class }) { _, message ->
                                    SessionTranscriptMessage(message)
                                }
                            } else {
                                item(key = "transcript-error", contentType = SessionDetailPresentation.TRANSCRIPT_ERROR) {
                                    ContentStatusPanel(R.drawable.ic_load_error, stringResource(R.string.session_detail_transcript_error_title),
                                        stringResource(R.string.session_detail_transcript_error_description),
                                        stringResource(R.string.session_detail_retry_transcript), "", onRetryTranscript, onReturnToHistory)
                                }
                            }
                        }
                        SessionDetailPresentation.LOADING -> item(key = "session-loading", contentType = SessionDetailPresentation.LOADING) {
                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                Text(stringResource(R.string.session_detail_loading_title), style = DesignTypography.Title, color = DesignColor.Primary)
                                Text(stringResource(R.string.session_detail_loading_description), style = DesignTypography.Detail, color = DesignColor.Muted)
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    repeat(3) { ContentSkeletonCards(1) }
                                }
                            }
                        }
                        SessionDetailPresentation.ERROR_DIALOG, SessionDetailPresentation.LOAD_ERROR -> Unit
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (state.presentation == SessionDetailPresentation.ERROR_DIALOG) {
            ContentErrorDialog(stringResource(R.string.session_detail_error_title), stringResource(R.string.session_detail_error_dialog_description),
                onRetrySession, onDismissError)
        }
    }
}
