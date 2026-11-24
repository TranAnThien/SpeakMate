package com.project.speakmate.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.R
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.ui.call.CallRoute
import com.project.speakmate.ui.training.components.TrainingUnavailableScreen
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
    LaunchedEffect(isRetry, viewModel) { viewModel.prepareCall(isRetry = isRetry) }
    val callActivity = state.activeCallActivity
    if (callActivity == null) {
        TrainingUnavailableScreen(
            title = stringResource(R.string.goal_assessment_header),
            isLoading = state.status == TrainingContentStatus.LOADING,
            onBack = onBack,
            onRetry = if (state.isLocked) onBack else viewModel::retry,
            errorMessage = if (state.isLocked) stringResource(R.string.goal_assessment_locked) else "",
            modifier = modifier
        )
    } else {
        CallRoute(
            scenario = callActivity.scenario,
            activityType = CallActivityType.GOAL_ASSESSMENT,
            onExit = onBack,
            activityContext = CallActivityContext.Training(callActivity),
            onPracticeCompleted = { attemptId -> onCompleted(attemptId, state.activeCallIsReplay) },
            modifier = modifier
        )
    }
}
