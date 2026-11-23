package com.project.speakmate.ui.practice.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.project.speakmate.R
import com.project.speakmate.ui.practice.state.ScenarioItemUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.utils.extension.default

@Composable
fun ScenarioCard(scenario: ScenarioItemUiState, onSelectScenario: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DesignColor.Surface)
        .clickable(onClick = onSelectScenario).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        com.project.speakmate.ui.components.ScenarioImage(scenario.imageUrl, Modifier.size(72.dp), scenario.imageRes, isCompact = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(scenario.title, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = DesignTypography.Title, color = DesignColor.Text)
            Text(scenario.description, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = DesignTypography.Detail, color = DesignColor.Muted)
            val level = scenario.suggestedLevel
            val minutes = scenario.estimatedDurationMinutes
            val metadata = when {
                level != null && minutes != null -> pluralStringResource(R.plurals.practice_level_duration, minutes, level.name, minutes)
                level != null -> level.name
                minutes != null -> pluralStringResource(R.plurals.practice_duration_minutes, minutes, minutes)
                else -> ""
            }
            if (metadata.isNotBlank()) Text(metadata, style = DesignTypography.Caption, color = DesignColor.Primary)

        }
        Icon(painterResource(R.drawable.ic_content_chevron), null, Modifier.size(20.dp), tint = DesignColor.Muted)
    }
}
