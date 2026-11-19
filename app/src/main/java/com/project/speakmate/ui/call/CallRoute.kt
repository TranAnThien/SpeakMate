package com.project.speakmate.ui.call

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.ui.call.components.CallAudioVisualizer
import com.project.speakmate.ui.call.components.CallDurationLabel
import com.project.speakmate.ui.call.state.CallPresentation
import kotlinx.coroutines.flow.StateFlow

@Composable
fun CallRoute(
    scenario: Scenario,
    activityType: CallActivityType,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    communicativeGoal: String = "",
    onCallFinished: ((CallSessionState) -> Unit)? = null,
    activityContext: CallActivityContext? = null,
    onPracticeCompleted: ((String) -> Unit)? = null
) {
    val viewModel: CallViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val onExitRequested by rememberUpdatedState(onExit)
    val onFinished by rememberUpdatedState(onCallFinished)
    val onCompleted by rememberUpdatedState(onPracticeCompleted)
    val lifecycleOwner = LocalLifecycleOwner.current
    val finishedSession = state.finishedSession
    val permissionController = rememberCallPermissionController(viewModel, scenario, activityType, communicativeGoal, activityContext)
    if (state.isExitReady) {
        LaunchedEffect(Unit) { onExitRequested() }
    }
    if (onCallFinished != null && finishedSession != null && !state.isExiting) {
        LaunchedEffect(finishedSession, lifecycleOwner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                onFinished?.invoke(finishedSession)
            }
        }
    }
    val completionAttemptId = state.completionAttemptId
    if (onPracticeCompleted != null && completionAttemptId != null && !state.isExiting) {
        LaunchedEffect(completionAttemptId, lifecycleOwner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.handoffCompletion()
                onCompleted?.invoke(completionAttemptId)
            }
        }
    }
    BackHandler {
        when {
            state.microphonePermission != null -> permissionController.onDismissPermission()
            state.isEndConfirmationVisible -> viewModel.dismissEndConfirmation()
            state.dictionary != null -> viewModel.closeDictionary()
            state.isTranscriptVisible -> viewModel.closeTranscript()
            state.presentation == CallPresentation.READY || state.presentation == CallPresentation.FAILED -> viewModel.exitCall()
            else -> viewModel.requestEndCall()
        }
    }

    val microphonePermission = state.microphonePermission
    if (microphonePermission != null) {
        MicrophonePermissionScreen(
            state = microphonePermission,
            onRequestPermission = permissionController.onRequestPermission,
            onOpenSettings = permissionController.onOpenSettings,
            onRecheckPermission = permissionController.onRequestStart,
            onBack = permissionController.onDismissPermission,
            onExit = viewModel::exitCall,
            modifier = modifier
        )
    } else {
        val shouldShowDuration = state.presentation != CallPresentation.READY
        CallScreen(
            state = remember(state, scenario.name) { state.copy(title = scenario.name, finishedSession = null) },
            onStartCall = permissionController.onRequestStart,
            onReturnToPreparation = viewModel::exitCall,
            onToggleMute = viewModel::toggleMute,
            onEndCall = viewModel::requestEndCall,
            onConfirmEndCall = viewModel::confirmEndCall,
            onContinueCall = viewModel::dismissEndConfirmation,
            onRetry = permissionController.onRequestStart,
            onExit = viewModel::exitCall,
            onOpenTranscript = viewModel::openTranscript,
            onCloseTranscript = viewModel::closeTranscript,
            onSelectTranscriptWord = viewModel::selectWord,
            onCloseDictionary = viewModel::closeDictionary,
            onPlayDictionaryWord = viewModel::playDictionaryWord,
            onSaveDictionaryWord = viewModel::saveDictionaryWord,
            modifier = modifier,
            onResumeCall = viewModel::resumeCall,
            onRetryDictionary = viewModel::retryLookup,
            durationContent = {
                if (shouldShowDuration) {
                    CallLiveDuration(viewModel.durationLabel)
                }
            },
            audioMeter = { meterModifier -> CallLiveAudioMeter(viewModel.audioLevel, meterModifier) }
        )
    }
}

@Composable
private fun CallLiveDuration(duration: StateFlow<String>) {
    val label by duration.collectAsStateWithLifecycle()
    CallDurationLabel(label)
}

@Composable
private fun CallLiveAudioMeter(level: StateFlow<Float>, modifier: Modifier) {
    val audioLevel = level.collectAsStateWithLifecycle()
    val readLevel = remember(audioLevel) { { audioLevel.value } }
    CallAudioVisualizer(readLevel, modifier)
}
