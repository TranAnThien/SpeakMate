package com.project.speakmate.ui.call

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.project.speakmate.R
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.ui.call.components.CallAudioVisualizer
import com.project.speakmate.ui.call.components.CallDurationLabel
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.call.state.MicrophonePermissionPresentation
import kotlinx.coroutines.flow.StateFlow

@Composable
fun CallRoute(
    scenario: Scenario,
    activityType: CallActivityType,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    communicativeGoal: String = "",
    onCallFinished: ((CallSessionState) -> Unit)? = null
) {
    val viewModel: CallViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val onExitRequested by rememberUpdatedState(onExit)
    val onFinished by rememberUpdatedState(onCallFinished)
    val lifecycleOwner = LocalLifecycleOwner.current
    val finishedSession = state.finishedSession
    val settingsUnavailable = stringResource(R.string.call_permission_settings_unavailable)
    val permission = Manifest.permission.RECORD_AUDIO
    var isAwaitingSettings by rememberSaveable { mutableStateOf(false) }
    var isPermissionRequestInProgress by remember { mutableStateOf(false) }
    var shouldOpenSettings by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.setForeground(true)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (activity?.isChangingConfigurations != true) viewModel.setForeground(false)
    }

    fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun startWithPermission() {
        shouldOpenSettings = false
        viewModel.startCall(scenario, activityType, communicativeGoal)
    }

    fun requestStart() {
        if (hasMicrophonePermission()) startWithPermission()
        else viewModel.showMicrophonePermission(
            if (shouldOpenSettings) MicrophonePermissionPresentation.DENIED else MicrophonePermissionPresentation.RATIONALE
        )
    }

    fun dismissPermission() {
        isAwaitingSettings = false
        viewModel.dismissMicrophonePermission()
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        isPermissionRequestInProgress = false
        if (viewModel.uiState.value.microphonePermission != null) {
            if (isGranted && hasMicrophonePermission()) {
                startWithPermission()
            } else {
                shouldOpenSettings = activity?.let {
                    !ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
                } == true
                requestStart()
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isAwaitingSettings) {
            isAwaitingSettings = false
            if (viewModel.uiState.value.microphonePermission != null && hasMicrophonePermission()) startWithPermission()
        }
    }
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
    BackHandler {
        when {
            state.microphonePermission != null -> dismissPermission()
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
            onRequestPermission = {
                if (!isPermissionRequestInProgress) {
                    if (hasMicrophonePermission()) {
                        startWithPermission()
                    } else {
                        isPermissionRequestInProgress = true
                        permissionLauncher.launch(permission)
                    }
                }
            },
            onOpenSettings = {
                isAwaitingSettings = true
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                try {
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    isAwaitingSettings = false
                    Toast.makeText(context, settingsUnavailable, Toast.LENGTH_LONG).show()
                }
            },
            onRecheckPermission = ::requestStart,
            onBack = ::dismissPermission,
            onExit = viewModel::exitCall,
            modifier = modifier
        )
    } else {
        val shouldShowDuration = state.presentation != CallPresentation.READY
        CallScreen(
            state = remember(state, scenario.name) { state.copy(title = scenario.name, finishedSession = null) },
            onStartCall = ::requestStart,
            onReturnToPreparation = viewModel::exitCall,
            onToggleMute = viewModel::toggleMute,
            onEndCall = viewModel::requestEndCall,
            onConfirmEndCall = viewModel::confirmEndCall,
            onContinueCall = viewModel::dismissEndConfirmation,
            onRetry = ::requestStart,
            onExit = viewModel::exitCall,
            onOpenTranscript = viewModel::openTranscript,
            onCloseTranscript = viewModel::closeTranscript,
            onSelectTranscriptWord = viewModel::selectWord,
            onCloseDictionary = viewModel::closeDictionary,
            onPlayDictionaryWord = viewModel::playDictionaryWord,
            onSaveDictionaryWord = {},
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
