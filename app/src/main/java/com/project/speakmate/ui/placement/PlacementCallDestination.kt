package com.project.speakmate.ui.placement

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun <T> PlacementCallDestination(
    shouldShowCall: Boolean,
    isCallDestination: Boolean,
    pendingNavigation: T?,
    onChangeCallDestination: (Boolean) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val changeCallDestination by rememberUpdatedState(onChangeCallDestination)
    LaunchedEffect(shouldShowCall, isCallDestination, pendingNavigation, lifecycleOwner) {
        if (pendingNavigation == null && shouldShowCall != isCallDestination) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                changeCallDestination(shouldShowCall)
            }
        }
    }
}
