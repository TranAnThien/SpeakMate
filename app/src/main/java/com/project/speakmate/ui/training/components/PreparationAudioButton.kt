package com.project.speakmate.ui.training.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun PreparationAudioButton(onPlay: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.accessibility_play_audio)
    Box(modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(DesignColor.PaleBlue)
        .clickable(onClickLabel = label, role = Role.Button, onClick = onPlay), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ic_training_preparation_speaker), null, Modifier.size(22.dp))
    }
}
