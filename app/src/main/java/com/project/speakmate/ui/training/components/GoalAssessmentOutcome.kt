package com.project.speakmate.ui.training.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.training.state.GoalAssessmentPresentation

@Composable
fun GoalAssessmentOutcome(
    presentation: GoalAssessmentPresentation,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val surface = when (presentation) {
        GoalAssessmentPresentation.NEXT_GOAL, GoalAssessmentPresentation.PATH_COMPLETED, GoalAssessmentPresentation.REPLAY_COMPLETED -> DesignColor.ResultSuccessSurface
        GoalAssessmentPresentation.FAIL_CRITERIA, GoalAssessmentPresentation.FAIL_REQUIRED_ACTION -> DesignColor.ErrorSurface
        GoalAssessmentPresentation.PARTIAL, GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE -> DesignColor.ResultWarningSurface
        else -> DesignColor.Background
    }
    TrainingResultOutcome(title, description, surface,
        isProcessing = presentation == GoalAssessmentPresentation.PROCESSING,
        isPassed = presentation == GoalAssessmentPresentation.NEXT_GOAL || presentation == GoalAssessmentPresentation.PATH_COMPLETED ||
            presentation == GoalAssessmentPresentation.REPLAY_COMPLETED,
        modifier = modifier, isIntroduction = presentation == GoalAssessmentPresentation.INTRODUCTION)
}
