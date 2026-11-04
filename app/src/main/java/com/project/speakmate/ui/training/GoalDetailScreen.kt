package com.project.speakmate.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.AppBottomBar
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.StateIllustration
import com.project.speakmate.ui.training.components.GoalDetailHeader
import com.project.speakmate.ui.training.components.TrainingUnitRow
import com.project.speakmate.ui.training.state.GoalDetailUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun GoalDetailScreen(
    state: GoalDetailUiState,
    onBack: () -> Unit,
    onOpenUnit: (String) -> Unit,
    onPrepareCurrentUnit: () -> Unit,
    onOpenTrainingPath: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        GoalDetailHeader(onBack)
        Box(Modifier.weight(1f)) {
            if (state.hasUnits) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(state.title, style = DesignTypography.Heading, color = DesignColor.Primary,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.training_goal_outcome), style = DesignTypography.Badge, color = DesignColor.Primary)
                        Text(state.description, style = DesignTypography.Detail, color = DesignColor.Muted)
                    }
                    Text(stringResource(R.string.training_units_heading), style = DesignTypography.SectionLabel, color = DesignColor.Muted)
                    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp)) {
                        state.units.forEachIndexed { index, unit ->
                            key(unit.id) {
                                val onOpen = remember(unit.id, onOpenUnit) { { onOpenUnit(unit.id) } }
                                if (index > 0) HorizontalDivider(color = DesignColor.Border)
                                TrainingUnitRow(unit, onOpen)
                            }
                        }
                    }
                }
            } else GoalEmptyContent(onOpenTrainingPath)
        }
        if (state.canPrepareCurrentUnit && state.hasUnits) {
            Box(Modifier.fillMaxWidth().background(DesignColor.Surface).padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 12.dp)) {
                PrimaryButton(stringResource(R.string.training_prepare_current), onPrepareCurrentUnit)
            }
        }
        AppBottomBar(onOpenTrainingPath, onOpenPractice, onOpenHistory, onOpenProfile)
    }
}

@Composable
private fun GoalEmptyContent(onOpenTrainingPath: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val contentHeight = maxHeight
        Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = contentHeight)
            .padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StateIllustration(R.drawable.ic_training_empty_units)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.training_no_units_title), style = DesignTypography.Heading,
                    color = DesignColor.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.training_no_units_description), style = DesignTypography.Body,
                    color = DesignColor.Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                PrimaryButton(stringResource(R.string.training_back_to_path), onOpenTrainingPath, Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
