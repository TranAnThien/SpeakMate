package com.project.speakmate.ui.components

import android.animation.ValueAnimator
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
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
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun AnimatedMascot(@RawRes animationRes: Int, @DrawableRes fallbackRes: Int, modifier: Modifier = Modifier, iterations: Int = 1, marker: String? = null) {
    val result = rememberLottieComposition(LottieCompositionSpec.RawRes(animationRes))
    val composition by result
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            !ValueAnimator.areAnimatorsEnabled() || result.isFailure -> Image(painterResource(fallbackRes), null, Modifier.matchParentSize())
            composition == null -> SpeakMateLoadingIndicator()
            else -> LottieAnimation(composition, modifier = Modifier.matchParentSize(), iterations = iterations,
                clipSpec = marker?.let { LottieClipSpec.Marker(it) },
                isPlaying = lifecycleState.isAtLeast(Lifecycle.State.STARTED), restartOnPlay = false, safeMode = true)
        }
    }
}
