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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.state.TrainingActivityDestination
import com.project.speakmate.ui.training.state.TrainingGoalUiState
import com.project.speakmate.ui.training.state.TrainingProgressStatus

@Composable
fun TrainingGoalCard(
    goal: TrainingGoalUiState,
    onToggle: () -> Unit,
    onOpenGoal: () -> Unit,
    onOpenUnit: (TrainingActivityDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val expansionDescription =
        stringResource(if (goal.isExpanded) R.string.accessibility_expanded else R.string.accessibility_collapsed)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DesignColor.Surface)
            .border(
                width = 1.dp,
                color =
                    if (goal.status == TrainingProgressStatus.CURRENT) DesignColor.CurrentBorder
                    else DesignColor.Border,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(17.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .semantics { stateDescription = expansionDescription }
                .clickable(role = Role.Button, onClick = onToggle),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TitleText(
                    text = stringResource(R.string.training_goal_number, goal.order + 1),
                    style = DesignTypography.SectionLabel,
                    color = DesignColor.Muted,
                    modifier = Modifier.weight(1f)
                )
                CaptionText(
                    text =
                        stringResource(
                            when (goal.status) {
                                TrainingProgressStatus.CURRENT -> R.string.training_current
                                TrainingProgressStatus.COMPLETED -> R.string.training_completed
                                TrainingProgressStatus.LOCKED -> R.string.training_locked
                            }
                        ),
                    style = DesignTypography.Badge,
                    color =
                        if (goal.status == TrainingProgressStatus.CURRENT) DesignColor.Primary
                        else DesignColor.Muted,
                    modifier =
                        Modifier.background(
                            if (goal.status == TrainingProgressStatus.CURRENT)
                                    DesignColor.PrimaryContainer
                                else DesignColor.NeutralSurface,
                            RoundedCornerShape(12.dp)
                        )
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                )
                Image(
                    painter =
                        painterResource(
                            if (goal.isExpanded) R.drawable.ic_training_collapse
                            else R.drawable.ic_training_expand
                        ),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp, 24.dp)
                )
            }
            TitleText(text = goal.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
            DetailText(text = goal.summary)
            DetailText(
                text =
                    stringResource(
                        when {
                            goal.isExpanded -> R.string.training_collapse_units
                            goal.status == TrainingProgressStatus.LOCKED -> R.string.training_view_locked_units
                            else -> R.string.training_view_units
                        }
                    ),
                style = DesignTypography.Link,
                color = DesignColor.Primary
            )
        }
        if (goal.isExpanded) {
            Spacer(Modifier.height(8.dp))
            TitleText(
                text = stringResource(R.string.training_units_heading),
                style = DesignTypography.SectionLabel,
                color = DesignColor.Muted
            )
            Spacer(Modifier.height(8.dp))
            goal.units.forEachIndexed { index, unit ->
                key(unit.isAssessment, unit.id) {
                    val onOpen =
                        remember(unit.destination, onOpenUnit) {
                            {
                                unit.destination?.let(onOpenUnit)
                                Unit
                            }
                        }
                    if (index > 0) HorizontalDivider(color = DesignColor.Border)
                    TrainingUnitRow(unit, onOpen)
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onOpenGoal),
                contentAlignment = Alignment.CenterStart
            ) {
                DetailText(
                    text = stringResource(R.string.training_goal_details_action),
                    style = DesignTypography.Link,
                    color = DesignColor.Primary
                )
            }
        }
    }
}
