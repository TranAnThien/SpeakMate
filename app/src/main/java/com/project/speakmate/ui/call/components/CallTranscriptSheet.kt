package com.project.speakmate.ui.call.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.call.state.CallDictionaryUiState
import com.project.speakmate.ui.call.state.CallTranscriptMessageUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import kotlin.math.roundToInt

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
    initialDictionaryAnchorTop: Dp = 0.dp
) {
    Surface(modifier.fillMaxSize(), color = DesignColor.TranscriptScrim) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val sheetHeight = maxHeight.coerceAtMost(623.dp)
            val density = LocalDensity.current
            var sheetTopPx by remember { mutableFloatStateOf(0f) }
            var anchorTopPx by remember { mutableStateOf<Int?>(null) }
            val selectWord = remember(onSelectWord, density) {
                { messageId: String, start: Int, word: String, bounds: Rect ->
                    anchorTopPx = (bounds.bottom - sheetTopPx).roundToInt() + with(density) { 8.dp.roundToPx() }
                    onSelectWord(messageId, start, word)
                }
            }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(sheetHeight)
                .onGloballyPositioned { sheetTopPx = it.positionInWindow().y }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(DesignColor.Surface)) {
                Column(Modifier.fillMaxSize()) {
                    CallTranscriptHeader(onClose)
                    CallTranscriptFollowEffect(messages, listState, isDictionaryOpen = dictionary != null)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth().drawWithContent {
                            drawContent()
                            val info = listState.layoutInfo
                            val visible = info.visibleItemsInfo
                            val total = info.totalItemsCount
                            val first = visible.firstOrNull()
                            if (first != null && total > 0 && size.height > 0f &&
                                (listState.canScrollForward || listState.canScrollBackward)
                            ) {
                                val track = (size.height - 24.dp.toPx()).coerceAtLeast(0f)
                                val thumb = track * (visible.size.toFloat() / total).coerceAtMost(1f)
                                val scrolled = first.index - first.offset.toFloat() / first.size.coerceAtLeast(1)
                                val progress = if (!listState.canScrollForward) 1f
                                    else (scrolled / (total - visible.size).coerceAtLeast(1)).coerceIn(0f, 1f)
                                drawRoundRect(DesignColor.ScrollIndicator, Offset(size.width - 7.dp.toPx(), 12.dp.toPx() + (track - thumb) * progress),
                                    Size(3.dp.toPx(), thumb), CornerRadius(2.dp.toPx()))
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(messages, key = { it.id }, contentType = { it.isUser }) { message ->
                            CallTranscriptMessage(message, dictionary?.takeIf { it.messageId == message.id }, selectWord)
                        }
                    }
                    Text(stringResource(R.string.call_transcript_footer), style = DesignTypography.Caption, color = DesignColor.Muted,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                            .padding(top = 14.dp, bottom = 28.dp))
                }
                if (dictionary != null) {
                    var popupHeightPx by remember { mutableIntStateOf(0) }
                    val availableHeightPx = with(density) { sheetHeight.roundToPx() }
                    val initialAnchorPx = with(density) { initialDictionaryAnchorTop.roundToPx() }
                    Box(Modifier.fillMaxWidth().padding(horizontal = 40.dp).offset {
                        IntOffset(0, (anchorTopPx ?: initialAnchorPx).coerceIn(0, (availableHeightPx - popupHeightPx).coerceAtLeast(0)))
                    }, contentAlignment = Alignment.TopCenter) {
                        CallDictionaryPopup(dictionary, onCloseDictionary, onPlayDictionaryWord, onSaveDictionaryWord, onRetryDictionary,
                            Modifier.widthIn(max = 316.dp).heightIn(max = sheetHeight).onSizeChanged { popupHeightPx = it.height })
                    }
                }
            }
        }
    }
}

@Composable
private fun CallTranscriptFollowEffect(
    messages: List<CallTranscriptMessageUiState>,
    listState: LazyListState,
    isDictionaryOpen: Boolean
) {
    var isFollowing by remember(listState) { mutableStateOf(true) }
    var isAutoScrolling by remember(listState) { mutableStateOf(false) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to !listState.canScrollForward }
            .collect { (isScrolling, isAtEnd) -> if (isScrolling && !isAutoScrolling) isFollowing = isAtEnd }
    }
    val lastIndex = messages.lastIndex
    val lastLength = messages.lastOrNull()?.text?.length
    LaunchedEffect(lastIndex, lastLength, isDictionaryOpen) {
        if (isFollowing && !isDictionaryOpen && lastIndex >= 0) {
            isAutoScrolling = true
            try {
                listState.scrollToItem(lastIndex)
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.let { listState.scrollBy(it.size.toFloat()) }
            } finally {
                isAutoScrolling = false
            }
        }
    }
}

@Composable
private fun CallTranscriptHeader(onClose: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.align(Alignment.TopCenter).padding(top = 10.dp).size(48.dp, 4.dp)
            .background(DesignColor.ScrollIndicator, RoundedCornerShape(2.dp)))
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 92.dp, top = 32.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.call_transcript_title), style = DesignTypography.Title, color = DesignColor.Primary)
            Text(stringResource(R.string.call_transcript_subtitle), style = DesignTypography.Detail, color = DesignColor.Muted)
        }
        Box(Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = 20.dp).size(48.dp).clickable(onClick = onClose),
            contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_call_close), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(DesignColor.Muted))
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(stringResource(R.string.call_transcript_hint), style = DesignTypography.Detail, color = DesignColor.Primary,
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().background(DesignColor.PaleBlue, RoundedCornerShape(16.dp)).padding(12.dp))
    Spacer(Modifier.height(4.dp))
}
