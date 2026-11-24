package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun TrainingPill(
    text: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    color: Color = DesignColor.Muted
) {
    CaptionText(
        text = text,
        color = color,
        style = DesignTypography.Badge,
        modifier =
            modifier
                .background(backgroundColor, RoundedCornerShape(28.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
