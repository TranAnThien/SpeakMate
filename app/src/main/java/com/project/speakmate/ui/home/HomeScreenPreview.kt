package com.project.speakmate.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.project.speakmate.ui.home.state.HomePresentation
import com.project.speakmate.ui.home.state.HomeUiState
import com.project.speakmate.ui.theme.SpeakMateTheme

@Preview(name = "Home / Unassessed / 33:3059", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun HomeUnassessedScreenPreview() {
    SpeakMateTheme {
        HomeScreen(
            state = HomeUiState(presentation = HomePresentation.UNASSESSED),
            onStartAssessment = {},
            onContinuePath = {},
            onStartReassessment = {},
            onReviewPath = {},
            onSelectScenario = {},
            onRetry = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Home / Active A2 / 33:3092", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun HomeActiveA2ScreenPreview() {
    SpeakMateTheme {
        HomeScreen(
            state = getHomePreviewState(HomePresentation.ACTIVE),
            onStartAssessment = {},
            onContinuePath = {},
            onStartReassessment = {},
            onReviewPath = {},
            onSelectScenario = {},
            onRetry = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Home / Path complete A2 / 33:3153", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun HomePathCompleteA2ScreenPreview() {
    SpeakMateTheme {
        HomeScreen(
            state = getHomePreviewState(HomePresentation.PATH_COMPLETE),
            onStartAssessment = {},
            onContinuePath = {},
            onStartReassessment = {},
            onReviewPath = {},
            onSelectScenario = {},
            onRetry = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Home / B2 complete / 33:3184", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun HomeB2CompleteScreenPreview() {
    SpeakMateTheme {
        HomeScreen(
            state = getHomePreviewState(HomePresentation.HIGHEST_LEVEL_COMPLETE).copy(level = "B2"),
            onStartAssessment = {},
            onContinuePath = {},
            onStartReassessment = {},
            onReviewPath = {},
            onSelectScenario = {},
            onRetry = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Home / Long name / Missing photo / 33:3269", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun HomeLongNameScreenPreview() {
    SpeakMateTheme {
        HomeScreen(
            state = getHomePreviewState(HomePresentation.ACTIVE).copy(displayName = "Nguyễn Hoàng Minh Anh Trần Văn Khánh", email = "quandang@example.com"),
            onStartAssessment = {},
            onContinuePath = {},
            onStartReassessment = {},
            onReviewPath = {},
            onSelectScenario = {},
            onRetry = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Home / Loading / 33:3215", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun HomeLoadingScreenPreview() {
    SpeakMateTheme {
        HomeScreen(
            state = HomeUiState(),
            onStartAssessment = {},
            onContinuePath = {},
            onStartReassessment = {},
            onReviewPath = {},
            onSelectScenario = {},
            onRetry = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Home / Load error dialog / 59:1237", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun HomeErrorDialogScreenPreview() {
    SpeakMateTheme {
        HomeScreen(
            state = HomeUiState(presentation = HomePresentation.ERROR_DIALOG),
            onStartAssessment = {},
            onContinuePath = {},
            onStartReassessment = {},
            onReviewPath = {},
            onSelectScenario = {},
            onRetry = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Home / Load error retry / 33:3244", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun HomeLoadErrorScreenPreview() {
    SpeakMateTheme {
        HomeScreen(
            state = HomeUiState(presentation = HomePresentation.ERROR),
            onStartAssessment = {},
            onContinuePath = {},
            onStartReassessment = {},
            onReviewPath = {},
            onSelectScenario = {},
            onRetry = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

private fun getHomePreviewState(presentation: HomePresentation): HomeUiState = HomeUiState(
    presentation = presentation,
    level = "A2",
    nextLevel = "B1",
    goalTitle = "Gọi món và trao đổi tại quán cà phê",
    unitTitle = "Điều chỉnh đơn gọi món",
    unitDescription = "Học cách yêu cầu thêm đường, đá hoặc đổi loại sữa."
)

