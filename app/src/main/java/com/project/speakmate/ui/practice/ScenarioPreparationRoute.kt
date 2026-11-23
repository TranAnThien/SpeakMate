package com.project.speakmate.ui.practice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.ui.call.CallRoute

@Composable
fun ScenarioPreparationRoute(
    isCalling: Boolean,
    onBack: () -> Unit,
    onStartCall: () -> Unit,
    onPracticeCompleted: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScenarioPreparationViewModel = hiltViewModel(),
    onCallCancelled: () -> Unit = onBack,
    noticeContent: @Composable () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scenario = state.scenario
    val lifecycleOwner = LocalLifecycleOwner.current
    val startCall by rememberUpdatedState(onStartCall)
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (!isCalling) viewModel.cancelStart() }
    LaunchedEffect(state.isStartReady, viewModel, lifecycleOwner) {
        if (!isCalling && state.isStartReady) lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (viewModel.consumeStartReady()) startCall()
        }
    }
    if (isCalling && scenario != null) {
        CallRoute(scenario, CallActivityType.FREE_PRACTICE, onBack, modifier,
            onCallFinished = { session ->
                if (session.outcome == CallOutcome.CANCELLED) onCallCancelled() else onBack()
            },
            onPracticeCompleted = { onPracticeCompleted(it, scenario.name) })
    } else {
        ScenarioPreparationScreen(
            state = state,
            onBack = onBack,
            onStartCall = {
                if (!isCalling) viewModel.prepareCall()
            },
            onRetry = viewModel::loadScenarioDetails,
            modifier = modifier,
            noticeContent = noticeContent
        )
    }
}
