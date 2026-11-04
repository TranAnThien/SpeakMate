package com.project.speakmate.ui.training.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.state.TrainingProgressStatus
import com.project.speakmate.ui.training.state.TrainingUnitUiState

@Composable
fun TrainingUnitRow(
    unit: TrainingUnitUiState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrent = unit.status == TrainingProgressStatus.CURRENT
    val statusColor = when (unit.status) {
        TrainingProgressStatus.COMPLETED -> DesignColor.Success
        TrainingProgressStatus.CURRENT -> DesignColor.Primary
        TrainingProgressStatus.LOCKED -> DesignColor.Muted
    }
    val iconRes = when {
        unit.isAssessment -> R.drawable.ic_training_flag
        unit.status == TrainingProgressStatus.COMPLETED -> R.drawable.ic_placement_passed
        isCurrent -> R.drawable.ic_training_play
        else -> R.drawable.ic_training_lock
    }
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(if (isCurrent) DesignColor.PaleBlue else DesignColor.Surface)
        .clickable(enabled = unit.canOpen, onClick = onOpen)
        .padding(horizontal = if (isCurrent) 8.dp else 0.dp, vertical = if (isCurrent) 8.dp else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).background(when (unit.status) {
            TrainingProgressStatus.COMPLETED -> DesignColor.SuccessSurface
            TrainingProgressStatus.CURRENT -> DesignColor.IconBlue
            TrainingProgressStatus.LOCKED -> DesignColor.NeutralSurface
        }, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Image(painterResource(iconRes), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(statusColor))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(unit.title, style = DesignTypography.Title, color = DesignColor.Text)
            Text(stringResource(when {
                unit.status == TrainingProgressStatus.COMPLETED -> R.string.training_completed_replay
                isCurrent -> R.string.training_current
                unit.isAssessment -> R.string.training_assessment_locked
                else -> R.string.training_locked
            }), style = if (isCurrent) DesignTypography.Badge else DesignTypography.Small, color = statusColor)
            if (unit.description.isNotBlank()) {
                Text(unit.description, style = DesignTypography.Detail, color = DesignColor.Muted)
            }
        }
    }
}
