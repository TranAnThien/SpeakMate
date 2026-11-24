package com.project.speakmate.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.button.SecondaryButton
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.training.components.TrainingGoalCard
import com.project.speakmate.ui.training.components.TrainingHeader
import com.project.speakmate.ui.training.components.TrainingPathIntroduction
import com.project.speakmate.ui.training.components.TrainingPathLevelInformationBottomSheet
import com.project.speakmate.ui.training.components.TrainingPathNoticeDialog
import com.project.speakmate.ui.training.components.TrainingPathRefreshNotice
import com.project.speakmate.ui.training.components.TrainingUnassessedContent
import com.project.speakmate.ui.training.components.TrainingUnavailableContent
import com.project.speakmate.ui.training.state.TrainingActivityDestination
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.ui.training.state.TrainingGoalUiState
import com.project.speakmate.ui.training.state.TrainingPathDialog
import com.project.speakmate.ui.training.state.TrainingPathFailure
import com.project.speakmate.ui.training.state.TrainingPathUiState

@Composable
fun TrainingPathScreen(
    state: TrainingPathUiState,
    onToggleGoal: (String) -> Unit,
    onOpenGoal: (String) -> Unit,
    onOpenUnit: (TrainingActivityDestination) -> Unit,
    onContinuePath: () -> Unit,
    onShowLevelInformation: () -> Unit,
    onRetry: () -> Unit,
    onStartPlacement: () -> Unit,
    onFreePractice: () -> Unit,
    onViewCompletedPath: () -> Unit,
    onDismissLevelInformation: () -> Unit,
    onDismissGoal: () -> Unit,
    onPrepareGoal: () -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    when (state.activeDialog) {
        TrainingPathDialog.LevelInformation -> {
            TrainingPathLevelInformationBottomSheet(onDismiss = onDismissLevelInformation)
        }
        is TrainingPathDialog.Goal ->
            state.selectedGoal?.let { goal ->
                key(state.selectedGoalId) {
                    GoalDetailScreen(
                        state = goal,
                        onBack = onDismissGoal,
                        onOpenUnit = onOpenUnit,
                        onPrepareCurrentUnit = onPrepareGoal,
                        onReturnToPath = onDismissGoal
                    )
                }
            }
        null -> Unit
    }
    TrainingPathNoticeDialog(unit = state.previewUnit, notice = state.notice, onDismiss = onDismissNotice)
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TrainingHeader(onShowLevelInformation = onShowLevelInformation, shouldShowInformation = true)
            Box(Modifier.weight(1f)) {
                when (state.status) {
                    TrainingContentStatus.CONTENT ->
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (state.isRefreshing || state.isStale) {
                                item(
                                    key = "training-stale",
                                    contentType = "training-stale"
                                ) {
                                    TrainingPathRefreshNotice(isRefreshing = state.isRefreshing)
                                }
                            }
                            if (state.isUnassessed) {
                                item(
                                    key = "training-unassessed",
                                    contentType = "training-unassessed"
                                ) {
                                    TrainingUnassessedContent(onStartPlacement, onFreePractice)
                                }
                            } else {
                                item(
                                    key = "training-introduction",
                                    contentType = "training-introduction"
                                ) {
                                    TrainingPathIntroduction(
                                        level = state.level,
                                        isPathCompleted = state.isPathCompleted,
                                        completedActivityCount = state.completedActivityCount,
                                        totalActivityCount = state.totalActivityCount,
                                        currentActivityTitle = state.currentActivityTitle,
                                        canContinue = state.canContinue,
                                        isAssessmentReady = state.isAssessmentReady,
                                        isRefreshing = state.isRefreshing,
                                        isStale = state.isStale,
                                        isOpeningActivity = state.isOpeningActivity,
                                        onContinue = onContinuePath
                                    )
                                }
                                items(
                                    items = state.goals,
                                    key = { it.id },
                                    contentType = { TrainingGoalUiState::class }
                                ) { goal ->
                                    val onToggle = remember(goal.id, onToggleGoal) { { onToggleGoal(goal.id) } }
                                    val onOpen = remember(goal.id, onOpenGoal) { { onOpenGoal(goal.id) } }
                                    TrainingGoalCard(
                                        goal = goal,
                                        onToggle = onToggle,
                                        onOpenGoal = onOpen,
                                        onOpenUnit = onOpenUnit
                                    )
                                }
                                item(
                                    key = "training-free-practice",
                                    contentType = "training-free-practice"
                                ) {
                                    SecondaryButton(
                                        stringResource(R.string.navigation_practice),
                                        onFreePractice
                                    )
                                }
                            }
                            item(key = "training-refresh", contentType = "training-refresh") {
                                SecondaryButton(
                                    text = stringResource(R.string.training_refresh),
                                    onClick = onRetry,
                                    enabled = !state.isRefreshing
                                )
                            }
                        }
                    TrainingContentStatus.LOADING -> TrainingUnavailableContent(isLoading = true, onRetry = onRetry)
                    TrainingContentStatus.ERROR ->
                        TrainingUnavailableContent(
                            isLoading = false,
                            onRetry = onRetry,
                            errorMessage =
                                stringResource(
                                    when (state.failure) {
                                        TrainingPathFailure.PROFILE -> R.string.training_profile_error
                                        TrainingPathFailure.UNCONFIGURED -> R.string.training_content_unconfigured
                                        TrainingPathFailure.PROGRESS -> R.string.training_progress_invalid
                                        TrainingPathFailure.CONTENT,
                                        null -> R.string.training_error_dialog_description
                                    }
                                )
                        )
                }
            }
            if (state.status == TrainingContentStatus.CONTENT &&
                state.isPathCompleted &&
                !state.isStale &&
                !state.isRefreshing) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(DesignColor.Surface)
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp, bottom = 12.dp)
                ) {
                    PrimaryButton(stringResource(R.string.training_view_completed_path), onViewCompletedPath)
                }
            }
        }
    }
}
