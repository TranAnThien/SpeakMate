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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.project.speakmate.utils.extension.default
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun PreparationAudioButton(
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    playback: com.project.speakmate.domain.model.PronunciationPlaybackState = com.project.speakmate.domain.model.PronunciationPlaybackState()
) {
    val status = playback.status.takeIf { playback.text == text }.default(com.project.speakmate.domain.model.PronunciationState.IDLE)
    val label = stringResource(when (status) {
        com.project.speakmate.domain.model.PronunciationState.LOADING -> R.string.call_pronunciation_loading
        com.project.speakmate.domain.model.PronunciationState.PLAYING -> R.string.call_pronunciation_stop
        com.project.speakmate.domain.model.PronunciationState.FAILED -> R.string.preparation_audio_retry
        else -> R.string.accessibility_play_audio
    })
    Box(modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(DesignColor.PaleBlue)
        .semantics { contentDescription = label }.clickable(enabled = text.isNotBlank(), role = Role.Button, onClick = onPlay), contentAlignment = Alignment.Center) {
        if (status == com.project.speakmate.domain.model.PronunciationState.LOADING) com.project.speakmate.ui.components.SpeakMateLoadingIndicator(size = 22.dp)
        else Image(painterResource(R.drawable.ic_training_preparation_speaker), null, Modifier.size(22.dp))
    }
}
