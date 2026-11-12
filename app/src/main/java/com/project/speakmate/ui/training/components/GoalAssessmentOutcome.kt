package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.state.GoalAssessmentPresentation

@Composable
fun GoalAssessmentOutcome(
    presentation: GoalAssessmentPresentation,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val icon: Int? = when (presentation) {
        GoalAssessmentPresentation.INTRODUCTION -> R.drawable.ic_training_assessment_coffee
        GoalAssessmentPresentation.PROCESSING -> null
        GoalAssessmentPresentation.PARTIAL -> R.drawable.ic_training_result_partial
        GoalAssessmentPresentation.FAIL_CRITERIA, GoalAssessmentPresentation.FAIL_REQUIRED_ACTION -> R.drawable.ic_call_close
        GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE -> R.drawable.ic_placement_microphone_off
        else -> R.drawable.ic_training_result_check
    }
    val foreground = when (presentation) {
        GoalAssessmentPresentation.INTRODUCTION, GoalAssessmentPresentation.PROCESSING -> DesignColor.Primary
        GoalAssessmentPresentation.PARTIAL, GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE -> DesignColor.ResultWarning
        GoalAssessmentPresentation.FAIL_CRITERIA, GoalAssessmentPresentation.FAIL_REQUIRED_ACTION -> DesignColor.Error
        else -> DesignColor.Success
    }
    val background = when (presentation) {
        GoalAssessmentPresentation.INTRODUCTION -> DesignColor.SecondaryButton
        GoalAssessmentPresentation.PROCESSING -> DesignColor.IconBlue
        GoalAssessmentPresentation.PARTIAL, GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE -> DesignColor.ResultWarningSurface
        GoalAssessmentPresentation.FAIL_CRITERIA, GoalAssessmentPresentation.FAIL_REQUIRED_ACTION -> DesignColor.ErrorSurface
        else -> DesignColor.ResultSuccessSurface
    }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
            if (presentation == GoalAssessmentPresentation.PROCESSING) {
                SpeakMateLoadingIndicator(size = 32.dp)
            } else {
                Icon(painterResource(requireNotNull(icon)), null, Modifier.size(32.dp), tint = foreground)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = DesignTypography.Heading, color = DesignColor.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(description, style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center)
    }
}
