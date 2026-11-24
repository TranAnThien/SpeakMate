package com.project.speakmate.ui.practice.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.assessment.rememberSpeakingLevelLabel
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.image.ScenarioImage
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.practice.state.ScenarioItemUiState
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun ScenarioCard(scenario: ScenarioItemUiState, onSelectScenario: () -> Unit, modifier: Modifier = Modifier) {
    SpeakMateCard(modifier = modifier, onClick = onSelectScenario) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScenarioImage(imageUrl = scenario.imageUrl, modifier = Modifier.size(72.dp), isCompact = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TitleText(text = scenario.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                DetailText(text = scenario.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val level = scenario.suggestedLevel
                val minutes = scenario.estimatedDurationMinutes
                val metadata =
                    when {
                        level != null && minutes != null ->
                            pluralStringResource(
                                R.plurals.practice_level_duration,
                                minutes,
                                rememberSpeakingLevelLabel(level),
                                minutes
                            )
                        level != null -> rememberSpeakingLevelLabel(level)
                        minutes != null -> pluralStringResource(R.plurals.practice_duration_minutes, minutes, minutes)
                        else -> ""
                    }
                if (metadata.isNotBlank()) CaptionText(text = metadata, color = DesignColor.Primary)
            }
            Icon(
                painter = painterResource(R.drawable.ic_content_chevron),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = DesignColor.Muted
            )
        }
    }
}
