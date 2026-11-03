package com.project.speakmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 56.dp
) {
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = minHeight)
            .clip(RoundedCornerShape(16.dp)).background(DesignColor.Primary)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = (minHeight / 2 - 12.dp).coerceAtLeast(0.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = DesignTypography.Button, color = DesignColor.Surface, textAlign = TextAlign.Center)
    }
}
