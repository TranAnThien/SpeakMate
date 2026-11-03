package com.project.speakmate.ui.call.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun CallStatusHeader(title: String, durationLabel: String, presentation: CallPresentation, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = DesignTypography.SectionTitle, color = DesignColor.Primary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(when (presentation) {
                CallPresentation.READY -> DesignColor.Placeholder
                CallPresentation.CONNECTING, CallPresentation.PROCESSING -> DesignColor.CallBusy
                CallPresentation.MUTED -> DesignColor.CallMuted
                CallPresentation.AUDIO_UNCLEAR -> DesignColor.CallWarning
                else -> DesignColor.CallActive
            }, CircleShape))
            Text(stringResource(when (presentation) {
                CallPresentation.READY -> R.string.call_ready
                CallPresentation.CONNECTING -> R.string.call_connecting
                CallPresentation.PROCESSING -> R.string.call_processing
                CallPresentation.SPEAKING -> R.string.call_speaking
                CallPresentation.MUTED -> R.string.call_muted
                CallPresentation.BARGE_IN -> R.string.call_barge_in
                CallPresentation.AUDIO_UNCLEAR -> R.string.call_audio_unclear
                else -> R.string.call_listening
            }), style = DesignTypography.CallStatus, color = DesignColor.Muted)
        }
        Spacer(Modifier.height(16.dp))
        if (presentation == CallPresentation.CONNECTING) {
            Row(Modifier.background(DesignColor.SecondaryButton, RoundedCornerShape(100.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_placement_schedule), null, Modifier.size(20.dp))
                Text(stringResource(R.string.call_timer_after_connection), style = DesignTypography.Navigation, color = DesignColor.Primary)
            }
        } else Text(durationLabel, style = DesignTypography.CallTimer, color = DesignColor.Primary)
    }
}
