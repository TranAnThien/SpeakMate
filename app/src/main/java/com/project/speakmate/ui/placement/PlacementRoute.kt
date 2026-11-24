package com.project.speakmate.ui.placement

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.ui.call.CallRoute
import com.project.speakmate.ui.placement.state.PlacementNavigation
import com.project.speakmate.ui.placement.state.PlacementPhase

@Composable
fun PlacementRoute(
    viewModel: PlacementViewModel,
    isCallDestination: Boolean,
    onChangeCallDestination: (Boolean) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val goHome by rememberUpdatedState(onHome)
    val shouldShowCall = state.phase == PlacementPhase.CALLING
    PlacementCallDestination(
        shouldShowCall = shouldShowCall,
        isCallDestination = isCallDestination,
        pendingNavigation = state.pendingNavigation,
        onChangeCallDestination = onChangeCallDestination
    )
    LaunchedEffect(state.pendingNavigation, viewModel, lifecycleOwner) {
        if (state.pendingNavigation != null) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                when (viewModel.consumePendingNavigation()) {
                    PlacementNavigation.HOME -> goHome()
                    null -> Unit
                }
            }
        }
    }
    BackHandler(enabled = !shouldShowCall || !isCallDestination, onBack = viewModel::exit)
    PlacementScreen(
        state = state,
        isCallDestination = isCallDestination,
        onExit = viewModel::exit,
        onContinueAssessment = viewModel::continueAssessment,
        onRetry = viewModel::retry,
        onRetryLoad = viewModel::retryLoad,
        onRetryGrading = viewModel::retryGrading,
        onRestartExpired = viewModel::restartExpiredAssessment,
        onStart = viewModel::start,
        onSkip = viewModel::skip,
        onResume = viewModel::resume,
        onRestart = viewModel::restart,
        onConfirmResultSaved = viewModel::confirmResultSaved,
        modifier = modifier,
        callContent = {
            val task = state.currentTask
            val scenario = state.scenario
            if (task != null && scenario != null) {
                CallRoute(
                    scenario = scenario,
                    activityType = CallActivityType.INITIAL_PLACEMENT,
                    onExit = viewModel::exit,
                    modifier = modifier,
                    onCallFinished = viewModel::submitCallResult,
                    activityContext = CallActivityContext.Assessment(
                        scenario = scenario,
                        activityType = CallActivityType.INITIAL_PLACEMENT,
                        communicativeGoal = task.communicativeGoal,
                        step = state.currentTaskNumber.takeIf { it > 0 },
                        targetLevel = task.targetLevel,
                        role = state.taskRole
                    )
                )
            }
        }
    )
}
