package com.project.speakmate.ui.placement

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
import com.project.speakmate.ui.placement.components.PlacementAppBar
import com.project.speakmate.ui.placement.components.PlacementBadge
import com.project.speakmate.ui.placement.components.PlacementSecondaryButton
import com.project.speakmate.ui.placement.state.PlacementStatus
import com.project.speakmate.ui.placement.state.PlacementStatusUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.theme.SpeakMateTheme

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
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        PlacementAppBar(stringResource(R.string.placement_title), onBack)
        Surface(modifier = Modifier.fillMaxSize(), color = DesignColor.Scrim) {
            Box(Modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = 364.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .background(DesignColor.Surface, RoundedCornerShape(24.dp)).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(64.dp).background(DesignColor.IconBlue, CircleShape), contentAlignment = Alignment.Center) {
                        when (state.status) {
                            PlacementStatus.PROCESSING -> SpeakMateLoadingIndicator(size = 32.dp)
                            PlacementStatus.CONFIRMATION ->
                                Image(painterResource(R.drawable.ic_placement_pending), null, Modifier.size(32.dp))
                            PlacementStatus.INSUFFICIENT_EVIDENCE, PlacementStatus.UNDETERMINED ->
                                Image(painterResource(R.drawable.ic_placement_microphone_off), null, Modifier.size(32.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    val badgeRes = when (state.status) {
                        PlacementStatus.PROCESSING -> null
                        PlacementStatus.CONFIRMATION -> R.string.placement_unconfirmed
                        PlacementStatus.INSUFFICIENT_EVIDENCE -> R.string.placement_insufficient_evidence
                        PlacementStatus.UNDETERMINED -> R.string.placement_undetermined_badge
                    }
                    if (badgeRes != null) {
                        PlacementBadge(stringResource(badgeRes))
                        Spacer(Modifier.height(12.dp))
                    }
                    Text(stringResource(when (state.status) {
                        PlacementStatus.PROCESSING -> R.string.placement_processing_title
                        PlacementStatus.CONFIRMATION -> R.string.placement_confirmation_title
                        PlacementStatus.INSUFFICIENT_EVIDENCE -> R.string.placement_retry_title
                        PlacementStatus.UNDETERMINED -> R.string.placement_undetermined_title
                    }), style = DesignTypography.Heading, color = DesignColor.Text, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(when (state.status) {
                        PlacementStatus.PROCESSING -> R.string.placement_processing_description
                        PlacementStatus.CONFIRMATION -> R.string.placement_confirmation_description
                        PlacementStatus.INSUFFICIENT_EVIDENCE -> R.string.placement_retry_description
                        PlacementStatus.UNDETERMINED -> R.string.placement_undetermined_description
                    }), style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center)
                    if (state.isTimeLimitReached) {
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.placement_time_limit_notice), style = DesignTypography.Body,
                            color = DesignColor.Muted, textAlign = TextAlign.Center)
                    }
                    val primaryTextRes = when (state.status) {
                        PlacementStatus.PROCESSING -> null
                        PlacementStatus.CONFIRMATION -> R.string.placement_continue
                        PlacementStatus.INSUFFICIENT_EVIDENCE -> R.string.placement_retry
                        PlacementStatus.UNDETERMINED -> R.string.placement_home
                    }
                    if (primaryTextRes != null) {
                        Spacer(Modifier.height(12.dp))
                        PrimaryButton(
                            text = stringResource(primaryTextRes),
                            onClick = when (state.status) {
                                PlacementStatus.PROCESSING -> onGoHome
                                PlacementStatus.CONFIRMATION -> onContinueAssessment
                                PlacementStatus.INSUFFICIENT_EVIDENCE -> onRetryAssessment
                                PlacementStatus.UNDETERMINED -> onGoHome
                            }
                        )
                    }
                    val secondaryTextRes = when (state.status) {
                        PlacementStatus.PROCESSING -> null
                        PlacementStatus.CONFIRMATION -> R.string.placement_later
                        PlacementStatus.INSUFFICIENT_EVIDENCE -> R.string.placement_home
                        PlacementStatus.UNDETERMINED -> null
                    }
                    if (secondaryTextRes != null) {
                        Spacer(Modifier.height(12.dp))
                        PlacementSecondaryButton(
                            text = stringResource(secondaryTextRes),
                            onClick = when (state.status) {
                                PlacementStatus.PROCESSING -> onGoHome
                                PlacementStatus.CONFIRMATION -> onDeferAssessment
                                PlacementStatus.INSUFFICIENT_EVIDENCE -> onGoHome
                                PlacementStatus.UNDETERMINED -> onGoHome
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Placement / Processing / 32:2273", widthDp = 412, heightDp = 835, showBackground = true)
@Composable
private fun PlacementProcessingScreenPreview() {
    SpeakMateTheme {
        PlacementStatusScreen(PlacementStatusUiState(), {}, {}, {}, {}, {})
    }
}

@Preview(name = "Placement / Next-task confirmation / 32:2290", widthDp = 412, heightDp = 835, showBackground = true)
@Composable
private fun PlacementConfirmationScreenPreview() {
    SpeakMateTheme {
        PlacementStatusScreen(PlacementStatusUiState(PlacementStatus.CONFIRMATION), {}, {}, {}, {}, {})
    }
}

@Preview(name = "Placement / Insufficient evidence / 32:2307", widthDp = 412, heightDp = 835, showBackground = true)
@Composable
private fun PlacementInsufficientEvidenceScreenPreview() {
    SpeakMateTheme {
        PlacementStatusScreen(PlacementStatusUiState(PlacementStatus.INSUFFICIENT_EVIDENCE), {}, {}, {}, {}, {})
    }
}

@Preview(name = "Placement / Undetermined", widthDp = 412, heightDp = 835, showBackground = true)
@Composable
private fun PlacementUndeterminedScreenPreview() {
    SpeakMateTheme {
        PlacementStatusScreen(PlacementStatusUiState(PlacementStatus.UNDETERMINED), {}, {}, {}, {}, {})
    }
}

@Preview(name = "Placement / Time limit", widthDp = 412, heightDp = 835, showBackground = true)
@Composable
private fun PlacementTimeLimitScreenPreview() {
    SpeakMateTheme {
        PlacementStatusScreen(PlacementStatusUiState(isTimeLimitReached = true), {}, {}, {}, {}, {})
    }
}
