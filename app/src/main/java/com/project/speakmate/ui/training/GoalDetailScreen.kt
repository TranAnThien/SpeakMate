package com.project.speakmate.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.SecondaryButton
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.components.TrainingUnitRow
import com.project.speakmate.ui.training.state.GoalDetailUiState
import com.project.speakmate.ui.training.state.TrainingActivityDestination
import com.project.speakmate.ui.training.state.TrainingProgressStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    state: GoalDetailUiState,
    onBack: () -> Unit,
    onOpenUnit: (TrainingActivityDestination) -> Unit,
    onPrepareCurrentUnit: () -> Unit,
    onReturnToPath: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(onDismissRequest = onBack, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DesignColor.SurfaceContainerLow,
        contentWindowInsets = { WindowInsets.safeContent.union(WindowInsets(top = 32.dp, bottom = 24.dp)) }) {
        Column(modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(state.title.ifBlank { stringResource(R.string.training_goal_detail_title) }, style = DesignTypography.AppBar, color = DesignColor.Text)
                        if (!state.isConfigured || !state.hasUnits) {
                            Text(stringResource(R.string.training_content_unconfigured), style = DesignTypography.SectionTitle, color = DesignColor.Text)
                            Text(stringResource(R.string.training_goal_unconfigured_description), style = DesignTypography.Body, color = DesignColor.Muted)
                        } else {
                            Column(Modifier.fillMaxWidth().background(DesignColor.SurfaceContainer, RoundedCornerShape(16.dp)).padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.training_goal_outcome), style = DesignTypography.Title, color = DesignColor.Text)
                                Text(state.description, style = DesignTypography.Detail, color = DesignColor.Muted)
                            }
                            if (state.isStale) Text(stringResource(R.string.training_stale_description), style = DesignTypography.Detail, color = DesignColor.ResultWarning)
                            if (state.isLocked) Text(stringResource(R.string.training_locked_explanation), style = DesignTypography.Detail, color = DesignColor.Muted)
                        }
                    }
                }
                if (state.isConfigured && state.hasUnits) {
                    items(state.units, key = { it.isAssessment to it.id }, contentType = { it.isAssessment }) { unit ->
                        val onOpen = remember(unit.destination, onOpenUnit) { { unit.destination?.let(onOpenUnit); Unit } }
                        TrainingUnitRow(unit, onOpen, Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.canPrepareCurrentUnit && state.isConfigured) {
                    PrimaryButton(stringResource(if (state.units.any { it.isAssessment && it.status == TrainingProgressStatus.CURRENT })
                        R.string.goal_assessment_header else R.string.training_prepare_current), onPrepareCurrentUnit)
                }
                SecondaryButton(stringResource(R.string.content_close), if (state.isConfigured) onBack else onReturnToPath)
            }
        }
    }
}
