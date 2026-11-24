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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.PronunciationPlaybackState
import com.project.speakmate.domain.model.PronunciationState
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.training.state.PreparationExampleUiState

@Composable
fun PreparationExampleCard(
    example: PreparationExampleUiState,
    onPlayExample: () -> Unit,
    modifier: Modifier = Modifier,
    playback: PronunciationPlaybackState = PronunciationPlaybackState()
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(DesignColor.ExampleSurface, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailText(text = example.sentence, color = DesignColor.Text)
        if (playback.text == example.sentence && playback.status == PronunciationState.FAILED)
            CaptionText(text = stringResource(R.string.call_pronunciation_unavailable), color = DesignColor.Error)
        DetailText(text = example.translation)
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DetailText(
                text = stringResource(R.string.preparation_listen_example),
                color = DesignColor.Primary,
                modifier = Modifier.weight(1f).padding(vertical = 14.dp)
            )
            PreparationAudioButton(onPlay = onPlayExample, text = example.sentence, playback = playback)
        }
    }
}
