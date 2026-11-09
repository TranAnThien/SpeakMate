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
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun CallDictionaryPopup(
    state: CallDictionaryUiState,
    onClose: () -> Unit,
    onPlayPronunciation: () -> Unit,
    onSaveWord: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier.fillMaxWidth(), color = DesignColor.Surface, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DesignColor.DictionaryBorder)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CallDictionaryHeader(state, onClose, onPlayPronunciation)
            if (state.hasPronunciationError) {
                Text(stringResource(R.string.call_pronunciation_unavailable), style = DesignTypography.Caption, color = DesignColor.Muted)
            }
            when (state.status) {
                CallDictionaryStatus.LOADING -> CallDictionaryMessage(stringResource(R.string.call_dictionary_loading))
                CallDictionaryStatus.NOT_FOUND -> CallDictionaryMessage(stringResource(R.string.call_dictionary_not_found))
                CallDictionaryStatus.ERROR -> {
                    CallDictionaryMessage(stringResource(R.string.call_dictionary_error))
                    CallSecondaryButton(stringResource(R.string.call_dictionary_retry), onRetry)
                }
                CallDictionaryStatus.SUCCESS -> CallDictionaryContent(state, onSaveWord)
            }
        }
    }
}

@Composable
private fun CallDictionaryHeader(state: CallDictionaryUiState, onClose: () -> Unit, onPlayPronunciation: () -> Unit) {
    val phoneticLine = listOf(state.phonetic, state.partOfSpeech).filter { it.isNotBlank() }
        .joinToString(stringResource(R.string.call_dictionary_detail_separator))
    val playLabel = stringResource(R.string.call_dictionary_play_pronunciation)
    val closeLabel = stringResource(R.string.call_dictionary_close)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(state.word, style = DesignTypography.Title, color = DesignColor.Text)
            if (phoneticLine.isNotEmpty()) Text(phoneticLine, style = DesignTypography.Detail, color = DesignColor.Muted)
        }
        Box(Modifier.size(48.dp).background(DesignColor.PaleBlue, RoundedCornerShape(12.dp))
            .clickable(enabled = !state.isPronouncing, role = Role.Button, onClick = onPlayPronunciation)
            .semantics { contentDescription = playLabel },
            contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_training_preparation_speaker), null, Modifier.size(22.dp))
        }
        Box(Modifier.size(48.dp).clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = closeLabel },
            contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_call_close), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(DesignColor.Muted))
        }
    }
}

@Composable
private fun CallDictionaryContent(state: CallDictionaryUiState, onSaveWord: () -> Unit) {
    Text(stringResource(R.string.call_dictionary_meaning, state.meaning), style = DesignTypography.Detail, color = DesignColor.Text)
    if (state.exampleSentence.isNotBlank()) {
        Column(Modifier.fillMaxWidth().background(DesignColor.ExampleSurface, RoundedCornerShape(16.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(state.exampleSentence, style = DesignTypography.Detail, color = DesignColor.Text)
            if (state.exampleTranslation.isNotBlank()) {
                Text(state.exampleTranslation, style = DesignTypography.Detail, color = DesignColor.Muted)
            }
        }
    }
    if (!state.canSave) {
        CallDictionaryMessage(stringResource(R.string.call_dictionary_insufficient))
        return
    }
    when (state.saveStatus) {
        CallVocabularySaveStatus.IDLE -> CallSecondaryButton(stringResource(R.string.call_dictionary_save), onSaveWord)
        CallVocabularySaveStatus.SAVING -> CallDictionaryMessage(stringResource(R.string.call_dictionary_saving))
        CallVocabularySaveStatus.SAVED -> CallDictionaryMessage(stringResource(R.string.call_dictionary_saved))
        CallVocabularySaveStatus.MOVED_TO_TOP -> CallDictionaryMessage(stringResource(R.string.call_dictionary_moved_to_top))
        CallVocabularySaveStatus.SAVED_REPLACING_OLDEST -> CallDictionaryMessage(stringResource(R.string.call_dictionary_oldest_removed))
        CallVocabularySaveStatus.FAILED -> {
            CallDictionaryMessage(stringResource(R.string.call_dictionary_save_failed))
            CallSecondaryButton(stringResource(R.string.call_dictionary_save), onSaveWord)
        }
    }
}

@Composable
private fun CallDictionaryMessage(text: String) {
    Text(text, style = DesignTypography.Detail, color = DesignColor.Muted)
}
