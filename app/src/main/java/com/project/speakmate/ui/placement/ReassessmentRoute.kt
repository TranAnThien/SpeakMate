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
import com.project.speakmate.domain.model.PlacementRole
import com.project.speakmate.domain.model.ReassessmentStep
import com.project.speakmate.ui.call.CallRoute
import com.project.speakmate.ui.placement.state.ReassessmentNavigation
import com.project.speakmate.ui.placement.state.ReassessmentPhase

@Composable
fun ReassessmentRoute(
    viewModel: ReassessmentViewModel,
    isCallDestination: Boolean,
    onChangeCallDestination: (Boolean) -> Unit,
    onReturnToPath: () -> Unit,
    onOpenPromotedPath: () -> Unit,
    onHome: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val shouldShowCall = state.phase == ReassessmentPhase.CALLING
    PlacementCallDestination(
        shouldShowCall = shouldShowCall,
        isCallDestination = isCallDestination,
        pendingNavigation = state.pendingNavigation,
        onChangeCallDestination = onChangeCallDestination
    )
    val openHome by rememberUpdatedState(onHome)
    val openPath by rememberUpdatedState(onOpenPromotedPath)
    val returnToPath by rememberUpdatedState(onReturnToPath)
    val openProfile by rememberUpdatedState(onOpenProfile)
    LaunchedEffect(state.pendingNavigation, viewModel, lifecycleOwner) {
        if (state.pendingNavigation != null) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                when (viewModel.consumePendingNavigation()) {
                    ReassessmentNavigation.OPEN_HOME -> openHome()
                    ReassessmentNavigation.OPEN_PROMOTED_PATH -> openPath()
                    ReassessmentNavigation.RETURN_TO_PATH -> returnToPath()
                    ReassessmentNavigation.OPEN_PROFILE -> openProfile()
                    null -> Unit
                }
            }
        }
    }
    val exitAssessment = viewModel::exit
    BackHandler(enabled = !shouldShowCall || !isCallDestination, onBack = exitAssessment)
    ReassessmentScreen(
        state = state,
        isCallDestination = isCallDestination,
        onExit = viewModel::exit,
        onContinueAssessment = viewModel::continueAssessment,
        onRetryTask = viewModel::retryTask,
        onRetryLoad = viewModel::retryLoad,
        onRetryGrading = viewModel::retryGrading,
        onStart = viewModel::start,
        onRetrySave = viewModel::retrySave,
        onOpenProfile = viewModel::openProfile,
        modifier = modifier,
        callContent = {
            val task = state.assessment?.currentTask
            val scenario = state.scenario
            if (task != null && scenario != null) {
                CallRoute(
                    scenario = scenario,
                    activityType = CallActivityType.REASSESSMENT,
                    onExit = viewModel::exit,
                    modifier = modifier,
                    activityContext =
                        CallActivityContext.Assessment(
                            scenario = scenario,
                            activityType = CallActivityType.REASSESSMENT,
                            communicativeGoal = task.communicativeGoal,
                            step = state.assessment?.step?.ordinal?.plus(1),
                            targetLevel = task.targetLevel,
                            role =
                                if (state.assessment?.step == ReassessmentStep.NEXT_PROBE) PlacementRole.PROBE
                                else PlacementRole.CONFIRMATION
                        ),
                    onCallFinished = viewModel::submitCallResult
                )
            }
        }
    )
}
