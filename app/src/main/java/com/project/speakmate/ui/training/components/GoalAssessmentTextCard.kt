package com.project.speakmate.ui.training.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun GoalAssessmentTextCard(title: String, text: String, isHighlighted: Boolean, modifier: Modifier = Modifier) {
    SpeakMateCard(
        modifier = modifier,
        backgroundColor = if (isHighlighted) DesignColor.SecondaryButton else DesignColor.Surface,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TitleText(text = title)
        DetailText(text = text, color = if (isHighlighted) DesignColor.Primary else DesignColor.Muted)
    }
}
