package com.project.speakmate.ui.profile.savedvocab.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.PronunciationState
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun VocabularyCard(
    item: VocabularyItem,
    onPlayClick: (VocabularyItem) -> Unit,
    onDeleteClick: (VocabularyItem) -> Unit,
    modifier: Modifier = Modifier,
    playbackStatus: PronunciationState = PronunciationState.IDLE
) {
    val play = remember(item, onPlayClick) { { onPlayClick(item) } }
    val delete = remember(item, onDeleteClick) { { onDeleteClick(item) } }
    val loadingDescription = stringResource(R.string.accessibility_loading)
    Surface(modifier.fillMaxWidth(), color = DesignColor.Surface, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.word, style = DesignTypography.Heading, color = DesignColor.Text)
                    val detail = listOf(item.phonetic, item.partOfSpeech).filter(String::isNotBlank).joinToString(stringResource(R.string.saved_vocab_detail_separator))
                    if (detail.isNotBlank()) Text(detail, style = DesignTypography.Detail, color = DesignColor.Muted)
                }
                if (playbackStatus == PronunciationState.LOADING || playbackStatus == PronunciationState.PLAYING) {
                    TextButton(onClick = play, modifier = Modifier.semantics {
                        if (playbackStatus == PronunciationState.LOADING) stateDescription = loadingDescription
                    }) {
                        if (playbackStatus == PronunciationState.LOADING) SpeakMateLoadingIndicator(size = 20.dp, strokeWidth = 2.dp)
                        Text(stringResource(R.string.saved_vocab_stop), style = DesignTypography.Button)
                    }
                } else IconButton(onClick = play) {
                    Icon(painterResource(R.drawable.ic_training_preparation_speaker),
                        stringResource(if (playbackStatus == PronunciationState.FAILED) R.string.saved_vocab_retry_pronunciation else R.string.accessibility_play_pronunciation),
                        Modifier.size(24.dp), tint = DesignColor.Text)
                }
                IconButton(onClick = delete) {
                    Icon(Icons.Default.Delete, stringResource(R.string.accessibility_delete_vocabulary), tint = DesignColor.Text)
                }
            }
            if (playbackStatus == PronunciationState.FAILED) {
                Text(stringResource(R.string.call_pronunciation_unavailable), style = DesignTypography.Detail, color = DesignColor.Error)
            }
            Text(item.mean, style = DesignTypography.Body, color = DesignColor.Text)
            if (item.example.sentence.isNotBlank()) {
                Text(item.example.sentence, style = DesignTypography.Detail, color = DesignColor.Muted)
                if (item.example.translate.isNotBlank()) Text(item.example.translate, style = DesignTypography.Detail, color = DesignColor.Muted)
            }
        }
    }
}
