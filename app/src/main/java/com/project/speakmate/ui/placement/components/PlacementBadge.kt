package com.project.speakmate.ui.placement.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun PlacementBadge(text: String, modifier: Modifier = Modifier) {
    CaptionText(
        text = text,
        modifier =
            modifier
                .background(DesignColor.PrimaryContainer, RoundedCornerShape(100.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
        color = DesignColor.Primary,
        style = DesignTypography.Badge
    )
}
