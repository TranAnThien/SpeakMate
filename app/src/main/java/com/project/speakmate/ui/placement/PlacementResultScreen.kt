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
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.AnimatedMascot
import com.project.speakmate.ui.components.BackAppBar
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.placement.components.PlacementBadge
import com.project.speakmate.ui.placement.state.PlacementResultUiState
import com.project.speakmate.ui.placement.state.PlacementSkillUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun PlacementResultScreen(
    state: PlacementResultUiState,
    onBack: () -> Unit,
    onStartTrainingPath: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(stringResource(R.string.placement_result_title), onBack, isClose = true)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 32.dp)) {
            PlacementLevelCard(state.level, state.levelLabel, state.isLowestLevelAssigned)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(if (state.isResultSaved) R.string.assessment_path_ready else R.string.assessment_path_pending, state.level),
                style = DesignTypography.Body, color = DesignColor.Muted)
            Spacer(Modifier.height(16.dp))
            if (state.saveFailed) Text(stringResource(R.string.placement_save_failed), style = DesignTypography.Detail, color = DesignColor.Error)
            Text(stringResource(R.string.assessment_criteria_count, listOf(state.range, state.accuracy, state.fluency, state.interaction, state.coherence).count { it.hasPassed }),
                color = DesignColor.Text, style = DesignTypography.Title)
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
            PrimaryButton(
                text = stringResource(when {
                    state.isSavingResult -> R.string.placement_saving
                    state.saveFailed || !state.isResultSaved -> R.string.placement_retry_save
                    else -> R.string.assessment_open_path
                }),
                onClick = onStartTrainingPath,
                enabled = !state.isSavingResult,
                isLoading = state.isSavingResult
            )
        }
    }
}

@Composable
private fun PlacementLevelCard(level: String, levelLabel: String, isLowestLevelAssigned: Boolean) {
    Column(Modifier.fillMaxWidth().background(if (isLowestLevelAssigned) DesignColor.ResultWarningSurface else DesignColor.ResultSuccessSurface,
        RoundedCornerShape(24.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedMascot(if (isLowestLevelAssigned) R.raw.lottie_result_encourage else R.raw.lottie_result_pass,
            R.drawable.img_placement_mascot, Modifier.size(140.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.profile_estimated_level, level), style = DesignTypography.Heading, color = DesignColor.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        PlacementBadge(levelLabel)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(if (isLowestLevelAssigned) R.string.placement_a1_assigned_disclaimer else R.string.placement_result_disclaimer),
            style = DesignTypography.Detail, color = DesignColor.Muted, textAlign = TextAlign.Center)
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
