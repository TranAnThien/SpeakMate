package com.project.speakmate.ui.call

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.project.speakmate.R
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.ui.call.state.MicrophonePermissionPresentation

class CallPermissionController(
    val onRequestStart: () -> Unit,
    val onDismissPermission: () -> Unit,
    val onRequestPermission: () -> Unit,
    val onOpenSettings: () -> Unit
)

@Composable
fun rememberCallPermissionController(
    viewModel: CallViewModel,
    scenario: Scenario,
    activityType: CallActivityType,
    communicativeGoal: String,
    activityContext: CallActivityContext?
): CallPermissionController {
    val context = LocalContext.current
    val activity = LocalActivity.current
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
        val callContext = activityContext ?: when (activityType) {
            CallActivityType.FREE_PRACTICE -> CallActivityContext.FreePractice(scenario)
            else -> CallActivityContext.Assessment(scenario, activityType, communicativeGoal)
        }
        viewModel.startCall(callContext)
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
    return CallPermissionController(
        onRequestStart = ::requestStart,
        onDismissPermission = ::dismissPermission,
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
        }
    )
}
