package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.state.PreparationVocabularyUiState

@Composable
fun PreparationVocabularyCard(
    word: PreparationVocabularyUiState,
    onPlayWord: () -> Unit,
    onPlayExample: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(word.word, style = DesignTypography.Title, color = DesignColor.Text)
                Text(word.pronunciationLabel, style = DesignTypography.Detail, color = DesignColor.Muted)
            }
            PreparationAudioButton(onPlayWord)
        }
        Text(stringResource(R.string.preparation_word_meaning, word.meaning), style = DesignTypography.Detail, color = DesignColor.Text)
        PreparationExampleCard(word.example, onPlayExample)
    }
}
