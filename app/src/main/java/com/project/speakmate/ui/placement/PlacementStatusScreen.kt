package com.project.speakmate.ui.placement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieConstants
import com.project.speakmate.R
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.PlacementRole
import com.project.speakmate.domain.model.PlacementStopReason
import com.project.speakmate.domain.model.ReassessmentStep
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.rules.PlacementRules
import com.project.speakmate.ui.components.AnimatedMascot
import com.project.speakmate.ui.components.BackAppBar
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.SecondaryButton
import com.project.speakmate.ui.placement.state.PlacementNextTask
import com.project.speakmate.ui.placement.state.PlacementStatus
import com.project.speakmate.ui.placement.state.PlacementStatusFlow
import com.project.speakmate.ui.placement.state.PlacementStatusUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.utils.AppConstant

@Composable
fun PlacementStatusScreen(
    state: PlacementStatusUiState,
    onBack: () -> Unit,
    onContinueAssessment: () -> Unit,
    onRetryAssessment: () -> Unit,
    onDeferAssessment: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isProcessing = state.status == PlacementStatus.PROCESSING || state.status == PlacementStatus.LOADING
    val isConfirmation = state.status == PlacementStatus.CONFIRMATION
    val canRetry = state.status == PlacementStatus.INSUFFICIENT_EVIDENCE || state.canRetryLoad
    val marker = when (state.status) {
        PlacementStatus.CONFIRMATION -> if (state.nextTask == PlacementNextTask.SAME_LEVEL) AppConstant.CALL_MASCOT_IDLE_MARKER else AppConstant.CALL_MASCOT_LISTENING_MARKER
        PlacementStatus.INSUFFICIENT_EVIDENCE -> AppConstant.CALL_MASCOT_MUTED_MARKER
        else -> AppConstant.CALL_MASCOT_ERROR_MARKER
    }
    val description = when {
        state.status == PlacementStatus.LOADING -> stringResource(R.string.assessment_loading_description)
        isConfirmation && state.flow == PlacementStatusFlow.REASSESSMENT -> stringResource(when (state.reassessmentStep) {
            ReassessmentStep.CURRENT_CONFIRMATION -> R.string.promotion_runtime_current_confirmation
            ReassessmentStep.NEXT_PROBE -> R.string.promotion_runtime_next_probe
            ReassessmentStep.NEXT_CONFIRMATION, null -> R.string.promotion_runtime_next_confirmation
        }, state.assessmentLevel.name)
        isConfirmation -> stringResource(when (state.nextTask) {
            PlacementNextTask.HIGHER_LEVEL -> R.string.assessment_next_higher
            PlacementNextTask.LOWER_LEVEL -> R.string.assessment_next_lower
            PlacementNextTask.SAME_LEVEL -> R.string.placement_additional_description
            null -> R.string.placement_confirmation_description
        })
        state.stopReason != null -> stringResource(when (state.stopReason) {
            PlacementStopReason.TECHNICAL_RETRY_LIMIT -> R.string.assessment_retry_limit
            PlacementStopReason.NO_TASK_AVAILABLE -> R.string.assessment_content_missing
            PlacementStopReason.TASK_LIMIT_REACHED -> R.string.assessment_task_limit
        }, PlacementRules.MAX_TECHNICAL_RETRIES)
        state.canRetryLoad -> stringResource(R.string.assessment_load_error)
        state.status == PlacementStatus.INSUFFICIENT_EVIDENCE -> stringResource(when (state.insufficientReason) {
            InsufficientEvidenceReason.NO_USER_SPEECH -> R.string.assessment_no_speech
            InsufficientEvidenceReason.NOT_ENOUGH_SPEECH -> R.string.assessment_not_enough_speech
            InsufficientEvidenceReason.AUDIO_UNAVAILABLE -> R.string.assessment_audio_missing
            InsufficientEvidenceReason.TRANSCRIPT_UNAVAILABLE -> R.string.assessment_transcript_missing
            InsufficientEvidenceReason.CALL_FAILED -> R.string.assessment_call_failed
            else -> R.string.placement_retry_description
        })
        isProcessing -> stringResource(R.string.placement_processing_description)
        else -> stringResource(R.string.placement_undetermined_description)
    }
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(stringResource(if (state.flow == PlacementStatusFlow.REASSESSMENT) R.string.promotion_header else R.string.placement_title), onBack)
        Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AnimatedMascot(if (isProcessing) R.raw.lottie_assessing else R.raw.lottie_call_mascot,
                    when {
                        isProcessing -> R.drawable.img_call_processing
                        marker == AppConstant.CALL_MASCOT_MUTED_MARKER -> R.drawable.img_call_muted
                        marker == AppConstant.CALL_MASCOT_ERROR_MARKER -> R.drawable.img_call_error
                        marker == AppConstant.CALL_MASCOT_LISTENING_MARKER -> R.drawable.img_call_listening
                        else -> R.drawable.img_placement_mascot
                    },
                    Modifier.size(160.dp), iterations = if (isProcessing) LottieConstants.IterateForever else 1,
                    marker = if (isProcessing) null else marker)
                Text(stringResource(when {
                    state.stopReason == PlacementStopReason.TECHNICAL_RETRY_LIMIT -> R.string.assessment_retry_limit_title
                    state.stopReason == PlacementStopReason.NO_TASK_AVAILABLE -> R.string.assessment_content_missing_title
                    state.canRetryLoad -> R.string.promotion_runtime_load_error
                    else -> when (state.status) {
                    PlacementStatus.LOADING -> R.string.assessment_loading_title
                    PlacementStatus.PROCESSING -> R.string.placement_processing_title
                    PlacementStatus.CONFIRMATION -> R.string.placement_confirmation_title
                    PlacementStatus.INSUFFICIENT_EVIDENCE -> R.string.placement_retry_title
                    PlacementStatus.UNDETERMINED -> R.string.placement_undetermined_title
                    }
                }), style = DesignTypography.Heading, color = DesignColor.Text, textAlign = TextAlign.Center)
                if (isConfirmation && state.taskNumber > 0) Text(stringResource(R.string.assessment_task_number, state.taskNumber),
                    style = DesignTypography.Title, color = DesignColor.Primary)
                if (isConfirmation && state.assessmentLevel != SpeakingLevel.NOT_ASSESSED) {
                    Text(stringResource(R.string.call_target_level, state.assessmentLevel.name), style = DesignTypography.Detail, color = DesignColor.Muted)
                }
                if (isConfirmation && state.taskRole != null) Text(stringResource(when (state.taskRole) {
                    PlacementRole.INITIAL -> R.string.assessment_role_initial
                    PlacementRole.PROBE -> R.string.assessment_role_probe
                    PlacementRole.CONFIRMATION -> R.string.assessment_role_confirmation
                }), style = DesignTypography.Detail, color = DesignColor.Muted)
                Text(description, style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center)
                if (state.isTimeLimitReached) Text(stringResource(R.string.placement_time_limit_notice), style = DesignTypography.Detail, color = DesignColor.Muted)
            }
        }
        if (!isProcessing) Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(stringResource(when {
                isConfirmation -> R.string.placement_continue
                canRetry -> if (state.status == PlacementStatus.INSUFFICIENT_EVIDENCE) R.string.assessment_record_again else R.string.content_retry
                else -> R.string.placement_home
            }), when {
                isConfirmation -> onContinueAssessment
                canRetry -> onRetryAssessment
                else -> onGoHome
            })
            if (isConfirmation || canRetry) SecondaryButton(stringResource(if (isConfirmation) R.string.placement_later else R.string.content_back),
                if (isConfirmation) onDeferAssessment else onGoHome)
        }
    }
}
