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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.call.state.CallDictionaryUiState
import com.project.speakmate.ui.call.state.CallTranscriptMessageUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun CallTranscriptMessage(
    message: CallTranscriptMessageUiState,
    dictionary: CallDictionaryUiState?,
    onSelectWord: (String, Int, String, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    var textLayout by remember(message.id) { mutableStateOf<TextLayoutResult?>(null) }
    var textCoordinates by remember(message.id) { mutableStateOf<LayoutCoordinates?>(null) }
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
                modifier = Modifier.onGloballyPositioned { textCoordinates = it }.drawBehind {
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
                }.pointerInput(message.id, message.text, onSelectWord) {
                    detectTapGestures(onLongPress = onLongPress@{ point ->
                        val layout = textLayout ?: return@onLongPress
                        val coordinates = textCoordinates ?: return@onLongPress
                        if (!coordinates.isAttached || message.text.isEmpty() || layout.layoutInput.text.text != message.text) return@onLongPress
                        val offset = layout.getOffsetForPosition(point).coerceIn(0, message.text.lastIndex)
                        val range = layout.getWordBoundary(offset)
                        val start = range.min.coerceIn(0, message.text.length)
                        val end = range.max.coerceIn(start, message.text.length)
                        val word = message.text.substring(start, end)
                        if (word.none { it.isLetterOrDigit() }) return@onLongPress
                        val bounds = layout.getBoundingBox(start)
                        val position = coordinates.localToWindow(Offset(bounds.left, bounds.top))
                        onSelectWord(message.id, start, word, Rect(position.x, position.y, position.x + bounds.width, position.y + bounds.height))
                    })
                }
            )
        }
        if (message.isInterrupted) {
            Text(stringResource(R.string.call_transcript_interrupted), style = DesignTypography.Caption, color = DesignColor.Muted)
        }
    }
}
