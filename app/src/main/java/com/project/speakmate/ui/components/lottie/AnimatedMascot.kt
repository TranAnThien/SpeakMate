package com.project.speakmate.ui.components.lottie

import android.animation.ValueAnimator
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.project.speakmate.ui.components.state.SpeakMateLoadingIndicator

@Composable
fun AnimatedMascot(
    animation: MascotAnimation,
    modifier: Modifier = Modifier,
    @DrawableRes fallbackRes: Int = animation.fallbackRes,
    iterations: Int = 1,
    marker: String? = null
) {
    val result = rememberLottieComposition(LottieCompositionSpec.RawRes(animation.animationRes))
    val composition by result
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            !ValueAnimator.areAnimatorsEnabled() || result.isFailure ->
                Image(
                    painter = painterResource(fallbackRes),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize()
                )
            composition == null -> SpeakMateLoadingIndicator()
            else ->
                LottieAnimation(
                    composition = composition,
                    modifier = Modifier.matchParentSize(),
                    iterations = iterations,
                    clipSpec = marker?.let { LottieClipSpec.Marker(it) },
                    isPlaying = lifecycleState.isAtLeast(Lifecycle.State.STARTED),
                    restartOnPlay = false,
                    safeMode = true
                )
        }
    }
}
