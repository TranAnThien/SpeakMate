package com.project.speakmate.ui.practice.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun PracticeTextCard(title: String, text: String, isHighlighted: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(if (isHighlighted) DesignColor.SecondaryButton else DesignColor.Surface,
        RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = DesignTypography.Title, color = DesignColor.Text)
        Text(text, style = DesignTypography.Detail, color = if (isHighlighted) DesignColor.Primary else DesignColor.Muted)
    }
}
