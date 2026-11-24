package com.project.speakmate.ui.call.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieConstants
import com.project.speakmate.R
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.components.lottie.AnimatedMascot
import com.project.speakmate.ui.components.lottie.MascotAnimation
import com.project.speakmate.utils.AppConstant

@Composable
fun CallAvatar(
    presentation: CallPresentation,
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
    isConnectionFailure: Boolean = false
) {
    val marker =
        when {
            isMuted && (presentation == CallPresentation.LISTENING || presentation == CallPresentation.USER_SPEAKING) ->
                AppConstant.CALL_MASCOT_MUTED_MARKER
            presentation == CallPresentation.SPEAKING -> AppConstant.CALL_MASCOT_SPEAKING_MARKER
            presentation == CallPresentation.LISTENING ||
                presentation == CallPresentation.USER_SPEAKING ||
                presentation == CallPresentation.BARGE_IN -> AppConstant.CALL_MASCOT_LISTENING_MARKER
            presentation == CallPresentation.PROCESSING || presentation == CallPresentation.FINALIZING ->
                AppConstant.CALL_MASCOT_THINKING_MARKER
            presentation == CallPresentation.FAILED || presentation == CallPresentation.MICROPHONE_BLOCKED ->
                AppConstant.CALL_MASCOT_ERROR_MARKER
            else -> AppConstant.CALL_MASCOT_IDLE_MARKER
        }
    val isConnecting = presentation == CallPresentation.CONNECTING || presentation == CallPresentation.RECONNECTING
    val isNetworkError = presentation == CallPresentation.FAILED && isConnectionFailure
    val animation =
        when {
            isNetworkError -> MascotAnimation.NETWORK_ERROR
            isConnecting -> MascotAnimation.CALL_CONNECTING
            else -> MascotAnimation.CALL_MASCOT
        }
    val fallbackRes =
        when (marker) {
            AppConstant.CALL_MASCOT_LISTENING_MARKER -> R.drawable.img_call_listening
            AppConstant.CALL_MASCOT_THINKING_MARKER -> R.drawable.img_call_processing
            AppConstant.CALL_MASCOT_SPEAKING_MARKER -> R.drawable.img_call_speaking
            AppConstant.CALL_MASCOT_MUTED_MARKER -> R.drawable.img_call_muted
            AppConstant.CALL_MASCOT_ERROR_MARKER -> R.drawable.img_call_error
            else -> R.drawable.img_placement_mascot
        }
    AnimatedMascot(
        animation = animation,
        modifier = modifier,
        fallbackRes = fallbackRes,
        iterations = if (isNetworkError) 1 else LottieConstants.IterateForever,
        marker = if (isConnecting || isNetworkError) null else marker
    )
}
