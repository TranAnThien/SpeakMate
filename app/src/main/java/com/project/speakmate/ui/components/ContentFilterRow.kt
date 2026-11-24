package com.project.speakmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun ContentFilterRow(
    filters: List<ContentFilterUiState>,
    selectedId: String,
    onSelectFilter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items = filters, key = { it.id }, contentType = { ContentFilterUiState::class }) { filter ->
            val isSelected = filter.id == selectedId
            val onSelect = remember(filter.id, onSelectFilter) { { onSelectFilter(filter.id) } }
            Box(
                Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = onSelect),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .heightIn(min = 32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) DesignColor.SecondaryContainer else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color.Transparent else DesignColor.Outline,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DetailText(
                        text = filter.label,
                        style = DesignTypography.Button,
                        color = if (isSelected) DesignColor.OnSecondaryContainer else DesignColor.Muted,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
