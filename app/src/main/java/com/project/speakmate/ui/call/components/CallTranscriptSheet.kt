package com.project.speakmate.ui.call.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.call.state.CallDictionaryUiState
import com.project.speakmate.ui.call.state.CallTranscriptMessageUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun CallTranscriptSheet(
    messages: List<CallTranscriptMessageUiState>,
    dictionary: CallDictionaryUiState?,
    listState: LazyListState,
    onClose: () -> Unit,
    onSelectWord: (String, Int, String) -> Unit,
    onCloseDictionary: () -> Unit,
    onPlayDictionaryWord: () -> Unit,
    onSaveDictionaryWord: () -> Unit,
    onRetryDictionary: () -> Unit,
    modifier: Modifier = Modifier,
    initialDictionaryAnchorTop: Dp = 0.dp,
    onStopPronunciation: () -> Unit = {}
) {
    val visibleMessages = remember(messages) { messages.distinctBy { it.id } }
    Surface(modifier.fillMaxSize(), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = DesignColor.Surface) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val sheetMaxHeight = maxHeight
            val density = LocalDensity.current
            val scope = rememberCoroutineScope()
            val latestDictionary by rememberUpdatedState(dictionary)
            var sheetPosition by remember { mutableStateOf(Offset.Zero) }
            var anchor by remember { mutableStateOf<Rect?>(null) }
            var popupHeight by remember { mutableIntStateOf(0) }
            var isFollowing by remember(listState) { mutableStateOf(listState.layoutInfo.visibleItemsInfo.isEmpty() || !listState.canScrollForward) }
            var isAutoScrolling by remember(listState) { mutableStateOf(false) }
            var hasNewMessages by remember(listState) { mutableStateOf(false) }
            LaunchedEffect(listState) {
                snapshotFlow { listState.isScrollInProgress to listState.canScrollForward }.collect { (scrolling, canScroll) ->
                    if (scrolling && !isAutoScrolling) isFollowing = !canScroll
                    if (!canScroll && latestDictionary == null) { hasNewMessages = false; isFollowing = true }
                }
            }
            LaunchedEffect(dictionary != null) { if (dictionary != null) isFollowing = false }
            val last = visibleMessages.lastOrNull()
            LaunchedEffect(last?.id, last?.text, last?.isPartial) {
                if (last != null) {
                    if (isFollowing && dictionary == null && !listState.isScrollInProgress) {
                        isAutoScrolling = true
                        try {
                            listState.scrollToItem(visibleMessages.lastIndex)
                            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.let { listState.scrollBy(it.size.toFloat()) }
                        } finally { isAutoScrolling = false }
                    } else hasNewMessages = true
                }
            }
            val selectWord = remember(onSelectWord) {
                { id: String, start: Int, word: String, bounds: Rect ->
                    anchor = bounds
                    onSelectWord(id, start, word)
                }
            }
            val updateAnchor = remember {
                { id: String, start: Int, bounds: Rect ->
                    val selection = latestDictionary
                    if (selection != null && selection.messageId == id && selection.selectionStart == start) anchor = bounds
                }
            }
            Column(Modifier.fillMaxSize().onGloballyPositioned { sheetPosition = it.positionInWindow() }) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.call_transcript_title), Modifier.weight(1f), style = DesignTypography.Title, color = DesignColor.Primary)
                    IconButton(onClick = onClose) { Icon(painterResource(R.drawable.ic_call_close), stringResource(R.string.call_transcript_close)) }
                }
                if (sheetMaxHeight > 360.dp) Text(stringResource(R.string.call_transcript_hint), Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = DesignTypography.Detail, color = DesignColor.Muted)
                if (visibleMessages.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.call_transcript_empty), style = DesignTypography.Detail, color = DesignColor.Muted)
                    }
                } else LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(visibleMessages, key = { it.id }, contentType = { it.isUser }) { message ->
                        CallTranscriptMessage(message, dictionary?.takeIf { it.messageId == message.id }, selectWord, onSelectionBoundsChanged = updateAnchor)
                    }
                }
                if (hasNewMessages) TextButton(onClick = {
                    onCloseDictionary()
                    isFollowing = true
                    hasNewMessages = false
                    scope.launch {
                        if (visibleMessages.isNotEmpty()) {
                            listState.animateScrollToItem(visibleMessages.lastIndex)
                            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.let { listState.scrollBy(it.size.toFloat()) }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.call_transcript_new_messages)) }
            }
            if (dictionary != null) {
                val width = minOf(316.dp, (maxWidth - 32.dp).coerceAtLeast(0.dp))
                val widthPx = with(density) { width.roundToPx() }
                val availableHeight = with(density) { maxHeight.roundToPx() }
                val availableWidth = with(density) { maxWidth.roundToPx() }
                val gap = with(density) { 8.dp.roundToPx() }
                val initialY = with(density) { initialDictionaryAnchorTop.roundToPx() }
                Box(Modifier.width(width).offset {
                    val bounds = anchor
                    val center = bounds?.center?.x?.minus(sheetPosition.x) ?: availableWidth / 2f
                    val below = bounds?.bottom?.minus(sheetPosition.y)?.roundToInt()?.plus(gap) ?: initialY
                    val above = bounds?.top?.minus(sheetPosition.y)?.roundToInt()?.minus(popupHeight + gap) ?: initialY
                    IntOffset((center - widthPx / 2f).roundToInt().coerceIn(0, (availableWidth - widthPx).coerceAtLeast(0)),
                        (if (below + popupHeight <= availableHeight) below else above).coerceIn(0, (availableHeight - popupHeight).coerceAtLeast(0)))
                }) {
                    CallDictionaryPopup(dictionary, onCloseDictionary, onPlayDictionaryWord, onSaveDictionaryWord, onRetryDictionary,
                        Modifier.heightIn(max = sheetMaxHeight).onSizeChanged { popupHeight = it.height }, onStopPronunciation = onStopPronunciation)
                }
            }
        }
    }
}
