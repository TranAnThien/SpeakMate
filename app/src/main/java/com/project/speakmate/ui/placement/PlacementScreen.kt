package com.project.speakmate.ui.placement

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.project.speakmate.R
import com.project.speakmate.domain.model.PlacementResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.ui.components.state.AssessmentPendingContent
import com.project.speakmate.ui.placement.components.AssessmentRecoveryContent
import com.project.speakmate.ui.placement.components.PlacementResumeDialog
import com.project.speakmate.ui.placement.state.PlacementPhase
import com.project.speakmate.ui.placement.state.PlacementStatus
import com.project.speakmate.ui.placement.state.PlacementStatusUiState
import com.project.speakmate.ui.placement.state.PlacementUiState
import com.project.speakmate.utils.extension.default

@Composable
fun PlacementScreen(
    state: PlacementUiState,
    isCallDestination: Boolean,
    onExit: () -> Unit,
    onContinueAssessment: () -> Unit,
    onRetry: () -> Unit,
    onRetryLoad: () -> Unit,
    onRetryGrading: () -> Unit,
    onRestartExpired: () -> Unit,
    onStart: () -> Unit,
    onSkip: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onConfirmResultSaved: () -> Unit,
    modifier: Modifier = Modifier,
    callContent: @Composable () -> Unit = {}
) {
    val shouldShowCall = state.phase == PlacementPhase.CALLING
    if (shouldShowCall != isCallDestination || state.phase == PlacementPhase.ALREADY_ASSESSED) {
        PlacementStatusScreen(
            state = PlacementStatusUiState(status = PlacementStatus.LOADING),
            onBack = onExit,
            onContinueAssessment = onContinueAssessment,
            onRetryAssessment = onRetry,
            onDeferAssessment = onExit,
            onGoHome = onExit,
            modifier = modifier
        )
        return
    }

    if (state.pendingGradingReason != null) {
        AssessmentPendingContent(
            onBack = onExit,
            onRetryGrading = onRetryGrading,
            onSaveUnassessed = null,
            title = stringResource(R.string.placement_title),
            modifier = modifier
        )
        return
    }
    if (state.recoveryProblem != null) {
        AssessmentRecoveryContent(
            title = stringResource(R.string.placement_title),
            description = stringResource(R.string.recovery_assessment_expired),
            onStartAgain = onRestartExpired,
            onBack = onExit,
            modifier = modifier
        )
        return
    }
    when {
        state.phase == PlacementPhase.INTRODUCTION ->
            PlacementIntroductionScreen(
                onBack = onExit,
                onStartAssessment = onStart,
                onSkipAssessment = onSkip,
                modifier = modifier,
                hasSaveFailed = state.saveFailed,
                isSaving = state.isSavingResult
            )
        state.phase == PlacementPhase.RESUME_PROMPT ->
            Box(modifier) {
                PlacementIntroductionScreen(
                    onBack = onExit,
                    onStartAssessment = onStart,
                    onSkipAssessment = onSkip,
                    modifier = Modifier.clearAndSetSemantics {}
                )
                PlacementResumeDialog(onResume = onResume, onRestart = onRestart, onDismiss = onExit)
            }
        state.phase == PlacementPhase.CALLING -> callContent()
        state.phase == PlacementPhase.NEXT_TASK ->
            PlacementStatusScreen(
                state =
                    PlacementStatusUiState(
                        status = PlacementStatus.CONFIRMATION,
                        isTimeLimitReached = state.hasReachedAudioLimit,
                        nextTask = state.nextTask,
                        taskNumber = state.currentTaskNumber,
                        taskRole = state.taskRole,
                        assessmentLevel = state.currentTask?.targetLevel.default(SpeakingLevel.NOT_ASSESSED)
                    ),
                onBack = onExit,
                onContinueAssessment = onContinueAssessment,
                onRetryAssessment = onRetry,
                onDeferAssessment = onExit,
                onGoHome = onExit,
                modifier = modifier
            )
        state.phase == PlacementPhase.GRADING ||
            state.phase == PlacementPhase.INSUFFICIENT_EVIDENCE ||
            state.phase == PlacementPhase.UNDETERMINED -> {
            val status =
                when (state.phase) {
                    PlacementPhase.GRADING -> PlacementStatus.PROCESSING
                    PlacementPhase.INSUFFICIENT_EVIDENCE -> PlacementStatus.INSUFFICIENT_EVIDENCE
                    else -> PlacementStatus.UNDETERMINED
                }
            PlacementStatusScreen(
                state =
                    PlacementStatusUiState(
                        status = status,
                        isTimeLimitReached = state.hasReachedAudioLimit,
                        insufficientReason = state.insufficientReason,
                        stopReason = state.stopReason,
                        canRetryLoad = state.canRetryLoad
                    ),
                onBack = onExit,
                onContinueAssessment = onContinueAssessment,
                onRetryAssessment = if (state.canRetryLoad) onRetryLoad else onRetry,
                onDeferAssessment = onExit,
                onGoHome = onExit,
                modifier = modifier
            )
        }
        state.phase == PlacementPhase.RESULT -> {
            val result = state.result
            if (result != null)
                PlacementCompletedResult(
                    result = result,
                    isSavingResult = state.isSavingResult,
                    saveFailed = state.saveFailed,
                    isResultSaved = state.isResultSaved,
                    onBack = onExit,
                    onSaveOrHome = onConfirmResultSaved,
                    modifier = modifier
                )
        }
        state.phase == PlacementPhase.LOADING ->
            PlacementStatusScreen(
                state = PlacementStatusUiState(status = PlacementStatus.LOADING),
                onBack = onExit,
                onContinueAssessment = onContinueAssessment,
                onRetryAssessment = onRetry,
                onDeferAssessment = onExit,
                onGoHome = onExit,
                modifier = modifier
            )
    }
}

@Composable
private fun PlacementCompletedResult(
    result: PlacementResult,
    isSavingResult: Boolean,
    saveFailed: Boolean,
    isResultSaved: Boolean,
    onBack: () -> Unit,
    onSaveOrHome: () -> Unit,
    modifier: Modifier
) {
    PlacementResultScreen(
        state =
            PlacementUiMapper.toResult(
                result = result,
                isSavingResult = isSavingResult,
                saveFailed = saveFailed,
                isResultSaved = isResultSaved
            ),
        onBack = onBack,
        onStartTrainingPath = onSaveOrHome,
        modifier = modifier
    )
}
