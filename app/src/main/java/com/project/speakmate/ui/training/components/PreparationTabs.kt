package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.state.PreparationSection

@Composable
fun PreparationTabs(
    selectedSection: PreparationSection,
    onSelectSection: (PreparationSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PreparationSection.entries.forEach { section ->
            key(section) {
                val onSelect = remember(section, onSelectSection) { { onSelectSection(section) } }
                Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (section == selectedSection) DesignColor.Primary else DesignColor.Surface)
                    .clickable(onClick = onSelect).padding(horizontal = 4.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(when (section) {
                        PreparationSection.OVERVIEW -> R.string.preparation_overview
                        PreparationSection.VOCABULARY -> R.string.preparation_vocabulary
                        PreparationSection.GRAMMAR -> R.string.preparation_grammar
                        PreparationSection.SENTENCE_PATTERNS -> R.string.preparation_patterns
                    }), style = DesignTypography.Badge, textAlign = TextAlign.Center,
                        color = if (section == selectedSection) DesignColor.Surface else DesignColor.Muted)
                }
            }
        }
    }
}
