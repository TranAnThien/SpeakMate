package com.project.speakmate.ui.call.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.call.state.CallDictionaryStatus
import com.project.speakmate.ui.call.state.CallDictionaryUiState
import com.project.speakmate.ui.call.state.CallVocabularySaveStatus
import com.project.speakmate.ui.components.button.SecondaryButton
import com.project.speakmate.ui.components.state.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun CallDictionaryPopup(
    state: CallDictionaryUiState,
    onClose: () -> Unit,
    onPlayPronunciation: () -> Unit,
    onSaveWord: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onStopPronunciation: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DesignColor.Surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DesignColor.DictionaryBorder)
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CallDictionaryHeader(state = state, onClose = onClose, onPlayPronunciation = onPlayPronunciation)
            if (state.isPronouncing && !state.isPronunciationLoading)
                SecondaryButton(stringResource(R.string.call_pronunciation_stop), onStopPronunciation)
            if (state.isPronouncing)
                CaptionText(
                    text =
                        stringResource(
                            if (state.isPronunciationLoading) R.string.call_pronunciation_loading
                            else R.string.call_pronunciation_playing
                        )
                )
            if (state.hasPronunciationError) {
                CaptionText(text = stringResource(R.string.call_pronunciation_unavailable))
            }
            when (state.status) {
                CallDictionaryStatus.LOADING -> {
                    SpeakMateLoadingIndicator(size = 24.dp)
                    CallDictionaryMessage(stringResource(R.string.call_dictionary_loading))
                }
                CallDictionaryStatus.NOT_FOUND ->
                    CallDictionaryMessage(stringResource(R.string.call_dictionary_not_found))
                CallDictionaryStatus.ERROR -> {
                    CallDictionaryMessage(stringResource(R.string.call_dictionary_error))
                    SecondaryButton(stringResource(R.string.call_dictionary_retry), onRetry)
                }
                CallDictionaryStatus.SUCCESS -> CallDictionaryContent(state, onSaveWord)
            }
        }
    }
}

@Composable
private fun CallDictionaryHeader(state: CallDictionaryUiState, onClose: () -> Unit, onPlayPronunciation: () -> Unit) {
    val phoneticLine =
        listOf(state.phonetic, state.partOfSpeech)
            .filter { it.isNotBlank() }
            .joinToString(stringResource(R.string.call_dictionary_detail_separator))
    val playLabel = stringResource(R.string.call_dictionary_play_pronunciation)
    val closeLabel = stringResource(R.string.call_dictionary_close)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TitleText(text = state.word)
            if (state.status == CallDictionaryStatus.SUCCESS && phoneticLine.isNotEmpty())
                DetailText(text = phoneticLine)
        }
        Box(
            Modifier
                .size(48.dp)
                .background(DesignColor.PrimaryContainer, RoundedCornerShape(12.dp))
                .clickable(
                    enabled = state.status == CallDictionaryStatus.SUCCESS && !state.isPronouncing,
                    role = Role.Button,
                    onClick = onPlayPronunciation
                )
                .semantics { contentDescription = playLabel },
            contentAlignment = Alignment.Center
        ) {
            if (state.isPronunciationLoading) SpeakMateLoadingIndicator(size = 22.dp)
            else
                Image(
                    painter = painterResource(R.drawable.ic_training_preparation_speaker),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
        }
        Box(
            Modifier
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onClose)
                .semantics {
                contentDescription = closeLabel
            },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_call_close),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(DesignColor.Muted)
            )
        }
    }
}

@Composable
private fun CallDictionaryContent(state: CallDictionaryUiState, onSaveWord: () -> Unit) {
    DetailText(text = stringResource(R.string.call_dictionary_meaning, state.meaning), color = DesignColor.Text)
    if (state.exampleSentence.isNotBlank()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(DesignColor.ExampleSurface, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DetailText(text = state.exampleSentence, color = DesignColor.Text)
            if (state.exampleTranslation.isNotBlank()) {
                DetailText(text = state.exampleTranslation)
            }
        }
    }
    if (!state.canSave) {
        CallDictionaryMessage(stringResource(R.string.call_dictionary_insufficient))
        return
    }
    when (state.saveStatus) {
        CallVocabularySaveStatus.IDLE -> SecondaryButton(stringResource(R.string.call_dictionary_save), onSaveWord)
        CallVocabularySaveStatus.SAVING -> CallDictionaryMessage(stringResource(R.string.call_dictionary_saving))
        CallVocabularySaveStatus.SAVED -> CallDictionaryMessage(stringResource(R.string.call_dictionary_saved))
        CallVocabularySaveStatus.MOVED_TO_TOP ->
            CallDictionaryMessage(stringResource(R.string.call_dictionary_moved_to_top))
        CallVocabularySaveStatus.SAVED_REPLACING_OLDEST ->
            CallDictionaryMessage(stringResource(R.string.call_dictionary_oldest_removed))
        CallVocabularySaveStatus.FAILED -> {
            CallDictionaryMessage(stringResource(R.string.call_dictionary_save_failed))
            SecondaryButton(stringResource(R.string.call_dictionary_save), onSaveWord)
        }
    }
}

@Composable
private fun CallDictionaryMessage(text: String) {
    DetailText(text = text)
}
