package com.project.speakmate.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.ui.call.CallRoute
import com.project.speakmate.ui.training.components.TrainingUnavailableContent
import com.project.speakmate.ui.training.state.TrainingContentStatus

@Composable
fun GoalAssessmentCallRoute(
    isRetry: Boolean,
    onBack: () -> Unit,
    onCompleted: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalAssessmentViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var hasPreparedRetry by rememberSaveable { mutableStateOf(false) }
    var activity by remember(viewModel) {
        mutableStateOf(viewModel.uiState.value.activity.takeIf { !isRetry || hasPreparedRetry || state.isReplay })
    }
    var isReplay by rememberSaveable { mutableStateOf(state.isReplay) }
    LaunchedEffect(state.activity, isRetry) {
        if (activity == null && state.status == TrainingContentStatus.CONTENT) {
            if (isRetry && !state.isReplay && !hasPreparedRetry) {
                viewModel.prepareRetry()
                hasPreparedRetry = true
            }
            activity = viewModel.uiState.value.activity
            isReplay = state.isReplay
        }
        if (activity != null) viewModel.stopObservingAssessment()
    }
    val callActivity = activity
    if (callActivity == null) {
        TrainingUnavailableContent(
            isLoading = state.status == TrainingContentStatus.LOADING,
            onRetry = if (state.isLocked) onBack else viewModel::retry,
            modifier = modifier
        )
    } else {
        CallRoute(
            scenario = callActivity.scenario,
            activityType = CallActivityType.GOAL_ASSESSMENT,
            onExit = onBack,
            activityContext = CallActivityContext.Training(callActivity),
            onPracticeCompleted = { attemptId -> onCompleted(attemptId, isReplay) },
            modifier = modifier
        )
    }
}
