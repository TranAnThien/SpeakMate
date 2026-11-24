package com.project.speakmate.ui.profile.savedvocab.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.state.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.text.BodyText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.HeadingText
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
    SpeakMateCard(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                HeadingText(text = item.word)
                val detail =
                    listOf(item.phonetic, item.partOfSpeech)
                        .filter(String::isNotBlank)
                        .joinToString(stringResource(R.string.saved_vocab_detail_separator))
                if (detail.isNotBlank()) DetailText(text = detail)
            }
            if (playbackStatus == PronunciationState.LOADING || playbackStatus == PronunciationState.PLAYING) {
                TextButton(
                    onClick = play,
                    modifier =
                        Modifier.semantics {
                            if (playbackStatus == PronunciationState.LOADING) stateDescription = loadingDescription
                        }
                ) {
                    if (playbackStatus == PronunciationState.LOADING)
                        SpeakMateLoadingIndicator(size = 20.dp, strokeWidth = 2.dp)
                    DetailText(
                        text = stringResource(R.string.saved_vocab_stop),
                        style = DesignTypography.Button
                    )
                }
            } else
                IconButton(onClick = play) {
                    Icon(
                        painter = painterResource(R.drawable.ic_training_preparation_speaker),
                        contentDescription =
                            stringResource(
                                if (playbackStatus == PronunciationState.FAILED)
                                    R.string.saved_vocab_retry_pronunciation
                                else R.string.accessibility_play_pronunciation
                            ),
                        modifier = Modifier.size(24.dp),
                        tint = DesignColor.Text
                    )
                }
            IconButton(onClick = delete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.accessibility_delete_vocabulary),
                    tint = DesignColor.Text
                )
            }
        }
        if (playbackStatus == PronunciationState.FAILED) {
            DetailText(text = stringResource(R.string.call_pronunciation_unavailable), color = DesignColor.Error)
        }
        BodyText(text = item.mean)
        if (item.example.sentence.isNotBlank()) {
            DetailText(text = item.example.sentence)
            if (item.example.translate.isNotBlank()) DetailText(text = item.example.translate)
        }
    }
}
