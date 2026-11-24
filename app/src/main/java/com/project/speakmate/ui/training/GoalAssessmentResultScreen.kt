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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.button.SecondaryButton
import com.project.speakmate.ui.components.header.BackAppBar
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.components.GoalAssessmentCriteriaContent
import com.project.speakmate.ui.training.components.GoalAssessmentFooter
import com.project.speakmate.ui.training.components.GoalAssessmentOutcome
import com.project.speakmate.ui.training.components.GoalAssessmentTextCard
import com.project.speakmate.ui.training.components.UnitResultCriterionCard
import com.project.speakmate.ui.training.components.UnitResultRequiredActions
import com.project.speakmate.ui.training.state.GoalAssessmentPresentation
import com.project.speakmate.ui.training.state.GoalAssessmentResultUiState

@Composable
fun GoalAssessmentResultScreen(
    state: GoalAssessmentResultUiState,
    onBack: () -> Unit,
    onReviewLessons: () -> Unit,
    onRetryAssessment: () -> Unit,
    onOpenNextGoal: () -> Unit,
    onViewCompletedPath: () -> Unit,
    onReturnToPath: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    onRetrySave: () -> Unit = {},
    onRetryProgress: () -> Unit = {}
) {
    val title = rememberGoalAssessmentResultTitle(state)
    val description = rememberGoalAssessmentResultDescription(state)
    val feedbackTitle = rememberGoalAssessmentResultFeedbackTitle(state)
    val feedback =
        if (state.presentation == GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE) description
        else {
            state.feedback.ifBlank { stringResource(R.string.goal_assessment_runtime_feedback) }
        }
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(stringResource(R.string.goal_assessment_header), onBack)
        if (state.presentation == GoalAssessmentPresentation.PROCESSING) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                GoalAssessmentOutcome(
                    presentation = state.presentation,
                    title = title,
                    description = description,
                    modifier = Modifier.verticalScroll(scrollState).padding(24.dp)
                )
            }
            Spacer(Modifier.height(88.dp))
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (state.presentation != GoalAssessmentPresentation.INTRODUCTION &&
                    state.presentation != GoalAssessmentPresentation.PROCESSING)
                    CaptionText(
                        text =
                            stringResource(
                                if (state.isSaved) R.string.practice_summary_saved_status
                                else R.string.practice_summary_pending_status
                            ),
                        style = DesignTypography.Label,
                        color = DesignColor.Primary
                    )
                GoalAssessmentOutcome(presentation = state.presentation, title = title, description = description)
                GoalAssessmentCriteriaContent(
                    isIntroduction = false,
                    isInsufficientEvidence = state.presentation == GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE
                )
                if (state.resultActions.isNotEmpty()) UnitResultRequiredActions(state.resultActions)
                state.criteria.forEach { criterion -> key(criterion.id) { UnitResultCriterionCard(criterion) } }
                if (state.hasLoadError) {
                    DetailText(
                        text = stringResource(R.string.assessment_progress_refresh_error),
                        color = DesignColor.Error
                    )
                    SecondaryButton(stringResource(R.string.content_reload), onRetryProgress)
                }
                GoalAssessmentTextCard(title = feedbackTitle, text = feedback, isHighlighted = true)
            }
            GoalAssessmentFooter(
                primaryText =
                    stringResource(
                        when {
                            state.isSaving -> R.string.placement_saving
                            state.isSaveFailed -> R.string.unit_result_retry_save
                            else ->
                                when (state.presentation) {
                                    GoalAssessmentPresentation.INTRODUCTION -> R.string.placement_start
                                    GoalAssessmentPresentation.NEXT_GOAL -> R.string.goal_assessment_open_next_goal
                                    GoalAssessmentPresentation.PATH_COMPLETED ->
                                        R.string.goal_assessment_view_completed_path
                                    GoalAssessmentPresentation.REPLAY_COMPLETED ->
                                        R.string.goal_assessment_return_to_path
                                    else -> R.string.goal_assessment_retry
                                }
                        }
                    ),
                secondaryText =
                    stringResource(
                        when (state.presentation) {
                            GoalAssessmentPresentation.INTRODUCTION -> R.string.goal_assessment_review_before_start
                            GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE -> R.string.goal_assessment_return_to_path
                            else -> R.string.unit_result_review
                        }
                    ),
                onPrimaryAction =
                    if (state.isSaveFailed) onRetrySave
                    else
                        when (state.presentation) {
                            GoalAssessmentPresentation.INTRODUCTION -> onRetryAssessment
                            GoalAssessmentPresentation.NEXT_GOAL -> onOpenNextGoal
                            GoalAssessmentPresentation.PATH_COMPLETED -> onViewCompletedPath
                            GoalAssessmentPresentation.REPLAY_COMPLETED -> onReturnToPath
                            else -> onRetryAssessment
                        },
                onSecondaryAction =
                    if (state.presentation == GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE) onReturnToPath
                    else onReviewLessons,
                isPrimaryEnabled = state.canContinue && !state.isSaving,
                isSecondaryEnabled = !state.isSaving
            )
        }
    }
}
