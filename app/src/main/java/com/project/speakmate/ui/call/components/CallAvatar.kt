package com.project.speakmate.ui.call.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun CallAvatar(presentation: CallPresentation, modifier: Modifier = Modifier) {
    Box(modifier.size(290.dp), contentAlignment = Alignment.Center) {
        if (presentation == CallPresentation.CONNECTING) {
            Box(Modifier.size(260.dp).border(4.dp, DesignColor.IconBlue, CircleShape))
        } else if (presentation == CallPresentation.LISTENING || presentation == CallPresentation.SPEAKING || presentation == CallPresentation.BARGE_IN) {
            Box(Modifier.size(290.dp).border(1.dp, DesignColor.CallOuterRing, CircleShape))
            Box(Modifier.size(240.dp).border(2.dp, DesignColor.CallInnerRing, CircleShape))
        }
        Image(painterResource(when (presentation) {
            CallPresentation.LISTENING, CallPresentation.BARGE_IN -> R.drawable.img_call_listening
            CallPresentation.PROCESSING -> R.drawable.img_call_processing
            CallPresentation.SPEAKING -> R.drawable.img_call_speaking
            CallPresentation.MUTED -> R.drawable.img_call_muted
            CallPresentation.AUDIO_UNCLEAR, CallPresentation.FAILED -> R.drawable.img_call_error
            else -> R.drawable.img_placement_mascot
        }), null, Modifier.size(220.dp))
    }
}
