package com.project.speakmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun AssessmentCriterionCard(
    title: String,
    description: String,
    statusLabel: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = DesignTypography.Title, color = DesignColor.Text, modifier = Modifier.weight(1f))
            Text(statusLabel, style = DesignTypography.Detail, color = statusColor, modifier = Modifier.width(124.dp))
        }
        Text(description, style = DesignTypography.Detail, color = DesignColor.Muted)
    }
}
