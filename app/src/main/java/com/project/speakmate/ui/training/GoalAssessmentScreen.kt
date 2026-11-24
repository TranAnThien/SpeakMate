package com.project.speakmate.ui.training

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.header.BackAppBar
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.training.components.GoalAssessmentCriteriaContent
import com.project.speakmate.ui.training.components.GoalAssessmentFooter
import com.project.speakmate.ui.training.components.GoalAssessmentOutcome
import com.project.speakmate.ui.training.components.GoalAssessmentRequiredActions
import com.project.speakmate.ui.training.components.GoalAssessmentTextCard
import com.project.speakmate.ui.training.state.GoalAssessmentPresentation
import com.project.speakmate.ui.training.state.GoalAssessmentUiState

@Composable
fun GoalAssessmentScreen(
    state: GoalAssessmentUiState,
    onBack: () -> Unit,
    onStartAssessment: () -> Unit,
    onReviewLessons: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    Column(modifier = modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(title = stringResource(R.string.goal_assessment_header), onBack = onBack)
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            GoalAssessmentOutcome(
                presentation = GoalAssessmentPresentation.INTRODUCTION,
                title = state.title,
                description = stringResource(R.string.goal_assessment_runtime_introduction, state.description)
            )
            GoalAssessmentRequiredActions(state.requiredActions)
            GoalAssessmentCriteriaContent(isIntroduction = true, isInsufficientEvidence = false)
            GoalAssessmentTextCard(
                title = stringResource(R.string.goal_assessment_introduction_feedback_title),
                text = stringResource(R.string.goal_assessment_introduction_feedback),
                isHighlighted = true
            )
        }
        GoalAssessmentFooter(
            primaryText = stringResource(R.string.placement_start),
            secondaryText = stringResource(R.string.goal_assessment_review_before_start),
            onPrimaryAction = onStartAssessment,
            onSecondaryAction = onReviewLessons,
            isPrimaryEnabled = !state.isStarting
        )
    }
}
