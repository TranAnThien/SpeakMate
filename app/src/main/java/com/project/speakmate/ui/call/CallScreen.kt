package com.project.speakmate.ui.call

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.ui.call.components.CallAvatar
import com.project.speakmate.ui.call.components.CallControls
import com.project.speakmate.ui.call.components.CallEndConfirmation
import com.project.speakmate.ui.call.components.CallFailureContent
import com.project.speakmate.ui.call.components.CallSecondaryButton
import com.project.speakmate.ui.call.components.CallStatusHeader
import com.project.speakmate.ui.call.components.CallTranscriptSheet
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.call.state.CallUiState
import com.project.speakmate.ui.components.PrimaryButton
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
    onSelectTranscriptWord: (String, String, Rect) -> Unit,
    onCloseDictionary: () -> Unit,
    onPlayDictionaryWord: () -> Unit,
    onSaveDictionaryWord: () -> Unit,
    modifier: Modifier = Modifier,
    transcriptScrollState: ScrollState = rememberScrollState(),
    onResumeCall: () -> Unit = {},
    durationContent: @Composable () -> Unit = {},
    audioMeter: @Composable (Modifier) -> Unit = {}
) {
    BoxWithConstraints(modifier.fillMaxSize().background(DesignColor.RoutingBackground)) {
        if (state.presentation == CallPresentation.FAILED) {
            CallFailureContent(
                description = stringResource(when (state.failure) {
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
                isConnectionFailure = state.failure == CallFailure.NETWORK || state.failure == null,
                retryLabel = when (state.failure) {
                    CallFailure.CONFIGURATION, CallFailure.ACCESS, CallFailure.MODEL -> null
                    CallFailure.MICROPHONE_PERMISSION -> stringResource(R.string.call_recheck_permission)
                    else -> stringResource(R.string.call_retry_connection)
                },
                onRetry = onRetry,
                onExit = onExit
            )
        } else {
            val contentHeight = maxHeight.coerceAtLeast(720.dp)
            val isConnecting = state.presentation == CallPresentation.CONNECTING
            val isPaused = state.presentation == CallPresentation.PAUSED
            val isReady = state.presentation == CallPresentation.READY
            Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).height(contentHeight)) {
                CallStatusHeader(state.title, state.presentation, durationContent = durationContent)
                CallAvatar(state.presentation, Modifier.align(Alignment.Center).offset(y = if (isConnecting) (-16).dp else (-9.5).dp))
                if (!isReady && !isConnecting && !isPaused) {
                    audioMeter(Modifier.align(Alignment.Center).offset(y = 158.dp))
                }
                if (state.presentation == CallPresentation.BARGE_IN || state.presentation == CallPresentation.AUDIO_UNCLEAR) {
                    Text(stringResource(if (state.presentation == CallPresentation.BARGE_IN) R.string.call_barge_in_notice else R.string.call_audio_unclear_notice),
                        style = if (state.presentation == CallPresentation.BARGE_IN) DesignTypography.CallStatus else DesignTypography.Detail,
                        color = DesignColor.Primary,
                        modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 32.dp)
                            .padding(top = if (state.presentation == CallPresentation.BARGE_IN) 140.dp else 148.dp)
                            .fillMaxWidth().heightIn(min = if (state.presentation == CallPresentation.BARGE_IN) 52.dp else 72.dp)
                            .background(if (state.presentation == CallPresentation.BARGE_IN) DesignColor.PaleBlue else DesignColor.SecondaryButton,
                                RoundedCornerShape(16.dp)).padding(16.dp))
                }
                if (isReady || isPaused) {
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when {
                            isReady -> {
                                PrimaryButton(stringResource(R.string.call_start), onStartCall)
                                CallSecondaryButton(
                                    stringResource(if (state.isStandaloneCall) R.string.call_exit else R.string.call_return_to_preparation),
                                    onReturnToPreparation
                                )
                            }
                            state.isResuming -> Text(
                                stringResource(R.string.call_resuming),
                                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                style = DesignTypography.Body,
                                color = DesignColor.Muted,
                                textAlign = TextAlign.Center
                            )
                            else -> PrimaryButton(stringResource(R.string.call_continue), onResumeCall)
                        }
                        if (isPaused) CallSecondaryButton(stringResource(R.string.call_end), onEndCall)
                    }
                } else {
                    CallControls(state.isMuted, isConnecting, onToggleMute, onEndCall,
                        onOpenTranscript, Modifier.align(Alignment.BottomCenter), isTranscriptAvailable = state.isTranscriptAvailable)
                }
            }
            if (state.isTranscriptAvailable && state.isTranscriptVisible) {
                CallTranscriptSheet(state.messages, state.dictionary, transcriptScrollState, onCloseTranscript, onSelectTranscriptWord,
                    onCloseDictionary, onPlayDictionaryWord, onSaveDictionaryWord)
            }
            if (state.isEndConfirmationVisible) {
                CallEndConfirmation(
                    description = stringResource(
                        if (state.isStandaloneCall) R.string.call_end_live_description else R.string.call_end_confirmation_description
                    ),
                    dismissLabel = stringResource(if (isPaused) R.string.call_back else R.string.call_continue),
                    onConfirmEndCall = onConfirmEndCall,
                    onContinueCall = onContinueCall
                )
            }
        }
    }
}
