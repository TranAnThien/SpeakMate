package com.project.speakmate.ui.placement

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.R
import com.project.speakmate.utils.extension.default
import com.project.speakmate.domain.rules.PlacementRules
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.ReassessmentStep
import com.project.speakmate.domain.model.ReassessmentTransition
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.ui.call.CallRoute
import com.project.speakmate.ui.placement.state.PlacementStatus
import com.project.speakmate.ui.placement.state.PlacementStatusFlow
import com.project.speakmate.ui.placement.state.PlacementStatusUiState
import com.project.speakmate.ui.placement.state.PromotionIntroductionUiState
import com.project.speakmate.ui.placement.state.PromotionResultPresentation
import com.project.speakmate.ui.placement.state.PromotionResultUiState
import com.project.speakmate.ui.placement.state.PromotionStepUiState
import com.project.speakmate.ui.placement.state.ReassessmentPhase
import com.project.speakmate.ui.placement.state.ReassessmentUiState

@Composable
fun ReassessmentRoute(
    viewModel: ReassessmentViewModel,
    isCallDestination: Boolean,
    onChangeCallDestination: (Boolean) -> Unit,
    onReturnToPath: () -> Unit,
    onOpenPromotedPath: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val changeCallDestination by rememberUpdatedState(onChangeCallDestination)
    val shouldShowCall = state.phase == ReassessmentPhase.CALLING
    LaunchedEffect(shouldShowCall, isCallDestination, lifecycleOwner) {
        if (shouldShowCall != isCallDestination) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                changeCallDestination(shouldShowCall)
            }
        }
    }
    val exitAssessment: () -> Unit = {
        viewModel.exit()
        when {
            state.currentLevel == SpeakingLevel.NOT_ASSESSED -> onHome()
            state.isSaved -> onOpenPromotedPath()
            else -> onReturnToPath()
        }
    }
    BackHandler(enabled = !shouldShowCall || !isCallDestination, onBack = exitAssessment)
    if (shouldShowCall != isCallDestination || state.phase in listOf(ReassessmentPhase.LOADING, ReassessmentPhase.CLOSED)) {
        PlacementStatusScreen(
            PlacementStatusUiState(flow = PlacementStatusFlow.REASSESSMENT),
            exitAssessment, viewModel::continueAssessment, viewModel::retryTask, exitAssessment, exitAssessment, modifier
        )
        return
    }
    when (state.phase) {
        ReassessmentPhase.INTRODUCTION -> PromotionIntroductionScreen(
            state = PromotionIntroductionUiState(
                title = stringResource(R.string.promotion_runtime_introduction_title, state.currentLevel.name, state.nextLevel.name),
                description = stringResource(R.string.promotion_runtime_introduction_description, state.currentLevel.name, state.nextLevel.name),
                steps = ReassessmentStep.entries.mapIndexed { index, step ->
                    PromotionStepUiState(
                        id = step.name,
                        number = index + 1,
                        text = stringResource(when (step) {
                            ReassessmentStep.CURRENT_CONFIRMATION -> R.string.promotion_runtime_current_confirmation
                            ReassessmentStep.NEXT_PROBE -> R.string.promotion_runtime_next_probe
                            ReassessmentStep.NEXT_CONFIRMATION -> R.string.promotion_runtime_next_confirmation
                        }, if (step == ReassessmentStep.CURRENT_CONFIRMATION) state.currentLevel.name else state.nextLevel.name)
                    )
                },
                conditionTitle = stringResource(R.string.promotion_runtime_condition_title, state.nextLevel.name),
                conditionDescription = stringResource(R.string.promotion_runtime_condition_description, state.currentLevel.name, state.nextLevel.name),
                note = stringResource(R.string.promotion_introduction_note)
            ),
            onBack = exitAssessment,
            onStartAssessment = viewModel::start,
            onReturnToPath = exitAssessment,
            modifier = modifier
        )
        ReassessmentPhase.CALLING -> {
            val task = state.assessment?.currentTask
            val scenario = state.scenario
            if (task != null && scenario != null) {
                CallRoute(
                    scenario = scenario,
                    activityType = CallActivityType.REASSESSMENT,
                    onExit = exitAssessment,
                    modifier = modifier,
                    activityContext = CallActivityContext.Assessment(scenario, CallActivityType.REASSESSMENT, task.communicativeGoal),
                    onCallFinished = viewModel::submitCallResult
                )
            }
        }
        ReassessmentPhase.GRADING, ReassessmentPhase.NEXT_TASK, ReassessmentPhase.ERROR -> PlacementStatusScreen(
            state = PlacementStatusUiState(
                status = when (state.phase) {
                    ReassessmentPhase.NEXT_TASK -> PlacementStatus.CONFIRMATION
                    ReassessmentPhase.ERROR -> PlacementStatus.UNDETERMINED
                    else -> PlacementStatus.PROCESSING
                },
                isTimeLimitReached = state.hasReachedAudioLimit,
                flow = PlacementStatusFlow.REASSESSMENT,
                reassessmentStep = state.assessment?.step,
                assessmentLevel = state.assessment?.currentTask?.targetLevel.default(state.currentLevel)
            ),
            onBack = exitAssessment,
            onContinueAssessment = viewModel::continueAssessment,
            onRetryAssessment = viewModel::retryTask,
            onDeferAssessment = exitAssessment,
            onGoHome = if (state.phase == ReassessmentPhase.ERROR) viewModel::retryLoad else exitAssessment,
            modifier = modifier
        )
        ReassessmentPhase.RESULT -> ReassessmentCompletedResult(state, viewModel::retryTask, viewModel::retrySave, exitAssessment, modifier)
        ReassessmentPhase.LOADING, ReassessmentPhase.CLOSED -> Unit
    }
}

