package com.project.speakmate.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.R
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.ui.training.components.TrainingUnavailableContent
import com.project.speakmate.ui.training.state.PathCompletionUiState
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.utils.extension.defaultEmpty

@Composable
fun PathCompletionRoute(
    onBack: () -> Unit,
    onReturnToPath: () -> Unit,
    onFreePractice: () -> Unit,
    onStartPromotion: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrainingPathViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val level = SpeakingLevel.entries.firstOrNull { it.name == state.level }
    if (state.status != TrainingContentStatus.CONTENT || state.canContinue || level == null) {
        TrainingUnavailableContent(
            isLoading = state.status == TrainingContentStatus.LOADING,
            onRetry = if (state.status == TrainingContentStatus.ERROR) viewModel::retry else onReturnToPath,
            modifier = modifier
        )
        return
    }
    val isHighestLevel = level == SpeakingLevel.B2
    val nextLevel = level.getNextLevel()?.name.defaultEmpty()
    PathCompletionScreen(
        state = PathCompletionUiState(
            levelLabel = level.name,
            nextLevelLabel = nextLevel,
            isHighestLevel = isHighestLevel,
            description = stringResource(R.string.path_completion_runtime_description, level.name),
            noticeTitle = stringResource(if (isHighestLevel) R.string.path_completion_b2_notice_title else R.string.path_completion_a2_notice_title),
            notice = if (isHighestLevel) stringResource(R.string.path_completion_b2_notice) else stringResource(R.string.path_completion_runtime_notice, nextLevel)
        ),
        onBack = onBack,
        onStartPromotion = onStartPromotion,
        onReturnToPath = onReturnToPath,
        onReviewPath = onReturnToPath,
        onFreePractice = onFreePractice,
        modifier = modifier,
        isPromotionEnabled = level.getNextLevel() != null
    )
}
