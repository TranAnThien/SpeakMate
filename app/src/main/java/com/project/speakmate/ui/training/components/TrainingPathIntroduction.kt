package com.project.speakmate.ui.training.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.HeadingText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun TrainingPathIntroduction(
    level: String,
    isPathCompleted: Boolean,
    completedActivityCount: Int,
    totalActivityCount: Int,
    currentActivityTitle: String,
    canContinue: Boolean,
    isAssessmentReady: Boolean,
    isRefreshing: Boolean,
    isStale: Boolean,
    isOpeningActivity: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SpeakMateCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16.dp,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CaptionText(text = stringResource(R.string.training_your_path), style = DesignTypography.Badge)
            HeadingText(
                text =
                    stringResource(
                        if (isPathCompleted && level == SpeakingLevel.B2.name)
                            R.string.training_b2_completed_title
                        else if (isPathCompleted) R.string.training_path_completed_title
                        else R.string.training_progress_title,
                        level,
                        completedActivityCount,
                        totalActivityCount
                    ),
                style = DesignTypography.AppBar
            )
            TrainingPill(
                text = stringResource(R.string.training_speaking_level, level),
                color = DesignColor.Primary,
                backgroundColor = DesignColor.PrimaryContainer
            )
            LinearProgressIndicator(
                progress = {
                    if (totalActivityCount > 0)
                        completedActivityCount.toFloat() / totalActivityCount
                    else 0f
                },
                modifier = Modifier.fillMaxWidth(),
                color = DesignColor.Primary,
                trackColor = DesignColor.SurfaceContainerHighest
            )
            CaptionText(text = stringResource(R.string.training_progress_explanation))
            if (currentActivityTitle.isNotBlank()) DetailText(text = currentActivityTitle)
            if (canContinue)
                PrimaryButton(
                    text =
                        stringResource(
                            if (isAssessmentReady) R.string.goal_assessment_header
                            else R.string.home_continue_path
                        ),
                    onClick = onContinue,
                    enabled = !isRefreshing && !isStale,
                    isLoading = isOpeningActivity
                )
        }
        TitleText(text = stringResource(R.string.training_goals_heading), style = DesignTypography.SectionLabel)
    }
}

