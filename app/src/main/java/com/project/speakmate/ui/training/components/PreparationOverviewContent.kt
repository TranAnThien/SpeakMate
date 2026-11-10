package com.project.speakmate.ui.training.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.state.PreparationModuleUiState
import com.project.speakmate.ui.training.state.PreparationOverviewUiState
import com.project.speakmate.ui.training.state.PreparationSection

@Composable
fun PreparationOverviewContent(overview: PreparationOverviewUiState, onSelectSection: (PreparationSection) -> Unit) {
    Box(Modifier.fillMaxWidth().height(142.dp).clip(RoundedCornerShape(12.dp))) {
        if (overview.imageUrl.isNotBlank()) {
            AsyncImage(
                model = overview.imageUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(painterResource(R.drawable.img_training_preparation_cafe), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp).padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(overview.scenarioTitle, style = DesignTypography.SectionTitle, color = DesignColor.Surface)
            Text(overview.scenarioRoles, style = DesignTypography.Detail, color = DesignColor.Surface)
        }
    }
    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.preparation_communicative_focus), style = DesignTypography.Label, color = DesignColor.Muted)
        Text(overview.communicativeFocus, style = DesignTypography.Detail, color = DesignColor.Text)
    }
    Text(stringResource(R.string.preparation_modules_heading), style = DesignTypography.Label, color = DesignColor.Muted)
    overview.modules.forEach { module ->
        key(module.section) {
            val onOpen = remember(module.section, onSelectSection) { { onSelectSection(module.section) } }
            PreparationModuleCard(module, onOpen)
        }
    }
    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.preparation_required_actions), style = DesignTypography.Label, color = DesignColor.Muted)
        overview.requiredActions.forEach { action ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_training_preparation_requirement), null, Modifier.size(20.dp))
                Text(action, style = DesignTypography.Detail, color = DesignColor.Text, modifier = Modifier.weight(1f))
            }
        }
    }
    PreparationNote(stringResource(R.string.preparation_overview_note))
}

@Composable
private fun PreparationModuleCard(module: PreparationModuleUiState, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DesignColor.Surface)
        .clickable(onClick = onOpen).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(DesignColor.PaleBlue, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Image(painterResource(when (module.section) {
                    PreparationSection.VOCABULARY -> R.drawable.ic_training_preparation_vocabulary
                    PreparationSection.GRAMMAR -> R.drawable.ic_placement_range
                    PreparationSection.SENTENCE_PATTERNS -> R.drawable.ic_training_preparation_patterns
                    PreparationSection.OVERVIEW -> R.drawable.ic_training_preparation_vocabulary
                }), null, Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(module.title, style = DesignTypography.Title, color = DesignColor.Text)
                Text(module.description, style = DesignTypography.Detail, color = DesignColor.Muted)
            }
        }
        Text(stringResource(R.string.preparation_open_content), style = DesignTypography.Detail, color = DesignColor.Primary)
    }
}
