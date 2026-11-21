package com.project.speakmate.ui.placement

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.placement.state.PromotionIntroductionUiState
import com.project.speakmate.ui.placement.state.PromotionStepUiState
import com.project.speakmate.ui.theme.SpeakMateTheme

@Preview(name = "Promotion Reassessment / Introduction / 17:663", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PromotionIntroductionScreenPreview() {
    SpeakMateTheme {
        PromotionIntroductionScreen(
            state = rememberPromotionIntroductionPreviewState(),
            onBack = {},
            onStartAssessment = {},
            onReturnToPath = {}
        )
    }
}

@Preview(name = "Promotion Reassessment / Introduction / Bottom / 131:6020", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PromotionIntroductionBottomScreenPreview() {
    SpeakMateTheme {
        PromotionIntroductionScreen(
            state = rememberPromotionIntroductionPreviewState(),
            onBack = {},
            onStartAssessment = {},
            onReturnToPath = {},
            scrollState = rememberScrollState(with(LocalDensity.current) { 187.dp.roundToPx() })
        )
    }
}

@Composable
private fun rememberPromotionIntroductionPreviewState(): PromotionIntroductionUiState = PromotionIntroductionUiState(
    title = stringResource(R.string.promotion_introduction_title),
    description = stringResource(R.string.promotion_introduction_description),
    steps = listOf(
        PromotionStepUiState("current_confirmation", 1, "Xác nhận trình độ A2 hiện tại."),
        PromotionStepUiState("next_probe", 2, "Thử sức với tình huống ở B1."),
        PromotionStepUiState("next_confirmation", 3, "Xác nhận B1 trong bối cảnh khác.")
    ),
    conditionTitle = stringResource(R.string.promotion_introduction_condition_title),
    conditionDescription = stringResource(R.string.promotion_introduction_condition_description),
    note = stringResource(R.string.promotion_introduction_note)
)
