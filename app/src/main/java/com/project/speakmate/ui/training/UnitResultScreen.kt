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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.components.UnitResultCriterionCard
import com.project.speakmate.ui.training.components.UnitResultFooter
import com.project.speakmate.ui.training.components.UnitResultOutcome
import com.project.speakmate.ui.training.components.UnitResultRequiredActions
import com.project.speakmate.ui.training.state.UnitResultPresentation
import com.project.speakmate.ui.training.state.UnitResultUiState

@Composable
fun UnitResultScreen(
    state: UnitResultUiState,
    onBack: () -> Unit,
    onContinuePath: () -> Unit,
    onReplayUnit: () -> Unit,
    onRetryUnit: () -> Unit,
    onReviewLesson: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    onRetrySave: () -> Unit = {},
    explanation: String = state.explanation,
    criteriaHeading: String = state.criteriaHeading,
    feedback: String = state.feedback
) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(if (state.isUnavailable || state.presentation == UnitResultPresentation.PROCESSING)
            stringResource(R.string.unit_result_header) else state.title, onBack)
        if (state.isUnavailable) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(Modifier.verticalScroll(scrollState).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AnimatedMascot(R.raw.lottie_network_error, R.drawable.img_call_error, Modifier.size(140.dp))
                    Text(stringResource(R.string.unit_result_unavailable), style = DesignTypography.Body,
                        color = DesignColor.Muted, textAlign = TextAlign.Center)
                }
            }
            Column(Modifier.fillMaxWidth().background(DesignColor.Surface).padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(stringResource(R.string.content_retry), onReviewLesson)
                SecondaryButton(stringResource(R.string.goal_assessment_return_to_path), onContinuePath)
            }
        } else if (state.presentation == UnitResultPresentation.PROCESSING) {
            Box(Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center) {
                UnitResultOutcome(state.presentation, explanation,
                    Modifier.verticalScroll(scrollState).padding(24.dp))
            }
            Spacer(Modifier.height(88.dp))
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)
                .padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(stringResource(if (state.isSaved) R.string.practice_summary_saved_status else R.string.practice_summary_pending_status),
                    style = DesignTypography.Label, color = DesignColor.Primary)
                if (state.isReplay) Text(stringResource(R.string.unit_result_replay_notice), style = DesignTypography.Detail, color = DesignColor.Muted)
                UnitResultOutcome(state.presentation, explanation)
                UnitResultRequiredActions(state.requiredActions)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(criteriaHeading, style = DesignTypography.Title, color = DesignColor.Text)
                    state.criteria.forEach { criterion ->
                        key(criterion.id) {
                            UnitResultCriterionCard(criterion)
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().background(DesignColor.SecondaryButton, RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.unit_result_next), style = DesignTypography.Title, color = DesignColor.Text)
                    Text(feedback, style = DesignTypography.Detail, color = DesignColor.Primary)
                }
            }
            UnitResultFooter(state.presentation == UnitResultPresentation.PASS,
                onContinuePath, onReplayUnit, onRetryUnit, onReviewLesson,
                hasSaveFailed = state.saveFailed, onRetrySave = onRetrySave,
                isActionEnabled = !state.isSaving && (state.isSaved || (state.saveFailed && state.canRetrySave)))
        }
    }
}
