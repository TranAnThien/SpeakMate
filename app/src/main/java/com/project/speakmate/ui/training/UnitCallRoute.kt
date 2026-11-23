package com.project.speakmate.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.R
import com.project.speakmate.ui.components.BackAppBar
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
    onCompleted: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UnitPreparationViewModel = hiltViewModel(),
    onCallCancelled: () -> Unit = onBack
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var hasPreparedRetry by rememberSaveable(unitId) { mutableStateOf(false) }
    var activity by remember(viewModel) {
        mutableStateOf(viewModel.uiState.value.activity.takeIf { !isRetry || hasPreparedRetry })
    }
    LaunchedEffect(unitId, viewModel) {
        if (viewModel.uiState.value.status == TrainingContentStatus.LOADING) viewModel.getPreparation(unitId, requireServer = true)
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
        Column(modifier.fillMaxSize()) {
            BackAppBar(stringResource(R.string.preparation_title), onBack)
            TrainingUnavailableContent(
                isLoading = state.status == TrainingContentStatus.LOADING,
                onRetry = if (state.isLocked) onBack else viewModel::retry,
                errorMessage = if (state.isLocked) stringResource(R.string.preparation_unit_locked) else "",
                loadingTitleRes = R.string.preparation_loading_title,
                loadingDescriptionRes = R.string.preparation_loading_description,
                errorTitleRes = R.string.preparation_error_title,
                errorDescriptionRes = R.string.preparation_error_description,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        CallRoute(
            scenario = callActivity.scenario,
            activityType = CallActivityType.TRAINING_UNIT,
            onExit = onBack,
            onCallFinished = { session ->
                if (session.outcome == com.project.speakmate.domain.model.CallOutcome.CANCELLED) onCallCancelled() else onBack()
            },
            activityContext = CallActivityContext.Training(callActivity),
            onPracticeCompleted = { onCompleted(it, state.isReplay) },
            modifier = modifier
        )
    }
}
