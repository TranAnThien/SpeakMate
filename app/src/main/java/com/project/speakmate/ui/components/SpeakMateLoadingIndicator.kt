package com.project.speakmate.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun SpeakMateLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    strokeWidth: Dp = 3.dp
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = DesignColor.Primary,
        strokeWidth = strokeWidth
    )
}
