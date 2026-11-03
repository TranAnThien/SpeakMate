package com.project.speakmate.ui.call.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.call.state.CallDictionaryUiState
import com.project.speakmate.ui.call.state.CallTranscriptMessageUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun CallTranscriptSheet(
    messages: List<CallTranscriptMessageUiState>,
    dictionary: CallDictionaryUiState?,
    scrollState: ScrollState,
    onClose: () -> Unit,
    onSelectWord: (String, String, Rect) -> Unit,
    onCloseDictionary: () -> Unit,
    onPlayDictionaryWord: () -> Unit,
    onSaveDictionaryWord: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier.fillMaxSize(), color = DesignColor.TranscriptScrim) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val sheetHeight = maxHeight.coerceAtMost(623.dp)
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(sheetHeight)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(DesignColor.Surface)) {
                Column(Modifier.fillMaxSize()) {
                    CallTranscriptHeader(onClose)
                    Column(Modifier.weight(1f).fillMaxWidth().drawWithContent {
                        drawContent()
                        val range = scrollState.maxValue
                        if (range > 0 && range != Int.MAX_VALUE && size.height > 0f) {
                            val track = (size.height - 24.dp.toPx()).coerceAtLeast(0f)
                            val extent = (size.height + range - 24.dp.toPx()).coerceAtLeast(size.height)
                            val thumb = (track * size.height / extent).coerceAtMost(track)
                            val top = 12.dp.toPx() + (track - thumb) * scrollState.value.toFloat() / range
                            drawRoundRect(DesignColor.ScrollIndicator, Offset(size.width - 7.dp.toPx(), top),
                                Size(3.dp.toPx(), thumb), CornerRadius(2.dp.toPx()))
                        }
                    }.verticalScroll(scrollState).padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        messages.forEach { message ->
                            key(message.id) { CallTranscriptMessage(message, dictionary, onSelectWord) }
                        }
                    }
                    Text(stringResource(R.string.call_transcript_footer), style = DesignTypography.Caption, color = DesignColor.Muted,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                            .padding(top = 14.dp, bottom = 28.dp))
                }
                if (dictionary != null) {
                    var popupHeightPx by remember { mutableIntStateOf(0) }
                    val density = LocalDensity.current
                    val availableHeightPx = with(density) { sheetHeight.roundToPx() }
                    val anchorTop = if (dictionary.anchorTopDp.isFinite()) dictionary.anchorTopDp.dp else 0.dp
                    Box(Modifier.fillMaxWidth().padding(horizontal = 40.dp).offset {
                        IntOffset(0, with(density) { anchorTop.roundToPx() }.coerceIn(0, (availableHeightPx - popupHeightPx).coerceAtLeast(0)))
                    }, contentAlignment = Alignment.TopCenter) {
                        CallDictionaryPopup(dictionary, onCloseDictionary, onPlayDictionaryWord, onSaveDictionaryWord,
                            Modifier.widthIn(max = 316.dp).heightIn(max = sheetHeight).onSizeChanged { popupHeightPx = it.height })
                    }
                }
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
