package com.project.speakmate.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.project.speakmate.R
import com.project.speakmate.ui.theme.SpeakMateTheme
import com.project.speakmate.ui.training.state.PathCompletionUiState

@Preview(name = "Path Completion / A2 / 17:411", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PathCompletionA2ScreenPreview() {
    SpeakMateTheme {
        PathCompletionScreen(
            state = PathCompletionUiState(
                levelLabel = "A2",
                nextLevelLabel = "B1",
                description = stringResource(R.string.path_completion_a2_description),
                noticeTitle = stringResource(R.string.path_completion_a2_notice_title),
                notice = stringResource(R.string.path_completion_a2_notice)
            ),
            onBack = {},
            onStartPromotion = {},
            onReturnToPath = {},
            onReviewPath = {},
            onFreePractice = {}
        )
    }
}

@Preview(name = "Path Completion / B2 terminal / 33:2599", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PathCompletionB2ScreenPreview() {
    SpeakMateTheme {
        PathCompletionScreen(
            state = PathCompletionUiState(
                levelLabel = "B2",
                isHighestLevel = true,
                description = stringResource(R.string.path_completion_b2_description),
                noticeTitle = stringResource(R.string.path_completion_b2_notice_title),
                notice = stringResource(R.string.path_completion_b2_notice)
            ),
            onBack = {},
            onStartPromotion = {},
            onReturnToPath = {},
            onReviewPath = {},
            onFreePractice = {}
        )
    }
}
