package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
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
fun PreparationNote(text: String, modifier: Modifier = Modifier) {
    Text(text, style = DesignTypography.Detail, color = DesignColor.Primary,
        modifier = modifier.fillMaxWidth().background(DesignColor.SecondaryButton, RoundedCornerShape(16.dp)).padding(16.dp))
}
