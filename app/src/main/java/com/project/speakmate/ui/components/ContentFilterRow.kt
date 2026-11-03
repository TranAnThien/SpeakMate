package com.project.speakmate.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun ContentFilterRow(
    filters: List<ContentFilterUiState>,
    selectedId: String,
    onSelectFilter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        filters.forEachIndexed { index, filter ->
            key(index, filter.id) {
                val isSelected = filter.id == selectedId
                val onSelect = remember(filter.id, onSelectFilter) { { onSelectFilter(filter.id) } }
                Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) DesignColor.Primary else DesignColor.Surface).clickable(onClick = onSelect)
                    .padding(horizontal = 4.dp, vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Text(filter.label, style = DesignTypography.SectionLabel,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) DesignColor.Surface else DesignColor.Muted, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
