package com.project.speakmate.ui.training

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.components.GoalAssessmentFooter
import com.project.speakmate.ui.training.components.GoalAssessmentOutcome
import com.project.speakmate.ui.training.components.GoalAssessmentRequiredActions
import com.project.speakmate.ui.training.components.GoalAssessmentTextCard
import com.project.speakmate.ui.training.components.PreparationHeader
import com.project.speakmate.ui.training.state.GoalAssessmentPresentation
import com.project.speakmate.ui.training.state.GoalAssessmentUiState

@Composable
fun GoalAssessmentScreen(
    state: GoalAssessmentUiState,
    onBack: () -> Unit,
    onStartAssessment: () -> Unit,
    onReviewLessons: () -> Unit,
    onRetryAssessment: () -> Unit,
    onOpenNextGoal: () -> Unit,
    onViewCompletedPath: () -> Unit,
    onReturnToPath: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        PreparationHeader(stringResource(R.string.goal_assessment_header), onBack)
        if (state.presentation == GoalAssessmentPresentation.PROCESSING) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                GoalAssessmentOutcome(state.presentation, state.title, state.description,
                    Modifier.verticalScroll(scrollState).padding(24.dp))
            }
            Spacer(Modifier.height(88.dp))
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)
                .padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                GoalAssessmentOutcome(state.presentation, state.title, state.description)
                if (state.presentation == GoalAssessmentPresentation.INTRODUCTION) {
                    GoalAssessmentRequiredActions(state.requiredActions)
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(when (state.presentation) {
                        GoalAssessmentPresentation.INTRODUCTION -> R.string.goal_assessment_rules_heading
                        GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE -> R.string.goal_assessment_unevaluated_heading
                        else -> R.string.goal_assessment_evaluated_heading
                    }), style = DesignTypography.Title, color = DesignColor.Text)
                    GoalAssessmentTextCard(
                        stringResource(if (state.presentation == GoalAssessmentPresentation.INTRODUCTION)
                            R.string.goal_assessment_calculation_title else R.string.goal_assessment_criteria_title),
                        stringResource(if (state.presentation == GoalAssessmentPresentation.INTRODUCTION)
                            R.string.goal_assessment_calculation_description else R.string.goal_assessment_criteria_description),
                        isHighlighted = false
                    )
                }
                GoalAssessmentTextCard(state.feedbackTitle, state.feedback, isHighlighted = true)
            }
            GoalAssessmentFooter(
                primaryText = stringResource(when (state.presentation) {
                    GoalAssessmentPresentation.INTRODUCTION -> R.string.placement_start
                    GoalAssessmentPresentation.NEXT_GOAL -> R.string.goal_assessment_open_next_goal
                    GoalAssessmentPresentation.PATH_COMPLETED -> R.string.goal_assessment_view_completed_path
                    GoalAssessmentPresentation.REPLAY_COMPLETED -> R.string.goal_assessment_return_to_path
                    else -> R.string.goal_assessment_retry
                }),
                secondaryText = stringResource(when (state.presentation) {
                    GoalAssessmentPresentation.INTRODUCTION -> R.string.goal_assessment_review_before_start
                    GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE -> R.string.goal_assessment_return_to_path
                    else -> R.string.unit_result_review
                }),
                onPrimaryAction = when (state.presentation) {
                    GoalAssessmentPresentation.INTRODUCTION -> onStartAssessment
                    GoalAssessmentPresentation.NEXT_GOAL -> onOpenNextGoal
                    GoalAssessmentPresentation.PATH_COMPLETED -> onViewCompletedPath
                    GoalAssessmentPresentation.REPLAY_COMPLETED -> onReturnToPath
                    else -> onRetryAssessment
                },
                onSecondaryAction = if (state.presentation == GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE)
                    onReturnToPath else onReviewLessons
            )
        }
    }
}
