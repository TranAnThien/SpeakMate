package com.project.speakmate.ui.placement

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.project.speakmate.R
import com.project.speakmate.ui.placement.state.PromotionResultPresentation
import com.project.speakmate.ui.placement.state.PromotionResultUiState
import com.project.speakmate.ui.theme.SpeakMateTheme

@Preview(name = "Reassessment / B1 Confirmed / Compact Dialog / Audited / 33:2388", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PromotionPromotedCompactScreenPreview() {
    SpeakMateTheme {
        PromotionResultScreen(
            state = PromotionResultUiState(
                presentation = PromotionResultPresentation.PROMOTED,
                title = stringResource(R.string.promotion_promoted_compact_title),
                description = stringResource(R.string.promotion_promoted_compact_description),
                currentLevelLabel = "A2",
                nextLevelLabel = "B1"
            ),
            onBack = {},
            onOpenPromotedPath = {},
            onReturnToCurrentPath = {},
            onRetryAssessment = {}
        )
    }
}

@Preview(name = "Reassessment / B1 Confirmed / Detailed Dialog / Audited / 34:3585", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PromotionPromotedDetailedScreenPreview() {
    SpeakMateTheme {
        PromotionResultScreen(
            state = PromotionResultUiState(
                presentation = PromotionResultPresentation.PROMOTED,
                title = stringResource(R.string.promotion_promoted_detailed_title),
                description = stringResource(R.string.promotion_promoted_detailed_description),
                currentLevelLabel = "A2",
                nextLevelLabel = "B1"
            ),
            onBack = {},
            onOpenPromotedPath = {},
            onReturnToCurrentPath = {},
            onRetryAssessment = {}
        )
    }
}

@Preview(name = "Reassessment / A2 Maintained / Compact Dialog / Audited / 33:2405", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PromotionMaintainedCompactScreenPreview() {
    SpeakMateTheme {
        PromotionResultScreen(
            state = PromotionResultUiState(
                presentation = PromotionResultPresentation.MAINTAINED,
                title = stringResource(R.string.promotion_maintained_compact_title),
                description = stringResource(R.string.promotion_maintained_compact_description),
                currentLevelLabel = "A2",
                nextLevelLabel = "B1"
            ),
            onBack = {},
            onOpenPromotedPath = {},
            onReturnToCurrentPath = {},
            onRetryAssessment = {}
        )
    }
}

@Preview(name = "Reassessment / A2 Maintained / Detailed Dialog / Audited / 34:3608", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PromotionMaintainedDetailedScreenPreview() {
    SpeakMateTheme {
        PromotionResultScreen(
            state = PromotionResultUiState(
                presentation = PromotionResultPresentation.MAINTAINED,
                title = stringResource(R.string.promotion_maintained_detailed_title),
                description = stringResource(R.string.promotion_maintained_detailed_description),
                currentLevelLabel = "A2",
                nextLevelLabel = "B1"
            ),
            onBack = {},
            onOpenPromotedPath = {},
            onReturnToCurrentPath = {},
            onRetryAssessment = {}
        )
    }
}

@Preview(name = "Reassessment / Insufficient Evidence / Compact Dialog / Audited / 33:2420", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PromotionInsufficientCompactScreenPreview() {
    SpeakMateTheme {
        PromotionResultScreen(
            state = PromotionResultUiState(
                presentation = PromotionResultPresentation.INSUFFICIENT_EVIDENCE,
                title = stringResource(R.string.promotion_insufficient_compact_title),
                description = stringResource(R.string.promotion_insufficient_compact_description),
                currentLevelLabel = "A2",
                nextLevelLabel = "B1"
            ),
            onBack = {},
            onOpenPromotedPath = {},
            onReturnToCurrentPath = {},
            onRetryAssessment = {}
        )
    }
}

@Preview(name = "Reassessment / Insufficient Evidence / Detailed Dialog / Audited / 34:3631", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PromotionInsufficientDetailedScreenPreview() {
    SpeakMateTheme {
        PromotionResultScreen(
            state = PromotionResultUiState(
                presentation = PromotionResultPresentation.INSUFFICIENT_EVIDENCE,
                title = stringResource(R.string.promotion_insufficient_detailed_title),
                description = stringResource(R.string.promotion_insufficient_detailed_description),
                currentLevelLabel = "A2",
                nextLevelLabel = "B1"
            ),
            onBack = {},
            onOpenPromotedPath = {},
            onReturnToCurrentPath = {},
            onRetryAssessment = {}
        )
    }
}
