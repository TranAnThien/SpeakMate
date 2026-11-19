package com.project.speakmate.ui.placement

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.project.speakmate.R
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PlacementResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.ui.call.CallRoute
import com.project.speakmate.ui.placement.components.PlacementResumeDialog
import com.project.speakmate.ui.placement.state.PlacementEffect
import com.project.speakmate.ui.placement.state.PlacementNextTask
import com.project.speakmate.ui.placement.state.PlacementPhase
import com.project.speakmate.ui.placement.state.PlacementResultUiState
import com.project.speakmate.ui.placement.state.PlacementSkillUiState
import com.project.speakmate.ui.placement.state.PlacementStatus
import com.project.speakmate.ui.placement.state.PlacementStatusUiState
import com.project.speakmate.utils.extension.default

@Composable
fun PlacementRoute(
    viewModel: PlacementViewModel,
    isCallDestination: Boolean,
    onChangeCallDestination: (Boolean) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val goHome by rememberUpdatedState(onHome)
    val changeCallDestination by rememberUpdatedState(onChangeCallDestination)
    val shouldShowCall = state.phase == PlacementPhase.CALLING
    LaunchedEffect(shouldShowCall, isCallDestination, lifecycleOwner) {
        if (shouldShowCall != isCallDestination) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                changeCallDestination(shouldShowCall)
            }
        }
    }
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    PlacementEffect.NavigateHome -> goHome()
                }
            }
        }
    }
    BackHandler(enabled = !shouldShowCall || !isCallDestination) {
        viewModel.exit()
    }

    if (shouldShowCall != isCallDestination) {
        PlacementStatusScreen(PlacementStatusUiState(), viewModel::exit, viewModel::continueAssessment,
            viewModel::retry, viewModel::exit, viewModel::exit, modifier)
        return
    }

    when (state.phase) {
        PlacementPhase.INTRODUCTION -> PlacementIntroductionScreen(viewModel::exit, viewModel::start, viewModel::skip, modifier, state.saveFailed)
        PlacementPhase.RESUME_PROMPT -> Box(modifier) {
            PlacementIntroductionScreen(viewModel::exit, viewModel::start, viewModel::skip, Modifier.clearAndSetSemantics {})
            PlacementResumeDialog(viewModel::resume, viewModel::restart)
        }
        PlacementPhase.CALLING -> {
            val task = state.currentTask
            val scenario = state.scenario
            if (task != null && scenario != null) {
                CallRoute(
                    scenario = scenario,
                    activityType = CallActivityType.INITIAL_PLACEMENT,
                    onExit = viewModel::exit,
                    modifier = modifier,
                    communicativeGoal = task.communicativeGoal,
                    onCallFinished = viewModel::submitCallResult
                )
            }
        }
        PlacementPhase.NEXT_TASK -> when (state.nextTask) {
            PlacementNextTask.SAME_LEVEL -> PlacementAdditionalTaskScreen(viewModel::exit, viewModel::continueAssessment, modifier)
            PlacementNextTask.HIGHER_LEVEL, PlacementNextTask.LOWER_LEVEL, null -> PlacementStatusScreen(
                PlacementStatusUiState(PlacementStatus.CONFIRMATION, state.hasReachedAudioLimit),
                viewModel::exit, viewModel::continueAssessment, viewModel::retry, viewModel::exit, viewModel::exit, modifier
            )
        }
        PlacementPhase.GRADING, PlacementPhase.INSUFFICIENT_EVIDENCE, PlacementPhase.UNDETERMINED -> {
            val status = when (state.phase) {
                PlacementPhase.GRADING -> PlacementStatus.PROCESSING
                PlacementPhase.INSUFFICIENT_EVIDENCE -> PlacementStatus.INSUFFICIENT_EVIDENCE
                else -> PlacementStatus.UNDETERMINED
            }
            PlacementStatusScreen(PlacementStatusUiState(status, state.hasReachedAudioLimit), viewModel::exit,
                viewModel::continueAssessment, viewModel::retry, viewModel::exit, viewModel::exit, modifier)
        }
        PlacementPhase.RESULT -> {
            val result = state.result
            if (result != null) PlacementCompletedResult(
                result, state.isSavingResult, state.saveFailed, viewModel::exit, viewModel::confirmResultSaved, modifier
            )
        }
        PlacementPhase.LOADING -> PlacementStatusScreen(PlacementStatusUiState(), viewModel::exit, viewModel::continueAssessment,
            viewModel::retry, viewModel::exit, viewModel::exit, modifier)
    }
}

@Composable
private fun PlacementCompletedResult(
    result: PlacementResult,
    isSavingResult: Boolean,
    saveFailed: Boolean,
    onBack: () -> Unit,
    onSaveOrHome: () -> Unit,
    modifier: Modifier
) {
    val levelLabel = when (result.level) {
        SpeakingLevel.A1 -> stringResource(R.string.placement_a1_label)
        SpeakingLevel.A2 -> stringResource(R.string.placement_a2_label)
        SpeakingLevel.B1 -> stringResource(R.string.placement_b1_label)
        SpeakingLevel.B2 -> stringResource(R.string.placement_b2_label)
        SpeakingLevel.NOT_ASSESSED -> ""
    }
    val passedFeedback = stringResource(R.string.placement_criterion_met_feedback)
    val failedFeedback = stringResource(R.string.placement_criterion_unmet_feedback)
    val state = remember(result, levelLabel, isSavingResult, saveFailed, passedFeedback, failedFeedback) {
        PlacementResultUiState(
            level = result.level.name,
            levelLabel = levelLabel,
            range = PlacementSkillUiState(
                result.feedback.range.takeIf(String::isNotBlank).default(if (result.speakingProfile.rangeMet) passedFeedback else failedFeedback),
                result.speakingProfile.rangeMet
            ),
            accuracy = PlacementSkillUiState(
                result.feedback.accuracy.takeIf(String::isNotBlank).default(if (result.speakingProfile.accuracyMet) passedFeedback else failedFeedback),
                result.speakingProfile.accuracyMet
            ),
            fluency = PlacementSkillUiState(
                result.feedback.fluency.takeIf(String::isNotBlank).default(if (result.speakingProfile.fluencyMet) passedFeedback else failedFeedback),
                result.speakingProfile.fluencyMet
            ),
            interaction = PlacementSkillUiState(
                result.feedback.interaction.takeIf(String::isNotBlank).default(if (result.speakingProfile.interactionMet) passedFeedback else failedFeedback),
                result.speakingProfile.interactionMet
            ),
            coherence = PlacementSkillUiState(
                result.feedback.coherence.takeIf(String::isNotBlank).default(if (result.speakingProfile.coherenceMet) passedFeedback else failedFeedback),
                result.speakingProfile.coherenceMet
            ),
            isLowestLevelAssigned = result.level == SpeakingLevel.A1 &&
                result.taskResult in listOf(AssessmentResult.PARTIAL, AssessmentResult.FAIL),
            isSavingResult = isSavingResult,
            saveFailed = saveFailed
        )
    }
    PlacementResultScreen(state, onBack, onSaveOrHome, modifier)
}
