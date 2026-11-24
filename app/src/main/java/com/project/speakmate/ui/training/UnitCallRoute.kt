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
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.ui.call.CallRoute
import com.project.speakmate.ui.training.components.TrainingUnavailableScreen
import com.project.speakmate.ui.training.state.TrainingContentStatus

@Composable
fun UnitCallRoute(
    unitId: String,
    isRetry: Boolean,
    onBack: () -> Unit,
    onCompleted: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UnitPreparationViewModel = hiltViewModel(),
    onCallCancelled: () -> Unit = onBack
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(unitId, isRetry, viewModel) { viewModel.prepareCall(unitId = unitId, isRetry = isRetry) }
    val callActivity = state.activeCallActivity
    if (callActivity == null) {
        TrainingUnavailableScreen(
            title = stringResource(R.string.preparation_title),
            isLoading = state.status == TrainingContentStatus.LOADING,
            onBack = onBack,
            onRetry = if (state.isLocked) onBack else viewModel::retry,
            errorMessage = if (state.isLocked) stringResource(R.string.preparation_unit_locked) else "",
            loadingTitleRes = R.string.preparation_loading_title,
            loadingDescriptionRes = R.string.preparation_loading_description,
            errorTitleRes = R.string.preparation_error_title,
            errorDescriptionRes = R.string.preparation_error_description,
            modifier = modifier
        )
    } else {
        CallRoute(
            scenario = callActivity.scenario,
            activityType = CallActivityType.TRAINING_UNIT,
            onExit = onBack,
            onCallFinished = { session ->
                if (session.outcome == CallOutcome.CANCELLED) onCallCancelled() else onBack()
            },
            activityContext = CallActivityContext.Training(callActivity),
            onPracticeCompleted = { attemptId -> onCompleted(attemptId, state.activeCallIsReplay) },
            modifier = modifier
        )
    }
}
