package com.project.speakmate.ui.training

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.project.speakmate.ui.theme.SpeakMateTheme
import com.project.speakmate.ui.training.state.GoalDetailUiState
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.ui.training.state.TrainingGoalUiState
import com.project.speakmate.ui.training.state.TrainingPathUiState
import com.project.speakmate.ui.training.state.TrainingProgressStatus
import com.project.speakmate.ui.training.state.TrainingUnitUiState

@Preview(name = "Training Path / Collapsed / 88:4", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun TrainingPathCollapsedScreenPreview() {
    SpeakMateTheme {
        TrainingPathScreen(
            state = getTrainingPathPreviewState(isExpanded = false),
            onToggleGoal = {},
            onOpenGoal = {},
            onOpenUnit = {},
            onContinuePath = {},
            onShowLevelInformation = {},
            onRetry = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Training Path / Goal 1 expanded / 89:56", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun TrainingPathExpandedScreenPreview() {
    SpeakMateTheme {
        TrainingPathScreen(
            state = getTrainingPathPreviewState(isExpanded = true),
            onToggleGoal = {},
            onOpenGoal = {},
            onOpenUnit = {},
            onContinuePath = {},
            onShowLevelInformation = {},
            onRetry = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Training Path / Scroll from Goal 1 to Goal 2 / 94:355", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun TrainingPathScrollScreenPreview() {
    SpeakMateTheme {
        TrainingPathScreen(
            state = getTrainingPathPreviewState(isExpanded = true),
            onToggleGoal = {},
            onOpenGoal = {},
            onOpenUnit = {},
            onContinuePath = {},
            onShowLevelInformation = {},
            onRetry = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {},
            listState = rememberLazyListState(initialFirstVisibleItemIndex = 1)
        )
    }
}

@Preview(name = "Training Path / Loading / 36:3936", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun TrainingPathLoadingScreenPreview() {
    SpeakMateTheme {
        TrainingPathScreen(
            state = TrainingPathUiState(),
            onToggleGoal = {},
            onOpenGoal = {},
            onOpenUnit = {},
            onContinuePath = {},
            onShowLevelInformation = {},
            onRetry = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Training Path / Load error dialog / 59:1280", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun TrainingPathErrorDialogScreenPreview() {
    SpeakMateTheme {
        TrainingPathScreen(
            state = TrainingPathUiState(status = TrainingContentStatus.ERROR_DIALOG),
            onToggleGoal = {},
            onOpenGoal = {},
            onOpenUnit = {},
            onContinuePath = {},
            onShowLevelInformation = {},
            onRetry = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Training Path / Load error retry / 36:3872", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun TrainingPathLoadErrorScreenPreview() {
    SpeakMateTheme {
        TrainingPathScreen(
            state = TrainingPathUiState(status = TrainingContentStatus.ERROR),
            onToggleGoal = {},
            onOpenGoal = {},
            onOpenUnit = {},
            onContinuePath = {},
            onShowLevelInformation = {},
            onRetry = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Goal detail / Populated / 90:131", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun GoalDetailScreenPreview() {
    SpeakMateTheme {
        GoalDetailScreen(
            state = GoalDetailUiState(
                title = "Gọi món và trao đổi tại quán cà phê",
                description = "Gọi đồ uống, yêu cầu điều chỉnh và thanh toán trong một cuộc hội thoại tự nhiên.",
                units = getTrainingUnitsPreviewState(hasDescription = true),
                canPrepareCurrentUnit = true
            ),
            onBack = {},
            onOpenUnit = {},
            onPrepareCurrentUnit = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Goal detail / No units / 36:4059", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun GoalDetailNoUnitsScreenPreview() {
    SpeakMateTheme {
        GoalDetailScreen(
            state = GoalDetailUiState(),
            onBack = {},
            onOpenUnit = {},
            onPrepareCurrentUnit = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

private fun getTrainingPathPreviewState(isExpanded: Boolean): TrainingPathUiState = TrainingPathUiState(
    status = TrainingContentStatus.CONTENT,
    level = "A2",
    goals = listOf(
        TrainingGoalUiState(
            id = "goal_1",
            label = "GOAL 1",
            title = "Gọi món và trao đổi tại quán cà phê",
            summary = "Unit hiện tại: Điều chỉnh đơn gọi món",
            unitsHeading = "CÁC UNIT TRONG GOAL 1",
            status = TrainingProgressStatus.CURRENT,
            isExpanded = isExpanded,
            units = getTrainingUnitsPreviewState()
        ),
        TrainingGoalUiState(
            id = "goal_2",
            label = "GOAL 2",
            title = "Hỏi đường và sử dụng phương tiện",
            summary = "Có thể xem trước, chưa thể bắt đầu."
        )
    ),
    canContinue = true
)

private fun getTrainingUnitsPreviewState(hasDescription: Boolean = false): List<TrainingUnitUiState> = listOf(
    TrainingUnitUiState(
        id = "unit_1",
        title = "Unit 1 · Gọi đồ uống",
        status = TrainingProgressStatus.COMPLETED,
        canOpen = true
    ),
    TrainingUnitUiState(
        id = "unit_2",
        title = "Unit 2 · Điều chỉnh đơn gọi món",
        description = if (hasDescription) "Yêu cầu thêm đường, đá hoặc đổi loại sữa." else "",
        status = TrainingProgressStatus.CURRENT,
        canOpen = true
    ),
    TrainingUnitUiState(
        id = "unit_3",
        title = "Unit 3 · Hỏi thông tin và thanh toán"
    ),
    TrainingUnitUiState(
        id = "goal_assessment",
        title = "Đánh giá mục tiêu",
        isAssessment = true
    )
)

