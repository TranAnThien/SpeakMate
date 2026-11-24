package com.project.speakmate.ui.placement

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.domain.model.PlacementRole
import com.project.speakmate.domain.model.ReassessmentStep
import com.project.speakmate.ui.components.state.AssessmentPendingContent
import com.project.speakmate.ui.placement.components.AssessmentRecoveryContent
import com.project.speakmate.ui.placement.state.PlacementStatus
import com.project.speakmate.ui.placement.state.PlacementStatusFlow
import com.project.speakmate.ui.placement.state.PlacementStatusUiState
import com.project.speakmate.ui.placement.state.PromotionIntroductionUiState
import com.project.speakmate.ui.placement.state.ReassessmentPhase
import com.project.speakmate.ui.placement.state.ReassessmentUiState
import com.project.speakmate.utils.extension.default

@Composable
fun ReassessmentScreen(
    state: ReassessmentUiState,
    isCallDestination: Boolean,
    onExit: () -> Unit,
    onContinueAssessment: () -> Unit,
    onRetryTask: () -> Unit,
    onRetryLoad: () -> Unit,
    onRetryGrading: () -> Unit,
    onStart: () -> Unit,
    onRetrySave: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    callContent: @Composable () -> Unit = {}
) {
    val shouldShowCall = state.phase == ReassessmentPhase.CALLING
    if (shouldShowCall != isCallDestination ||
        state.phase in listOf(ReassessmentPhase.LOADING, ReassessmentPhase.CLOSED)) {
        PlacementStatusScreen(
            state = PlacementStatusUiState(status = PlacementStatus.LOADING, flow = PlacementStatusFlow.REASSESSMENT),
            onBack = onExit,
            onContinueAssessment = onContinueAssessment,
            onRetryAssessment = onRetryTask,
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
            title = stringResource(R.string.promotion_header),
            modifier = modifier
        )
        return
    }
    if (state.recoveryProblem != null) {
        AssessmentRecoveryContent(
            title = stringResource(R.string.promotion_header),
            description = stringResource(R.string.recovery_assessment_expired),
            onStartAgain = onRetryLoad,
            onBack = onExit,
            modifier = modifier
        )
        return
    }
    when (state.phase) {
        ReassessmentPhase.INTRODUCTION ->
            PromotionIntroductionScreen(
                state = PromotionIntroductionUiState(currentLevel = state.currentLevel, nextLevel = state.nextLevel),
                onBack = onExit,
                onStartAssessment = onStart,
                onReturnToPath = onExit,
                modifier = modifier
            )
        ReassessmentPhase.CALLING -> callContent()
        ReassessmentPhase.GRADING,
        ReassessmentPhase.NEXT_TASK,
        ReassessmentPhase.ERROR ->
            PlacementStatusScreen(
                state =
                    PlacementStatusUiState(
                        status =
                            when (state.phase) {
                                ReassessmentPhase.NEXT_TASK -> PlacementStatus.CONFIRMATION
                                ReassessmentPhase.ERROR -> PlacementStatus.UNDETERMINED
                                else -> PlacementStatus.PROCESSING
                            },
                        isTimeLimitReached = state.hasReachedAudioLimit,
                        flow = PlacementStatusFlow.REASSESSMENT,
                        reassessmentStep = state.assessment?.step,
                        assessmentLevel = state.assessment?.currentTask?.targetLevel.default(state.currentLevel),
                        taskNumber = state.assessment?.step?.ordinal?.plus(1).default(fallback = 0),
                        taskRole =
                            if (state.assessment?.step == ReassessmentStep.NEXT_PROBE) PlacementRole.PROBE
                            else PlacementRole.CONFIRMATION,
                        canRetryLoad = state.phase == ReassessmentPhase.ERROR
                    ),
                onBack = onExit,
                onContinueAssessment = onContinueAssessment,
                onRetryAssessment = if (state.phase == ReassessmentPhase.ERROR) onRetryLoad else onRetryTask,
                onDeferAssessment = onExit,
                onGoHome = onExit,
                modifier = modifier
            )
        ReassessmentPhase.RESULT ->
            ReassessmentCompletedResult(
                state = state,
                onRetry = onRetryTask,
                onRetrySave = onRetrySave,
                onReturnToPath = onExit,
                onOpenProfile = onOpenProfile,
                modifier = modifier
            )
        ReassessmentPhase.LOADING,
        ReassessmentPhase.CLOSED -> Unit
    }
}

@Composable
private fun ReassessmentCompletedResult(
    state: ReassessmentUiState,
    onRetry: () -> Unit,
    onRetrySave: () -> Unit,
    onReturnToPath: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier
) {
    PromotionResultScreen(
        state = ReassessmentUiMapper.toResult(state),
        onBack = onReturnToPath,
        onOpenPromotedPath = onReturnToPath,
        onReturnToCurrentPath = onReturnToPath,
        onRetryAssessment = onRetry,
        modifier = modifier,
        onRetrySave = onRetrySave,
        onOpenProfile = onOpenProfile
    )
}
