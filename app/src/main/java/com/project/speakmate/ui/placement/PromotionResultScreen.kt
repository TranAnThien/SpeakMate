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
import com.project.speakmate.R
import com.project.speakmate.ui.components.AnimatedMascot
import com.project.speakmate.ui.components.BackAppBar
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.SecondaryButton
import com.project.speakmate.ui.placement.state.PromotionResultPresentation
import com.project.speakmate.ui.placement.state.PromotionResultUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.utils.AppConstant

@Composable
fun PromotionResultScreen(
    state: PromotionResultUiState,
    onBack: () -> Unit,
    onOpenPromotedPath: () -> Unit,
    onReturnToCurrentPath: () -> Unit,
    onRetryAssessment: () -> Unit,
    modifier: Modifier = Modifier,
    onRetrySave: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    val promoted = state.presentation == PromotionResultPresentation.PROMOTED
    val hasPendingSave = promoted && !state.isResultSaved
    val canRetry = state.presentation == PromotionResultPresentation.INSUFFICIENT_EVIDENCE && state.canRetryAssessment
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(stringResource(R.string.promotion_header), onBack)
        Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                AnimatedMascot(if (promoted) R.raw.lottie_result_pass else R.raw.lottie_call_mascot,
                    if (state.presentation == PromotionResultPresentation
                        .INSUFFICIENT_EVIDENCE) R.drawable.img_call_muted else R.drawable.img_placement_mascot, Modifier.size(160.dp), marker = if (promoted) null else if (
                        state.presentation == PromotionResultPresentation.MAINTAINED) AppConstant.CALL_MASCOT_IDLE_MARKER else AppConstant.CALL_MASCOT_MUTED_MARKER)
                Text(state.title, style = DesignTypography.Heading, color = DesignColor.Text, textAlign = TextAlign.Center)
                Text(state.description, style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center)
                Text(stringResource(R.string.placement_result_disclaimer), style = DesignTypography.Detail, color = DesignColor.Muted, textAlign = TextAlign.Center)
            }
        }
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(stringResource(when {
                state.isSaveFailed -> R.string.unit_result_retry_save
                hasPendingSave -> R.string.placement_saving
                promoted -> R.string.promotion_open_path
                canRetry -> R.string.assessment_record_again
                else -> R.string.promotion_return_to_path
            }, if (promoted) state.nextLevelLabel else state.currentLevelLabel),
                when {
                    state.isSaveFailed -> onRetrySave
                    promoted -> onOpenPromotedPath
                    canRetry -> onRetryAssessment
                    else -> onReturnToCurrentPath
                }, enabled = !state.isSaving && (!hasPendingSave || state.isSaveFailed), isLoading = state.isSaving)
            if (promoted && state.isResultSaved) SecondaryButton(stringResource(R.string.assessment_view_profile), onOpenProfile)
            else if (canRetry || hasPendingSave) SecondaryButton(stringResource(R.string.content_back), onReturnToCurrentPath)
        }
    }
}
