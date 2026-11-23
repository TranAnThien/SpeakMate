package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.state.PreparationPatternUiState

@Composable
fun PreparationPatternCard(
    pattern: PreparationPatternUiState,
    onPlayPattern: () -> Unit,
    onPlayExample: (String) -> Unit,
    modifier: Modifier = Modifier,
    playback: com.project.speakmate.domain.model.PronunciationPlaybackState = com.project.speakmate.domain.model.PronunciationPlaybackState()
) {
    Column(modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.heightIn(min = 48.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(pattern.label.ifBlank { stringResource(R.string.preparation_patterns) }, style = DesignTypography.Label, color = DesignColor.Muted, modifier = Modifier.weight(1f))
            PreparationAudioButton(onPlayPattern, text = pattern.pattern, playback = playback)
        }
        if (playback.text == pattern.pattern && playback.status == com.project.speakmate.domain.model.PronunciationState.FAILED) Text(
            stringResource(R.string.preparation_audio_retry),
            style = DesignTypography.Caption,
            color = DesignColor.Error
        )
        Text(pattern.pattern, style = DesignTypography.Title, color = DesignColor.Text)
        Text(stringResource(R.string.preparation_pattern_meaning, pattern.meaning), style = DesignTypography.Detail, color = DesignColor.Muted)
        Text(stringResource(R.string.preparation_usage), style = DesignTypography.Label, color = DesignColor.Muted)
        Text(pattern.usage, style = DesignTypography.Detail, color = DesignColor.Text)
        val examples = pattern.examples.ifEmpty { listOf(pattern.example).filter { it.sentence.isNotBlank() } }
        examples.forEach { example ->
            val onPlay = remember(example.sentence, onPlayExample) { { onPlayExample(example.sentence) } }
            PreparationExampleCard(example, onPlay, playback = playback)
        }
    }
}
