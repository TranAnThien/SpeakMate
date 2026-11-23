package com.project.speakmate.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.SecondaryButton
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.components.TrainingGoalCard
import com.project.speakmate.ui.training.components.TrainingHeader
import com.project.speakmate.ui.training.components.TrainingPathNoticeDialog
import com.project.speakmate.ui.training.components.TrainingUnavailableContent
import com.project.speakmate.ui.training.state.TrainingActivityDestination
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.ui.training.state.TrainingGoalUiState
import com.project.speakmate.ui.training.state.TrainingPathFailure
import com.project.speakmate.ui.training.state.TrainingPathUiState

@OptIn(ExperimentalMaterial3Api::class)
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
    if (state.isLevelInformationVisible) {
        ModalBottomSheet(onDismissRequest = onDismissLevelInformation, containerColor = DesignColor.SurfaceContainerLow,
            contentWindowInsets = { WindowInsets.safeContent.union(WindowInsets(top = 32.dp, bottom = 24.dp)) }) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.training_estimated_level), style = DesignTypography.AppBar, color = DesignColor.Text)
                Text(stringResource(R.string.training_level_information), style = DesignTypography.Body, color = DesignColor.Muted)
                PrimaryButton(stringResource(R.string.training_understood), onDismissLevelInformation)
            }
        }
    }
    state.selectedGoal?.let { goal ->
        key(state.selectedGoalId) {
            GoalDetailScreen(goal, onDismissGoal, onOpenUnit, onPrepareGoal, onDismissGoal)
        }
    }
    TrainingPathNoticeDialog(state.previewUnit, state.notice, onDismissNotice)
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TrainingHeader(onShowLevelInformation, true)
            Box(Modifier.weight(1f)) {
                when (state.status) {
                    TrainingContentStatus.CONTENT -> LazyColumn(
                        state = listState, modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (state.isRefreshing || state.isStale) {
                            item(key = R.string.training_stale_title, contentType = R.string.training_stale_title) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(if (state.isRefreshing) R.string.training_refreshing else R.string.training_stale_title),
                                        style = DesignTypography.Title, color = DesignColor.Text)
                                    Text(stringResource(R.string.training_stale_description), style = DesignTypography.Detail, color = DesignColor.Muted)
                                    if (state.isRefreshing) SpeakMateLoadingIndicator(size = 24.dp)
                                }
                            }
                        }
                        if (state.isUnassessed) {
                            item(key = R.string.training_unassessed_title, contentType = R.string.training_unassessed_title) {
                                TrainingUnassessedContent(onStartPlacement, onFreePractice)
                            }
                        } else {
                            item(key = R.string.training_estimated_level, contentType = R.string.training_estimated_level) {
                                TrainingPathIntroduction(state, onContinuePath)
                            }
                            items(state.goals, key = { it.id }, contentType = { TrainingGoalUiState::class }) { goal ->
                                val onToggle = remember(goal.id, onToggleGoal) { { onToggleGoal(goal.id) } }
                                val onOpen = remember(goal.id, onOpenGoal) { { onOpenGoal(goal.id) } }
                                TrainingGoalCard(goal, onToggle, onOpen, onOpenUnit)
                            }
                            item(key = R.string.navigation_practice, contentType = R.string.navigation_practice) {
                                SecondaryButton(stringResource(R.string.navigation_practice), onFreePractice)
                            }
                        }
                        item(key = R.string.training_refresh, contentType = R.string.training_refresh) {
                            SecondaryButton(stringResource(R.string.training_refresh), onRetry, enabled = !state.isRefreshing)
                        }
                    }
                    TrainingContentStatus.LOADING -> TrainingUnavailableContent(isLoading = true, onRetry = onRetry)
                    TrainingContentStatus.ERROR -> TrainingUnavailableContent(
                        isLoading = false,
                        onRetry = onRetry,
                        errorMessage = stringResource(when (state.failure) {
                            TrainingPathFailure.PROFILE -> R.string.training_profile_error
                            TrainingPathFailure.UNCONFIGURED -> R.string.training_content_unconfigured
                            TrainingPathFailure.PROGRESS -> R.string.training_progress_invalid
                            TrainingPathFailure.CONTENT, null -> R.string.training_error_dialog_description
                        })
                    )
                }
            }
            if (state.status == TrainingContentStatus.CONTENT && state.isPathCompleted && !state.isStale && !state.isRefreshing) {
                Box(Modifier.fillMaxWidth().background(DesignColor.Surface).padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 12.dp)) {
                    PrimaryButton(stringResource(R.string.training_view_completed_path), onViewCompletedPath)
                }
            }

        }

    }
}

@Composable
private fun TrainingPathIntroduction(state: TrainingPathUiState, onContinue: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.training_your_path), style = DesignTypography.Badge, color = DesignColor.Muted)
            Text(stringResource(if (state.isPathCompleted && state.level == SpeakingLevel.B2.name) R.string.training_b2_completed_title
                else if (state.isPathCompleted) R.string.training_path_completed_title else R.string.training_progress_title,
                state.level, state.completedActivityCount, state.totalActivityCount), style = DesignTypography.AppBar, color = DesignColor.Text)
            Text(stringResource(R.string.training_speaking_level, state.level), style = DesignTypography.Badge, color = DesignColor.Primary,
                modifier = Modifier.background(DesignColor.PrimaryContainer, RoundedCornerShape(28.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
            LinearProgressIndicator(progress = { if (state.totalActivityCount > 0) state.completedActivityCount.toFloat() / state.totalActivityCount else 0f },
                modifier = Modifier.fillMaxWidth(), color = DesignColor.Primary, trackColor = DesignColor.SurfaceContainerHighest)
            Text(stringResource(R.string.training_progress_explanation), style = DesignTypography.Caption, color = DesignColor.Muted)
            if (state.currentActivityTitle.isNotBlank()) Text(state.currentActivityTitle, style = DesignTypography.Detail, color = DesignColor.Muted)
            if (state.canContinue) PrimaryButton(stringResource(if (state.isAssessmentReady) R.string.goal_assessment_header else R.string.home_continue_path),
                onContinue, enabled = !state.isRefreshing && !state.isStale, isLoading = state.isOpeningActivity)
        }
        Text(stringResource(R.string.training_goals_heading), style = DesignTypography.SectionLabel, color = DesignColor.Text)
    }
}

@Composable
private fun TrainingUnassessedContent(onStartPlacement: () -> Unit, onFreePractice: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.training_your_path), style = DesignTypography.Badge, color = DesignColor.Muted)
            Text(stringResource(R.string.training_unassessed_title), style = DesignTypography.AppBar, color = DesignColor.Text)
            Text(stringResource(R.string.training_unassessed_badge), style = DesignTypography.Badge, color = DesignColor.Muted,
                modifier = Modifier.background(DesignColor.SecondaryContainer, RoundedCornerShape(28.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
            Text(stringResource(R.string.training_unassessed_description), style = DesignTypography.Caption, color = DesignColor.Muted)
            PrimaryButton(stringResource(R.string.training_start_placement), onStartPlacement)
        }
        OutlinedCard(onClick = onFreePractice, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.navigation_practice), style = DesignTypography.Title, color = DesignColor.Text)
                Text(stringResource(R.string.training_unassessed_practice), style = DesignTypography.Detail, color = DesignColor.Muted)
            }
        }
    }
}
