package com.project.speakmate.ui.training

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.AppBottomBar
import com.project.speakmate.ui.components.AppDestination
import com.project.speakmate.ui.components.LoadErrorDialog
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.training.components.TrainingGoalCard
import com.project.speakmate.ui.training.components.TrainingHeader
import com.project.speakmate.ui.training.components.TrainingUnavailableContent
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.ui.training.state.TrainingGoalUiState
import com.project.speakmate.ui.training.state.TrainingPathUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun TrainingPathScreen(
    state: TrainingPathUiState,
    onToggleGoal: (String) -> Unit,
    onOpenGoal: (String) -> Unit,
    onOpenUnit: (String) -> Unit,
    onContinuePath: () -> Unit,
    onShowLevelInformation: () -> Unit,
    onRetry: () -> Unit,
    onOpenTrainingPath: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TrainingHeader()
            Box(Modifier.weight(1f)) {
                when (state.status) {
                    TrainingContentStatus.CONTENT -> LazyColumn(
                        state = listState, modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item(key = R.string.training_estimated_level, contentType = R.string.training_estimated_level) {
                            TrainingPathIntroduction(state.level, onShowLevelInformation)
                        }
                        items(state.goals, key = { it.id }, contentType = { TrainingGoalUiState::class }) { goal ->
                            val onToggle = remember(goal.id, onToggleGoal) { { onToggleGoal(goal.id) } }
                            val onOpen = remember(goal.id, onOpenGoal) { { onOpenGoal(goal.id) } }
                            TrainingGoalCard(goal, onToggle, onOpen, onOpenUnit)
                        }
                    }
                    TrainingContentStatus.LOADING -> TrainingUnavailableContent(isLoading = true, onRetry = onRetry)
                    TrainingContentStatus.ERROR -> TrainingUnavailableContent(
                        isLoading = false,
                        onRetry = onRetry,
                        errorMessage = state.errorMessage
                    )
                    TrainingContentStatus.ERROR_DIALOG -> Unit
                }
            }
            if (state.status == TrainingContentStatus.CONTENT && state.canContinue) {
                Box(Modifier.fillMaxWidth().background(DesignColor.Surface).padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 12.dp)) {
                    PrimaryButton(stringResource(R.string.home_continue_path), onContinuePath)
                }
            }
            AppBottomBar(
                onOpenTrainingPath = onOpenTrainingPath,
                onOpenPractice = onOpenPractice,
                onOpenHistory = onOpenHistory,
                onOpenProfile = onOpenProfile,
                selectedDestination = AppDestination.TRAINING
            )
        }
        if (state.status == TrainingContentStatus.ERROR_DIALOG) {
            LoadErrorDialog(stringResource(R.string.training_error_dialog_title), stringResource(R.string.training_error_dialog_description), onRetry)
        }
    }
}

@Composable
private fun TrainingPathIntroduction(level: String, onShowLevelInformation: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, DesignColor.Border, RoundedCornerShape(16.dp)).padding(17.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(DesignColor.IconBlue, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(level, style = DesignTypography.LevelBadge, color = DesignColor.Primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.training_estimated_level), style = DesignTypography.Detail, color = DesignColor.Muted)
                Text(level, style = DesignTypography.Brand, color = DesignColor.Primary)
            }
            Box(Modifier.size(48.dp).clickable(onClick = onShowLevelInformation), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.ic_placement_info), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(DesignColor.Muted))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.training_goals_heading), style = DesignTypography.SectionLabel, color = DesignColor.Muted)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().background(DesignColor.PaleBlue, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_placement_info), null, Modifier.size(20.dp, 24.dp))
            Text(stringResource(R.string.training_goals_hint), style = DesignTypography.Detail, color = DesignColor.Primary)
        }
    }
}
