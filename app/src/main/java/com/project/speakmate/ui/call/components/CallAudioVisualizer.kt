package com.project.speakmate.ui.call.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignColor
import kotlin.math.abs

@Composable
fun CallAudioVisualizer(readLevel: () -> Float, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 72.dp, height = 28.dp)) {
        val strength = readLevel()
        val radius = 2.dp.toPx()
        val amplitude = radius + (center.y - 2 * radius) * strength
        for (bar in -1..1) {
            val x = center.x + bar * 12.dp.toPx()
            val halfHeight = amplitude / (1 + abs(bar))
            drawLine(DesignColor.IconBlue, Offset(x, center.y - halfHeight),
                Offset(x, center.y + halfHeight), 2 * radius, StrokeCap.Round)
        }
    }
}
