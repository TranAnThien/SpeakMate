package com.project.speakmate.ui.placement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.BackAppBar
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.SecondaryButton
import com.project.speakmate.ui.placement.state.PromotionResultPresentation
import com.project.speakmate.ui.placement.state.PromotionResultUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun PromotionResultScreen(
    state: PromotionResultUiState,
    onBack: () -> Unit,
    onOpenPromotedPath: () -> Unit,
    onReturnToCurrentPath: () -> Unit,
    onRetryAssessment: () -> Unit,
    modifier: Modifier = Modifier,
    onRetrySave: () -> Unit = {}
) {
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(stringResource(R.string.promotion_header), onBack)
        Surface(Modifier.fillMaxSize(), color = DesignColor.Scrim) {
            Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
                Surface(Modifier.widthIn(max = 364.dp).fillMaxWidth().offset(y = (-16).dp),
                    shape = RoundedCornerShape(24.dp), color = DesignColor.Surface) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(64.dp).background(if (state.presentation == PromotionResultPresentation.PROMOTED)
                            DesignColor.ResultSuccessSurface else DesignColor.ResultWarningSurface, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(painterResource(when (state.presentation) {
                                PromotionResultPresentation.PROMOTED -> R.drawable.ic_placement_passed
                                PromotionResultPresentation.MAINTAINED -> R.drawable.ic_placement_promotion_maintained
                                PromotionResultPresentation.INSUFFICIENT_EVIDENCE -> R.drawable.ic_placement_microphone_off
                            }), null, Modifier.size(32.dp), tint = if (state.presentation == PromotionResultPresentation.PROMOTED)
                                DesignColor.Success else DesignColor.ResultWarning)
                        }
                        Text(state.title, style = DesignTypography.Heading, color = DesignColor.Text, textAlign = TextAlign.Center)
                        Text(state.description, style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center)
                        PrimaryButton(
                            when {
                                state.isSaving || (state.presentation == PromotionResultPresentation.PROMOTED && !state.isResultSaved && !state.isSaveFailed) -> stringResource(R.string.placement_saving)
                                state.isSaveFailed -> stringResource(R.string.unit_result_retry_save)
                                else -> when (state.presentation) {
                                    PromotionResultPresentation.PROMOTED -> stringResource(R.string.promotion_open_path, state.nextLevelLabel)
                                    PromotionResultPresentation.MAINTAINED -> stringResource(R.string.promotion_return_to_path, state.currentLevelLabel)
                                    PromotionResultPresentation.INSUFFICIENT_EVIDENCE -> stringResource(if (state.canRetryAssessment) R.string.placement_retry else R.string.goal_assessment_return_to_path)
                                }
                            },
                            if (state.isSaveFailed) onRetrySave else when (state.presentation) {
                                PromotionResultPresentation.PROMOTED -> onOpenPromotedPath
                                PromotionResultPresentation.MAINTAINED -> onReturnToCurrentPath
                                PromotionResultPresentation.INSUFFICIENT_EVIDENCE -> if (state.canRetryAssessment) onRetryAssessment else onReturnToCurrentPath
                            },
                            enabled = !state.isSaving && (state.presentation != PromotionResultPresentation.PROMOTED || state.isResultSaved || state.isSaveFailed)
                        )
                        if (state.presentation == PromotionResultPresentation.INSUFFICIENT_EVIDENCE && state.canRetryAssessment) {
                            SecondaryButton(stringResource(R.string.promotion_return_to_path, state.currentLevelLabel), onReturnToCurrentPath)
                        }
                    }
                }
            }
        }
    }
}
