package com.project.speakmate.ui.training.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.project.speakmate.ui.training.state.PreparationVocabularyUiState

@Composable
fun PreparationVocabularyCard(
    word: PreparationVocabularyUiState,
    onPlayWord: () -> Unit,
    onPlayExample: () -> Unit,
    modifier: Modifier = Modifier,
    playback: PronunciationPlaybackState = PronunciationPlaybackState()
) {
    SpeakMateCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 16.dp,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TitleText(text = word.word)
                DetailText(text = word.pronunciationLabel)
            }
            PreparationAudioButton(onPlay = onPlayWord, text = word.word, playback = playback)
        }
        if (playback.text == word.word && playback.status == PronunciationState.FAILED)
            CaptionText(text = stringResource(R.string.call_pronunciation_unavailable), color = DesignColor.Error)
        DetailText(text = stringResource(R.string.preparation_word_meaning, word.meaning), color = DesignColor.Text)
        PreparationExampleCard(example = word.example, onPlayExample = onPlayExample, playback = playback)
    }
}
