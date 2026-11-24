package com.project.speakmate.ui.placement

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.project.speakmate.ui.components.assessment.rememberSpeakingLevelLabel
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.card.AssessmentCriterionCard
import com.project.speakmate.ui.components.header.BackAppBar
import com.project.speakmate.ui.components.lottie.AnimatedMascot
import com.project.speakmate.ui.components.lottie.MascotAnimation
import com.project.speakmate.ui.components.state.StatusContent
import com.project.speakmate.ui.components.text.BodyText
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.TitleText
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
        BackAppBar(title = stringResource(R.string.placement_result_title), onBack = onBack, isClose = true)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp)
        ) {
            PlacementLevelCard(
                level = state.level.name,
                levelLabel = rememberSpeakingLevelLabel(state.level),
                isLowestLevelAssigned = state.isLowestLevelAssigned
            )
            Spacer(Modifier.height(16.dp))
            BodyText(
                text =
                    stringResource(
                        if (state.isResultSaved) R.string.assessment_path_ready
                        else R.string.assessment_path_pending,
                        state.level.name
                    ),
                color = DesignColor.Muted
            )
            Spacer(Modifier.height(16.dp))
            if (state.saveFailed)
                DetailText(text = stringResource(R.string.placement_save_failed), color = DesignColor.Error)
            TitleText(
                text =
                    stringResource(
                        R.string.assessment_criteria_count,
                        listOf(state.range, state.accuracy, state.fluency, state.interaction, state.coherence)
                            .count { it.hasPassed }
                    )
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlacementSkillCard(
                    title = stringResource(R.string.placement_range),
                    iconRes = R.drawable.ic_placement_range,
                    state = state.range
                )
                PlacementSkillCard(
                    title = stringResource(R.string.placement_accuracy),
                    iconRes = R.drawable.ic_placement_accuracy,
                    state = state.accuracy
                )
                PlacementSkillCard(
                    title = stringResource(R.string.placement_fluency),
                    iconRes = R.drawable.ic_placement_fluency,
                    state = state.fluency
                )
                PlacementSkillCard(
                    title = stringResource(R.string.placement_interaction),
                    iconRes = R.drawable.ic_placement_interaction,
                    state = state.interaction
                )
                PlacementSkillCard(
                    title = stringResource(R.string.placement_coherence),
                    iconRes = R.drawable.ic_placement_coherence,
                    state = state.coherence
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .background(DesignColor.Surface)
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp)
        ) {
            PrimaryButton(
                text =
                    stringResource(
                        when {
                            state.isSavingResult -> R.string.placement_saving
                            state.saveFailed || !state.isResultSaved -> R.string.placement_retry_save
                            else -> R.string.assessment_open_path
                        }
                    ),
                onClick = onStartTrainingPath,
                enabled = !state.isSavingResult,
                isLoading = state.isSavingResult
            )
        }
    }
}

@Composable
private fun PlacementLevelCard(level: String, levelLabel: String, isLowestLevelAssigned: Boolean) {
    StatusContent(
        title = stringResource(R.string.profile_estimated_level, level),
        titleStyle = DesignTypography.Heading,
        titleColor = DesignColor.Text,
        titleTextAlign = TextAlign.Center,
        description =
            stringResource(
                if (isLowestLevelAssigned) R.string.placement_a1_assigned_disclaimer
                else R.string.placement_result_disclaimer
            ),
        descriptionStyle = DesignTypography.Detail,
        descriptionColor = DesignColor.Muted,
        descriptionTextAlign = TextAlign.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    if (isLowestLevelAssigned) DesignColor.ResultWarningSurface else DesignColor.SuccessContainer,
                    RoundedCornerShape(24.dp)
                )
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        illustration = {
            AnimatedMascot(
                animation =
                    if (isLowestLevelAssigned) MascotAnimation.RESULT_ENCOURAGE else MascotAnimation.RESULT_PASS,
                modifier = Modifier.size(140.dp)
            )
            Spacer(Modifier.height(16.dp))
        },
        titleContent = {
            Spacer(Modifier.height(8.dp))
            PlacementBadge(levelLabel)
            Spacer(Modifier.height(8.dp))
        }
    )
}

@Composable
private fun PlacementSkillCard(title: String, @DrawableRes iconRes: Int, state: PlacementSkillUiState) {
    AssessmentCriterionCard(
        title = title,
        description =
            state.feedback.ifBlank {
                stringResource(
                    if (state.hasPassed) R.string.placement_criterion_met_feedback
                    else R.string.placement_criterion_unmet_feedback
                )
            },
        statusLabel = "",
        statusColor = if (state.hasPassed) DesignColor.Success else DesignColor.Error,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 12.dp,
        isStacked = false,
        icon = {
            Box(
                Modifier.size(32.dp)
                    .background(
                        if (state.hasPassed) DesignColor.PrimaryContainer else DesignColor.ErrorSurface,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        status = {
            Column(
                modifier = Modifier.width(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Image(
                    painter =
                        painterResource(
                            if (state.hasPassed) R.drawable.ic_placement_passed
                            else R.drawable.ic_placement_failed
                        ),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                CaptionText(
                    text =
                        stringResource(
                            if (state.hasPassed) R.string.placement_passed else R.string.placement_not_passed
                        ),
                    style = DesignTypography.Badge,
                    color = if (state.hasPassed) DesignColor.Success else DesignColor.Error
                )
            }
        }
    )
}
