package com.project.speakmate.ui.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.utils.AppConstant

@Composable
fun AssessmentCriterionCard(
    title: String,
    description: String,
    statusLabel: String,
    statusColor: Color,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 16.dp,
    isStacked: Boolean = LocalDensity.current.fontScale > 1f,
    icon: @Composable (() -> Unit)? = null,
    status: @Composable (() -> Unit)? = null
) {
    SpeakMateCard(modifier = modifier, contentPadding = contentPadding) {
        if (isStacked) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssessmentCriterionDescription(title, description)
                if (status != null) status() else AssessmentCriterionStatus(statusLabel, statusColor)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.invoke()
                AssessmentCriterionDescription(
                    title = title,
                    description = description,
                    modifier = Modifier.weight(1f)
                )
                if (status != null) status() else AssessmentCriterionStatus(statusLabel, statusColor)
            }
        }
    }
}

@Composable
private fun AssessmentCriterionDescription(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TitleText(text = title)
        DetailText(text = description)
    }
}

@Composable
private fun AssessmentCriterionStatus(statusLabel: String, statusColor: Color) {
    CaptionText(
        text = statusLabel,
        style = DesignTypography.Badge,
        color = statusColor,
        modifier =
            Modifier.background(statusColor.copy(alpha = AppConstant.STATUS_CONTAINER_ALPHA), CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
