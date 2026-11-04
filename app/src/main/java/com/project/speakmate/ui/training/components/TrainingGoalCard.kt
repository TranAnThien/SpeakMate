package com.project.speakmate.ui.training.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.state.TrainingGoalUiState
import com.project.speakmate.ui.training.state.TrainingProgressStatus

@Composable
fun TrainingGoalCard(
    goal: TrainingGoalUiState,
    onToggle: () -> Unit,
    onOpenGoal: () -> Unit,
    onOpenUnit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DesignColor.Surface)
        .border(1.dp, if (goal.status == TrainingProgressStatus.CURRENT) DesignColor.CurrentBorder else DesignColor.Border,
            RoundedCornerShape(16.dp)).padding(17.dp)) {
        Column(Modifier.fillMaxWidth().clickable(onClick = onToggle), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(goal.label, style = DesignTypography.SectionLabel, color = DesignColor.Muted, modifier = Modifier.weight(1f))
                Text(stringResource(when (goal.status) {
                    TrainingProgressStatus.CURRENT -> R.string.training_current
                    TrainingProgressStatus.COMPLETED -> R.string.training_completed
                    TrainingProgressStatus.LOCKED -> R.string.training_locked
                }), style = DesignTypography.Badge,
                    color = if (goal.status == TrainingProgressStatus.CURRENT) DesignColor.Primary else DesignColor.Muted,
                    modifier = Modifier.background(if (goal.status == TrainingProgressStatus.CURRENT) DesignColor.IconBlue else DesignColor.NeutralSurface,
                        RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 5.dp))
                Image(painterResource(if (goal.isExpanded) R.drawable.ic_training_collapse else R.drawable.ic_training_expand), null, Modifier.size(20.dp, 24.dp))
            }
            Text(goal.title, style = DesignTypography.Title, color = DesignColor.Text)
            Text(goal.summary, style = DesignTypography.Detail, color = DesignColor.Muted)
            Text(stringResource(when {
                goal.isExpanded -> R.string.training_collapse_units
                goal.status == TrainingProgressStatus.LOCKED -> R.string.training_preview_units
                else -> R.string.training_view_units
            }), style = DesignTypography.Link, color = DesignColor.Primary)
        }
        if (goal.isExpanded) {
            Spacer(Modifier.height(8.dp))
            Text(goal.unitsHeading, style = DesignTypography.SectionLabel, color = DesignColor.Muted)
            Spacer(Modifier.height(8.dp))
            goal.units.forEachIndexed { index, unit ->
                key(unit.id) {
                    val onOpen = remember(unit.id, onOpenUnit) { { onOpenUnit(unit.id) } }
                    if (index > 0) HorizontalDivider(color = DesignColor.Border)
                    TrainingUnitRow(unit, onOpen)
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onOpenGoal), contentAlignment = Alignment.CenterStart) {
                Text(stringResource(R.string.training_goal_details_action), style = DesignTypography.Link, color = DesignColor.Primary)
            }
        }
    }
}
