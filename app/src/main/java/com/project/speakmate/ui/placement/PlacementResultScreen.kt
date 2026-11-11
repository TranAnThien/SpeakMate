package com.project.speakmate.ui.placement

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.placement.components.PlacementAppBar
import com.project.speakmate.ui.placement.components.PlacementBadge
import com.project.speakmate.ui.placement.state.PlacementResultUiState
import com.project.speakmate.ui.placement.state.PlacementSkillUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.theme.SpeakMateTheme

@Composable
fun PlacementResultScreen(
    state: PlacementResultUiState,
    onBack: () -> Unit,
    onStartTrainingPath: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        PlacementAppBar(stringResource(R.string.placement_result_title), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 32.dp)) {
            PlacementLevelCard(state.level, state.levelLabel)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.placement_skill_details), color = DesignColor.Primary, style = DesignTypography.Button)
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlacementSkillCard(stringResource(R.string.placement_range), R.drawable.ic_placement_range, state.range)
                PlacementSkillCard(stringResource(R.string.placement_accuracy), R.drawable.ic_placement_accuracy, state.accuracy)
                PlacementSkillCard(stringResource(R.string.placement_fluency), R.drawable.ic_placement_fluency, state.fluency)
                PlacementSkillCard(stringResource(R.string.placement_interaction), R.drawable.ic_placement_interaction, state.interaction)
                PlacementSkillCard(stringResource(R.string.placement_coherence), R.drawable.ic_placement_coherence, state.coherence)
            }
        }
        Box(Modifier.fillMaxWidth().background(DesignColor.Surface).padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp)) {
            PrimaryButton(stringResource(R.string.placement_home), onStartTrainingPath)
        }
    }
}

@Composable
private fun PlacementLevelCard(level: String, levelLabel: String) {
    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.placement_estimated_level), color = DesignColor.Muted, style = DesignTypography.Label)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.size(64.dp).background(DesignColor.PaleBlue, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text(level, style = DesignTypography.Level, color = DesignColor.Primary)
        }
        Spacer(Modifier.height(8.dp))
        PlacementBadge(levelLabel)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.placement_result_disclaimer), style = DesignTypography.Caption,
            color = DesignColor.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PlacementSkillCard(title: String, @DrawableRes iconRes: Int, state: PlacementSkillUiState) {
    Row(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(32.dp).background(
            if (state.hasPassed) DesignColor.PaleBlue else DesignColor.ErrorSurface, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center) {
            Image(painterResource(iconRes), null, Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = DesignTypography.Title, color = DesignColor.Text)
            Text(state.feedback, style = DesignTypography.Detail, color = DesignColor.Muted)
        }
        Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Image(painterResource(if (state.hasPassed) R.drawable.ic_placement_passed else R.drawable.ic_placement_failed),
                null, Modifier.size(16.dp))
            Text(stringResource(if (state.hasPassed) R.string.placement_passed else R.string.placement_not_passed),
                style = DesignTypography.Badge, color = if (state.hasPassed) DesignColor.Success else DesignColor.Error)
        }
    }
}

@Preview(name = "Placement / Result / A2 / 17:54", widthDp = 396, heightDp = 819, showBackground = true)
@Composable
private fun PlacementResultA2ScreenPreview() {
    SpeakMateTheme {
        PlacementResultScreen(
            state = PlacementResultUiState(
                level = "A2",
                levelLabel = stringResource(R.string.placement_a2_label),
                range = PlacementSkillUiState(stringResource(R.string.placement_preview_range_feedback), true),
                accuracy = PlacementSkillUiState(stringResource(R.string.placement_preview_accuracy_feedback), false),
                fluency = PlacementSkillUiState(stringResource(R.string.placement_preview_fluency_feedback), true),
                interaction = PlacementSkillUiState(stringResource(R.string.placement_preview_interaction_feedback), true),
                coherence = PlacementSkillUiState(stringResource(R.string.placement_preview_coherence_feedback), true)
            ),
            onBack = {},
            onStartTrainingPath = {}
        )
    }
}
