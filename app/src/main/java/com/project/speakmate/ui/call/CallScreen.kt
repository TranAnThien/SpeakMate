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
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
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
    transcriptScrollState: ScrollState = rememberScrollState()
) {
    BoxWithConstraints(modifier.fillMaxSize().background(DesignColor.RoutingBackground)) {
        if (state.presentation == CallPresentation.FAILED) {
            CallFailureContent(onRetry, onExit)
        } else {
            val contentHeight = maxHeight.coerceAtLeast(720.dp)
            val isConnecting = state.presentation == CallPresentation.CONNECTING
            Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).height(contentHeight)) {
                CallStatusHeader(state.title, state.durationLabel, state.presentation)
                CallAvatar(state.presentation, Modifier.align(Alignment.Center).offset(y = if (isConnecting) (-16).dp else (-9.5).dp))
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
                if (state.presentation == CallPresentation.READY) {
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryButton(stringResource(R.string.call_start), onStartCall)
                        CallSecondaryButton(stringResource(R.string.call_return_to_preparation), onReturnToPreparation)
                    }
                } else {
                    CallControls(state.presentation == CallPresentation.MUTED, isConnecting, onToggleMute, onEndCall,
                        onOpenTranscript, Modifier.align(Alignment.BottomCenter), isTranscriptAvailable = state.isTranscriptAvailable)
                }
            }
            if (state.isTranscriptAvailable && state.isTranscriptVisible) {
                CallTranscriptSheet(state.messages, state.dictionary, transcriptScrollState, onCloseTranscript, onSelectTranscriptWord,
                    onCloseDictionary, onPlayDictionaryWord, onSaveDictionaryWord)
            }
            if (state.isEndConfirmationVisible) CallEndConfirmation(onConfirmEndCall, onContinueCall)
        }
    }
}
