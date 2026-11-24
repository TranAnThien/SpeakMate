package com.project.speakmate.app

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.project.speakmate.R
import com.project.speakmate.ui.call.components.CallExitNoticeCard
import kotlinx.coroutines.launch

class AppNavigationNotice(
    val hostState: SnackbarHostState,
    val entryId: String?,
    val showCancelledCall: () -> Unit
)

@Composable
fun rememberAppCallNotice(
    navController: NavHostController,
    sessionUserId: String,
    entry: NavBackStackEntry?
): AppNavigationNotice {
    val cancellationNoticeHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var cancellationNoticeEntryId by remember { mutableStateOf<String?>(null) }
    val cancellationMessage = stringResource(R.string.call_cancelled_not_saved)
    val showCancelledCallNotice =
        remember(navController, cancellationNoticeHost, scope, cancellationMessage) {
            {
                cancellationNoticeEntryId = navController.currentBackStackEntry?.id
                scope.launch {
                    cancellationNoticeHost.currentSnackbarData?.dismiss()
                    cancellationNoticeHost.showSnackbar(cancellationMessage, duration = SnackbarDuration.Short)
                }
                Unit
            }
        }
    LaunchedEffect(entry, sessionUserId) {
        if (entry?.id != cancellationNoticeEntryId) {
            cancellationNoticeHost.currentSnackbarData?.dismiss()
            cancellationNoticeEntryId = null
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { cancellationNoticeHost.currentSnackbarData?.dismiss() }
    return AppNavigationNotice(
        hostState = cancellationNoticeHost,
        entryId = cancellationNoticeEntryId,
        showCancelledCall = showCancelledCallNotice
    )
}

@Composable
fun AppNavigationNoticeContent(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        CallExitNoticeCard(data.visuals.message)
    }
}
