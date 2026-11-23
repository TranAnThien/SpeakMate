package com.project.speakmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.utils.AppConstant

@Composable
fun AssessmentCriterionCard(
    title: String,
    description: String,
    statusLabel: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    val cardModifier = modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp)
    if (LocalDensity.current.fontScale > 1f) {
        Column(cardModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AssessmentCriterionDescription(title, description)
            AssessmentCriterionStatus(statusLabel, statusColor)
        }
    } else {
        Row(cardModifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AssessmentCriterionDescription(title, description, Modifier.weight(1f))
            AssessmentCriterionStatus(statusLabel, statusColor)
        }
    }
}

@Composable
private fun AssessmentCriterionDescription(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = DesignTypography.Title, color = DesignColor.Text)
        Text(description, style = DesignTypography.Detail, color = DesignColor.Muted)
    }
}

@Composable
private fun AssessmentCriterionStatus(statusLabel: String, statusColor: Color) {
    Text(statusLabel, style = DesignTypography.Badge, color = statusColor,
        modifier = Modifier.background(statusColor.copy(alpha = AppConstant.STATUS_CONTAINER_ALPHA), CircleShape).padding(horizontal = 12.dp, vertical = 6.dp))
}
