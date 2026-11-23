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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.PlacementRole
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.utils.extension.default

@Composable
fun CallStatusHeader(
    title: String,
    presentation: CallPresentation,
    modifier: Modifier = Modifier,
    durationContent: @Composable () -> Unit,
    isMuted: Boolean = false,
    activityType: CallActivityType = CallActivityType.FREE_PRACTICE,
    activityIndex: Int? = null,
    onShowTitle: () -> Unit = {},
    isCompact: Boolean = false,
    activityLevel: SpeakingLevel? = null,
    activityRole: PlacementRole? = null
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = if (isCompact) 4.dp else 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = DesignTypography.SectionTitle, color = DesignColor.Primary, textAlign = TextAlign.Center,
                maxLines = if (isCompact) 1 else 2, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onShowTitle) { Icon(painterResource(R.drawable.ic_placement_info), stringResource(R.string.call_show_full_title)) }
        }
        if (!isCompact) Text(stringResource(when (activityType) {
            CallActivityType.FREE_PRACTICE -> R.string.history_type_free_practice
            CallActivityType.TRAINING_UNIT -> if (activityIndex != null) R.string.call_unit_subtitle else R.string.history_type_unit
            CallActivityType.GOAL_ASSESSMENT -> R.string.goal_assessment_header
            CallActivityType.INITIAL_PLACEMENT -> if (activityIndex != null) R.string.call_placement_subtitle else R.string.placement_title
            CallActivityType.REASSESSMENT -> if (activityIndex != null) R.string.call_reassessment_subtitle else R.string.promotion_header
        }, activityIndex.default(1)), style = DesignTypography.Detail, color = DesignColor.Muted, textAlign = TextAlign.Center)
        if (!isCompact && activityLevel != null) Text(stringResource(R.string.call_target_level, activityLevel.name), style = DesignTypography.Detail, color = DesignColor.Muted)
        if (!isCompact && activityRole != null) Text(stringResource(when (activityRole) {
            PlacementRole.INITIAL -> R.string.assessment_role_initial
            PlacementRole.PROBE -> R.string.assessment_role_probe
            PlacementRole.CONFIRMATION -> R.string.assessment_role_confirmation
        }), style = DesignTypography.Detail, color = DesignColor.Muted)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(when (presentation) {
                CallPresentation.READY -> DesignColor.CallBusy
                CallPresentation.CONNECTING, CallPresentation.PROCESSING, CallPresentation.FINALIZING, CallPresentation.RECONNECTING -> DesignColor.CallBusy
                CallPresentation.PAUSED -> DesignColor.CallMuted
                CallPresentation.AUDIO_UNCLEAR, CallPresentation.MICROPHONE_BLOCKED -> DesignColor.CallWarning
                else -> DesignColor.CallActive
            }, CircleShape))
            Text(stringResource(when (presentation) {
                CallPresentation.READY -> R.string.call_ready
                CallPresentation.CONNECTING -> R.string.call_connecting
                CallPresentation.USER_SPEAKING -> R.string.call_user_speaking
                CallPresentation.FINALIZING -> R.string.call_finalizing
                CallPresentation.PROCESSING -> R.string.call_processing
                CallPresentation.SPEAKING -> R.string.call_speaking
                CallPresentation.PAUSED -> R.string.call_paused
                CallPresentation.BARGE_IN -> R.string.call_barge_in
                CallPresentation.AUDIO_UNCLEAR -> R.string.call_audio_unclear
                CallPresentation.RECONNECTING -> R.string.call_reconnecting
                CallPresentation.MICROPHONE_BLOCKED -> R.string.call_microphone_blocked
                else -> R.string.call_listening
            }), style = DesignTypography.CallStatus, color = DesignColor.Muted)
        }
        if (isMuted) Text(stringResource(R.string.call_muted), style = DesignTypography.Detail, color = DesignColor.Muted)
        Spacer(Modifier.height(8.dp))
        if (presentation == CallPresentation.CONNECTING) {
            Row(Modifier.background(DesignColor.SecondaryButton, RoundedCornerShape(100.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_placement_schedule), null, Modifier.size(20.dp))
                Text(stringResource(R.string.call_timer_after_connection), style = DesignTypography.Navigation, color = DesignColor.Primary)
            }
        } else {
            durationContent()
        }
    }
}

@Composable
fun CallDurationLabel(label: String, modifier: Modifier = Modifier) {
    Text(label, modifier, style = DesignTypography.CallTimer, color = DesignColor.Primary)
}
