package com.project.speakmate.ui.training.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.PronunciationPlaybackState
import com.project.speakmate.domain.model.PronunciationState
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.state.PreparationPatternUiState

@Composable
fun PreparationPatternCard(
    pattern: PreparationPatternUiState,
    onPlayPattern: () -> Unit,
    onPlayExample: (String) -> Unit,
    modifier: Modifier = Modifier,
    playback: PronunciationPlaybackState = PronunciationPlaybackState()
) {
    SpeakMateCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 16.dp,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CaptionText(
                text = stringResource(R.string.preparation_patterns),
                style = DesignTypography.Label,
                modifier = Modifier.weight(1f)
            )
            PreparationAudioButton(onPlay = onPlayPattern, text = pattern.pattern, playback = playback)
        }
        if (playback.text == pattern.pattern && playback.status == PronunciationState.FAILED)
            CaptionText(text = stringResource(R.string.preparation_audio_retry), color = DesignColor.Error)
        TitleText(text = pattern.pattern)
        DetailText(text = stringResource(R.string.preparation_pattern_meaning, pattern.meaning))
        CaptionText(text = stringResource(R.string.preparation_usage), style = DesignTypography.Label)
        DetailText(text = pattern.usage, color = DesignColor.Text)
        val examples = pattern.examples
        examples.forEach { example ->
            val onPlay = remember(example.sentence, onPlayExample) { { onPlayExample(example.sentence) } }
            PreparationExampleCard(example = example, onPlayExample = onPlay, playback = playback)
        }
    }
}
