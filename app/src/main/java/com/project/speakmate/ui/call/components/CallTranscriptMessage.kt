package com.project.speakmate.ui.call.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.call.state.CallDictionaryUiState
import com.project.speakmate.ui.call.state.CallTranscriptMessageUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.utils.AppConstant
import java.text.BreakIterator
import java.util.Locale

@Composable
fun CallTranscriptMessage(
    message: CallTranscriptMessageUiState,
    dictionary: CallDictionaryUiState?,
    onSelectWord: (String, Int, String, Rect) -> Unit,
    modifier: Modifier = Modifier,
    onSelectionBoundsChanged: (String, Int, Rect) -> Unit = { _, _, _ -> }
) {
    var textLayout by remember(message.id) { mutableStateOf<TextLayoutResult?>(null) }
    var textCoordinates by remember(message.id) { mutableStateOf<LayoutCoordinates?>(null) }
    val words = remember(message.text) {
        val iterator = BreakIterator.getWordInstance(Locale.ENGLISH)
        iterator.setText(message.text)
        buildList {
            var start = iterator.first()
            var end = iterator.next()
            while (end != BreakIterator.DONE) {
                val word = message.text.substring(start, end)
                if (word.any { it.isLetterOrDigit() }) add(start to word)
                start = end
                end = iterator.next()
            }
        }
    }
    var wordPage by remember(message.id, message.text) { mutableIntStateOf(0) }
    val firstWord = wordPage * AppConstant.TRANSCRIPT_ACCESSIBILITY_WORDS_PER_PAGE
    val pageWords = remember(words, firstWord) {
        words.drop(firstWord).take(AppConstant.TRANSCRIPT_ACCESSIBILITY_WORDS_PER_PAGE)
    }
    val wordLabels = pageWords.mapIndexed { index, (_, word) ->
        stringResource(R.string.call_transcript_lookup_word, word, firstWord + index + 1)
    }
    val previousWordsLabel = stringResource(R.string.call_transcript_previous_words)
    val nextWordsLabel = stringResource(R.string.call_transcript_next_words)
    val wordActions = remember(words, pageWords, wordLabels, firstWord, previousWordsLabel, nextWordsLabel, message.id, message.text, onSelectWord) {
        buildList {
            pageWords.forEachIndexed { index, (start, word) ->
                add(CustomAccessibilityAction(wordLabels[index]) {
                    selectTranscriptWord(message, start, word, textLayout, textCoordinates, onSelectWord)
                })
            }
            if (firstWord > 0) add(CustomAccessibilityAction(previousWordsLabel) {
                wordPage = (wordPage - 1).coerceAtLeast(0)
                true
            })
            if (firstWord + pageWords.size < words.size) add(CustomAccessibilityAction(nextWordsLabel) {
                val lastPage = (words.size - 1) / AppConstant.TRANSCRIPT_ACCESSIBILITY_WORDS_PER_PAGE
                wordPage = (wordPage + 1).coerceAtMost(lastPage)
                true
            })
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start) {
        Text(stringResource(if (message.isUser) R.string.call_transcript_user else R.string.call_transcript_ai),
            style = DesignTypography.Badge, color = DesignColor.Muted)
        Column(Modifier.widthIn(max = 300.dp).fillMaxWidth()
            .background(if (message.isUser) DesignColor.TranscriptUser else DesignColor.ExampleSurface, RoundedCornerShape(16.dp)).padding(12.dp)) {
            BasicText(
                text = message.text,
                style = DesignTypography.Detail.copy(color = if (message.isPartial) DesignColor.Muted else DesignColor.Text),
                onTextLayout = { textLayout = it },
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    textCoordinates = coordinates
                    val selection = dictionary
                    val layout = textLayout
                    if (selection != null && selection.messageId == message.id && layout != null &&
                        layout.layoutInput.text.text == message.text && selection.selectionStart in message.text.indices &&
                        message.text.regionMatches(selection.selectionStart, selection.word, 0, selection.word.length)) {
                        val bounds = layout.getBoundingBox(selection.selectionStart)
                        val position = coordinates.localToWindow(Offset(bounds.left, bounds.top))
                        onSelectionBoundsChanged(message.id, selection.selectionStart, Rect(position.x, position.y, position.x + bounds.width, position.y + bounds.height))
                    }
                }.drawBehind {
                    val layout = textLayout
                    val selection = dictionary
                    if (layout != null && layout.layoutInput.text.text == message.text && selection != null && selection.messageId == message.id) {
                        val start = selection.selectionStart.coerceIn(0, message.text.length)
                        val length = selection.word.length.coerceAtMost(message.text.length - start)
                        if (length > 0 && length == selection.word.length && message.text.regionMatches(start, selection.word, 0, length)) {
                            val end = start + length
                            val first = layout.getBoundingBox(start)
                            val last = layout.getBoundingBox(end - 1)
                            if (first.top == last.top) {
                                val left = minOf(first.left, last.left)
                                val right = maxOf(first.right, last.right)
                                drawRoundRect(DesignColor.DictionarySelection, Offset(left - 2.dp.toPx(), first.top),
                                    Size(right - left + 4.dp.toPx(), first.height), CornerRadius(12.dp.toPx()))
                            } else drawPath(layout.getPathForRange(start, end), DesignColor.DictionarySelection)
                        }
                    }
                }.semantics { customActions = wordActions }.pointerInput(message.id, message.text, onSelectWord) {
                    detectTapGestures(onLongPress = onLongPress@{ point ->
                        val layout = textLayout ?: return@onLongPress
                        val coordinates = textCoordinates ?: return@onLongPress
                        if (!coordinates.isAttached || message.text.isEmpty() || layout.layoutInput.text.text != message.text) return@onLongPress
                        val offset = layout.getOffsetForPosition(point).coerceIn(0, message.text.lastIndex)
                        val range = layout.getWordBoundary(offset)
                        val start = range.min.coerceIn(0, message.text.length)
                        val end = range.max.coerceIn(start, message.text.length)
                        val word = message.text.substring(start, end)
                        selectTranscriptWord(message, start, word, layout, coordinates, onSelectWord)
                    })
                }
            )
        }
        if (message.isPartial && !message.isInterrupted) Text(stringResource(R.string.call_transcript_partial), style = DesignTypography.Caption, color = DesignColor.Muted)
        if (message.isInterrupted) {
            Text(stringResource(R.string.call_transcript_interrupted), style = DesignTypography.Caption, color = DesignColor.Muted)
        }
    }
}

private fun selectTranscriptWord(
    message: CallTranscriptMessageUiState,
    start: Int,
    word: String,
    layout: TextLayoutResult?,
    coordinates: LayoutCoordinates?,
    onSelectWord: (String, Int, String, Rect) -> Unit
): Boolean {
    if (layout == null || coordinates == null || !coordinates.isAttached ||
        layout.layoutInput.text.text != message.text || start !in message.text.indices ||
        word.none { it.isLetterOrDigit() } || word.length > message.text.length - start ||
        !message.text.regionMatches(start, word, 0, word.length)) return false
    val bounds = layout.getBoundingBox(start)
    val position = coordinates.localToWindow(Offset(bounds.left, bounds.top))
    onSelectWord(message.id, start, word, Rect(position.x, position.y, position.x + bounds.width, position.y + bounds.height))
    return true
}
