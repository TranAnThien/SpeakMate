package com.project.speakmate.ui.call.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun CallControls(
    isMuted: Boolean,
    isConnecting: Boolean,
    onToggleMute: () -> Unit,
    onEndCall: () -> Unit,
    onOpenTranscript: () -> Unit,
    modifier: Modifier = Modifier,
    isTranscriptAvailable: Boolean = true
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 9.dp,
        bottom = if (isConnecting) 45.dp else if (isTranscriptAvailable) 37.dp else 42.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isTranscriptAvailable) 12.dp else 24.dp, Alignment.CenterHorizontally)) {
        if (!isConnecting) CallControlItem(
            stringResource(if (isMuted) R.string.call_unmute else R.string.call_mute),
            if (isMuted) R.drawable.ic_placement_microphone else R.drawable.ic_placement_microphone_off,
            if (isMuted) DesignColor.IconBlue else DesignColor.Surface, DesignColor.Text, 56.dp, 24.dp, onToggleMute,
            if (isTranscriptAvailable) Modifier.weight(1f) else Modifier.size(width = 108.dp, height = 110.dp),
            isToggled = isMuted,
            toggleDescription = stringResource(R.string.call_mute)
        )
        CallControlItem(stringResource(if (isConnecting) R.string.call_cancel_connection else R.string.call_end),
            R.drawable.ic_call_end, DesignColor.CallDanger, DesignColor.Surface, 72.dp, 36.dp, onEndCall,
            if (isConnecting || !isTranscriptAvailable) Modifier.size(width = 108.dp, height = 110.dp) else Modifier.weight(1f))
        if (!isConnecting && isTranscriptAvailable) CallControlItem(stringResource(R.string.call_transcript), R.drawable.ic_call_transcript,
            DesignColor.Surface, DesignColor.Text, 56.dp, 24.dp, onOpenTranscript, Modifier.weight(1f))
    }
}

@Composable
private fun CallControlItem(
    label: String,
    @DrawableRes iconRes: Int,
    background: Color,
    iconColor: Color,
    buttonSize: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isToggled: Boolean? = null,
    toggleDescription: String? = null
) {
    val actionModifier = if (isToggled == null) modifier.clickable(onClick = onClick)
    else modifier.toggleable(value = isToggled, role = Role.Switch, onValueChange = { onClick() })
    val labeledModifier = if (toggleDescription == null) actionModifier
    else actionModifier.semantics { contentDescription = toggleDescription }
    Column(labeledModifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(buttonSize).background(background, CircleShape), contentAlignment = Alignment.Center) {
                Image(painterResource(iconRes), null, Modifier.size(iconSize), colorFilter = ColorFilter.tint(iconColor))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            modifier = if (toggleDescription == null) Modifier else Modifier.clearAndSetSemantics {},
            style = DesignTypography.Body,
            color = DesignColor.Muted,
            textAlign = TextAlign.Center
        )
    }
}
