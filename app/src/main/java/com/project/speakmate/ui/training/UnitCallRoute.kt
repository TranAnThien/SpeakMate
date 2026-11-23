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
fun UnitCallRoute(
    unitId: String,
    isRetry: Boolean,
    onBack: () -> Unit,
    onCompleted: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UnitPreparationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var hasPreparedRetry by rememberSaveable(unitId) { mutableStateOf(false) }
    var activity by remember(viewModel) {
        mutableStateOf(viewModel.uiState.value.activity.takeIf { !isRetry || hasPreparedRetry })
    }
    LaunchedEffect(unitId, viewModel) {
        if (viewModel.uiState.value.status == TrainingContentStatus.LOADING) viewModel.getPreparation(unitId)
    }
    LaunchedEffect(state.activity, isRetry) {
        if (activity == null && state.canStartRoleplay) {
            if (isRetry && !hasPreparedRetry) {
                viewModel.prepareRetry()
                hasPreparedRetry = true
            }
            activity = viewModel.uiState.value.activity
        }
        if (activity != null) viewModel.stopObservingPreparation()
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
            activityType = CallActivityType.TRAINING_UNIT,
            onExit = onBack,
            activityContext = CallActivityContext.Training(callActivity),
            onPracticeCompleted = onCompleted,
            modifier = modifier
        )
    }
}
