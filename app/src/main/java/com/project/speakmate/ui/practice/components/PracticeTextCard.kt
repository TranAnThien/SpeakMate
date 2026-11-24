package com.project.speakmate.ui.practice.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun PracticeTextCard(title: String, text: String, isHighlighted: Boolean, modifier: Modifier = Modifier) {
    val formattedText = remember(text) { text.parseMarkdownBold() }
    SpeakMateCard(
        modifier = modifier,
        backgroundColor = if (isHighlighted) DesignColor.SecondaryButton else DesignColor.Surface,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TitleText(text = title)
        Text(
            text = formattedText,
            style = DesignTypography.Detail,
            color = if (isHighlighted) DesignColor.Primary else DesignColor.Muted
        )
    }
}
