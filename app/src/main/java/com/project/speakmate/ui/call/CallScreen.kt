package com.project.speakmate.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.PlacementRole
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.ui.call.components.CallAvatar
import com.project.speakmate.ui.call.components.CallControls
import com.project.speakmate.ui.call.components.CallEndConfirmation
import com.project.speakmate.ui.call.components.CallFailureContent
import com.project.speakmate.ui.call.components.CallStatusHeader
import com.project.speakmate.ui.call.components.CallTranscriptSheet
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.call.state.CallUiState
import com.project.speakmate.ui.components.MessageDialog
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.SecondaryButton
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun CallScreen(
    state: CallUiState,
    onStartCall: () -> Unit,
    onReturnToPreparation: () -> Unit,
    onToggleMute: () -> Unit,
    onEndCall: () -> Unit,
    onConfirmEndCall: () -> Unit,
    onContinueCall: () -> Unit,
    onRetry: () -> Unit,
    onExit: () -> Unit,
    onOpenTranscript: () -> Unit,
    onCloseTranscript: () -> Unit,
    onSelectTranscriptWord: (String, Int, String) -> Unit,
    onCloseDictionary: () -> Unit,
    onPlayDictionaryWord: () -> Unit,
    onSaveDictionaryWord: () -> Unit,
    modifier: Modifier = Modifier,
    transcriptListState: LazyListState = rememberLazyListState(),
    onResumeCall: () -> Unit = {},
    onRetryDictionary: () -> Unit = {},
    initialDictionaryAnchorTop: Dp = 0.dp,
    durationContent: @Composable () -> Unit = {},
    audioMeter: @Composable (Modifier) -> Unit = {},
    onShowTitle: () -> Unit = {},
    onHideTitle: () -> Unit = {},
    onStopDictionaryWord: () -> Unit = {},
    title: String = state.title,
    activityIndex: Int? = state.activityIndex,
    activityLevel: SpeakingLevel? = state.activityLevel,
    activityRole: PlacementRole? = state.activityRole
) {
    BoxWithConstraints(modifier.fillMaxSize().background(DesignColor.RoutingBackground)) {
        if (state.presentation == CallPresentation.FAILED) {
            CallFailureContent(
                description = stringResource(if (state.hasCompletionError) R.string.call_completion_error else when (state.failure) {
                    CallFailure.CONFIGURATION -> R.string.call_failure_configuration
                    CallFailure.ACCESS -> R.string.call_failure_access
                    CallFailure.QUOTA -> R.string.call_failure_quota
                    CallFailure.MODEL -> R.string.call_failure_model
                    CallFailure.TIMEOUT -> R.string.call_failure_timeout
                    CallFailure.PROTOCOL -> R.string.call_failure_protocol
                    CallFailure.MICROPHONE_PERMISSION -> R.string.call_failure_microphone
                    CallFailure.AUDIO -> R.string.call_failure_audio
                    CallFailure.PLAYBACK_BACKLOG -> R.string.call_failure_playback_backlog
                    CallFailure.PAUSE_EXPIRED -> R.string.call_failure_pause_expired
                    CallFailure.NETWORK, null -> R.string.call_failure_description
                }),
                isConnectionFailure = !state.hasCompletionError && (state.failure == CallFailure.NETWORK || state.failure == null),
                retryLabel = if (state.hasCompletionError) stringResource(R.string.call_retry_completion) else when (state.failure) {
                    CallFailure.CONFIGURATION, CallFailure.ACCESS, CallFailure.MODEL -> null
                    CallFailure.MICROPHONE_PERMISSION -> stringResource(R.string.call_recheck_permission)
                    else -> stringResource(R.string.call_retry_connection)
                }, onRetry = onRetry, onExit = onExit
            )
        } else {
            val isConnecting = state.presentation == CallPresentation.CONNECTING
            val isReconnecting = state.presentation == CallPresentation.RECONNECTING
            val isPaused = state.presentation == CallPresentation.PAUSED || isReconnecting
            val isFinalizing = state.presentation == CallPresentation.FINALIZING
            val isReady = state.presentation == CallPresentation.READY
            val headerHeight = maxHeight * if (state.isTranscriptVisible) 0.4f else 0.5f
            Column(Modifier.fillMaxSize()) {
                CallStatusHeader(title, state.presentation, Modifier.heightIn(max = headerHeight).verticalScroll(rememberScrollState()),
                    durationContent, state.isMuted, state.activityType, activityIndex, onShowTitle,
                    isCompact = state.isTranscriptVisible, activityLevel = activityLevel, activityRole = activityRole)
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    if (state.isTranscriptAvailable && state.isTranscriptVisible) {
                        CallTranscriptSheet(state.messages, state.dictionary, transcriptListState, onCloseTranscript, onSelectTranscriptWord,
                            onCloseDictionary, onPlayDictionaryWord, onSaveDictionaryWord, onRetryDictionary,
                            Modifier.fillMaxSize(), initialDictionaryAnchorTop, onStopPronunciation = onStopDictionaryWord)
                    } else {
                        val avatarSize = minOf(maxWidth - 48.dp, (maxHeight - 80.dp).coerceAtLeast(0.dp), 290.dp).coerceAtLeast(0.dp)
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            if (avatarSize > 0.dp) CallAvatar(state.presentation, Modifier.size(avatarSize), state.isMuted)
                            if (!isReady && !isConnecting && !isPaused && !isFinalizing) audioMeter(Modifier.fillMaxWidth().height(32.dp))
                            if (state.presentation == CallPresentation.MICROPHONE_BLOCKED) SecondaryButton(stringResource(R.string.call_continue), onResumeCall)
                            if (isFinalizing || state.presentation == CallPresentation.MICROPHONE_BLOCKED || isPaused) {
                                Text(stringResource(when {
                                    isFinalizing -> R.string.call_finalizing_notice
                                    state.presentation == CallPresentation.MICROPHONE_BLOCKED -> R.string.call_microphone_blocked_notice
                                    isReconnecting -> R.string.call_reconnecting_notice
                                    state.isResuming -> R.string.call_resuming
                                    else -> R.string.call_paused
                                }), Modifier.padding(vertical = 8.dp), style = DesignTypography.Detail, color = DesignColor.Muted)
                            }
                            if (state.isTranscriptAvailable && !isReady && !isConnecting && !isFinalizing) {
                                SecondaryButton(stringResource(R.string.call_transcript), onOpenTranscript)
                                state.messages.lastOrNull()?.let { message ->
                                    Text(message.text, Modifier.padding(vertical = 8.dp), style = DesignTypography.Detail, color = DesignColor.Muted,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                if (isReady) {
                    Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.call_ready), style = DesignTypography.Body, color = DesignColor.Primary)
                        SecondaryButton(stringResource(R.string.call_return_to_preparation), onReturnToPreparation)
                    }
                } else if (isPaused) {
                    Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isReconnecting) PrimaryButton(stringResource(R.string.call_continue), onResumeCall, enabled = !state.isResuming)
                        SecondaryButton(stringResource(R.string.call_end), onEndCall)
                    }
                } else {
                    CallControls(state.isMuted, isConnecting, onToggleMute, onEndCall, onOpenTranscript,
                        isTranscriptAvailable = state.isTranscriptAvailable, isEnabled = !isFinalizing)
                }
            }
            if (state.isEndConfirmationVisible) CallEndConfirmation(
                description = stringResource(when {
                    !state.hasUserSpeech -> R.string.call_end_silent_description
                    state.activityType == CallActivityType.FREE_PRACTICE -> R.string.call_end_free_spoken_description
                    state.activityType == CallActivityType.TRAINING_UNIT -> R.string.call_end_confirmation_description
                    else -> R.string.call_end_assessment_description
                }),
                dismissLabel = stringResource(if (isPaused) R.string.call_back else R.string.call_continue),
                onConfirmEndCall = onConfirmEndCall, onContinueCall = onContinueCall
            )
        }
        if (state.isTitleVisible) MessageDialog(
            stringResource(R.string.call_show_full_title), title, onHideTitle
        ) {
            PrimaryButton(stringResource(R.string.content_close), onHideTitle)
        }
    }
}
