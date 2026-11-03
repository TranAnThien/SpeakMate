package com.project.speakmate.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.project.speakmate.ui.history.state.SessionMessageUiState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun SessionTranscriptMessage(message: SessionMessageUiState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message.metadata, style = DesignTypography.Badge, color = DesignColor.Muted,
            textAlign = if (message.isUser) TextAlign.End else TextAlign.Start)
        Text(message.text, style = DesignTypography.Detail, color = if (message.isUser) DesignColor.Surface else DesignColor.Text,
            modifier = Modifier.fillMaxWidth().padding(start = if (message.isUser) 32.dp else 0.dp,
                end = if (message.isUser) 0.dp else 32.dp)
                .background(if (message.isUser) DesignColor.Primary else DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp))
    }
}