@Composable
private fun ReassessmentCompletedResult(
    state: ReassessmentUiState,
    onRetry: () -> Unit,
    onRetrySave: () -> Unit,
    onReturnToPath: () -> Unit,
    modifier: Modifier
) {
    val transition = state.result
    val presentation = when (transition) {
        is ReassessmentTransition.Promoted -> PromotionResultPresentation.PROMOTED
        is ReassessmentTransition.NotPromoted -> PromotionResultPresentation.MAINTAINED
        else -> PromotionResultPresentation.INSUFFICIENT_EVIDENCE
    }
    val title = stringResource(when {
        transition is ReassessmentTransition.Promoted && !state.isSaved -> R.string.promotion_runtime_save_title
        transition is ReassessmentTransition.Promoted -> R.string.promotion_runtime_promoted_title
        transition is ReassessmentTransition.NotPromoted -> R.string.promotion_runtime_maintained_title
        transition is ReassessmentTransition.NotEligible -> R.string.promotion_runtime_not_eligible
        else -> R.string.promotion_insufficient_compact_title
    }, state.nextLevel.name)
    val description = stringResource(when (transition) {
        is ReassessmentTransition.Promoted -> if (state.isSaved) R.string.promotion_runtime_promoted_description else R.string.promotion_runtime_qualified_description
        is ReassessmentTransition.NotPromoted -> R.string.promotion_runtime_maintained_description
        is ReassessmentTransition.RetryTask -> R.string.promotion_runtime_insufficient_description
        is ReassessmentTransition.Stopped -> if (transition.reason == ReassessmentTransition.StopReason.TECHNICAL_RETRY_LIMIT) {
            R.string.promotion_runtime_retry_limit
        } else R.string.promotion_runtime_unavailable_description
        else -> R.string.promotion_runtime_not_eligible_description
    }, state.currentLevel.name, state.nextLevel.name, PlacementRules.MAX_TECHNICAL_RETRIES)
    val saveFailure = stringResource(R.string.unit_result_save_failed)
    PromotionResultScreen(
        state = PromotionResultUiState(
            presentation = presentation,
            title = title,
            description = if (state.isSaveFailed) stringResource(R.string.promotion_runtime_save_failed, description, saveFailure) else description,
            currentLevelLabel = state.currentLevel.name,
            nextLevelLabel = state.nextLevel.name,
            canRetryAssessment = transition is ReassessmentTransition.RetryTask,
            isSaving = state.isSaving,
            isSaveFailed = state.isSaveFailed,
            isResultSaved = state.isSaved
        ),
        onBack = onReturnToPath,
        onOpenPromotedPath = onReturnToPath,
        onReturnToCurrentPath = onReturnToPath,
        onRetryAssessment = onRetry,
        modifier = modifier,
        onRetrySave = onRetrySave
    )
}
