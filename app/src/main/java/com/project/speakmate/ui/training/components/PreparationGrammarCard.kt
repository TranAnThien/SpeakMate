package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.PronunciationPlaybackState
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.state.PreparationGrammarUiState

@Composable
fun PreparationGrammarCard(
    grammar: PreparationGrammarUiState,
    onPlayExample: (String) -> Unit,
    modifier: Modifier = Modifier,
    playback: PronunciationPlaybackState = PronunciationPlaybackState()
) {
    SpeakMateCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 16.dp,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TitleText(text = grammar.title)
        DetailText(text = grammar.explanation)
        Column(
            Modifier
                .fillMaxWidth()
                .background(DesignColor.ExampleSurface, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CaptionText(text = stringResource(R.string.preparation_structure), style = DesignTypography.Label)
            DetailText(text = grammar.structure, color = DesignColor.Text)
        }
        val examples = grammar.examples
        examples.forEach { example ->
            val onPlay = remember(example.sentence, onPlayExample) { { onPlayExample(example.sentence) } }
            PreparationExampleCard(example = example, onPlayExample = onPlay, playback = playback)
        }
        PreparationNote(grammar.usageNote)
    }
}
