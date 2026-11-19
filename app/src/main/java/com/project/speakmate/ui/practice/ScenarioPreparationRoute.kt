package com.project.speakmate.ui.practice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.ui.call.CallRoute

@Composable
fun ScenarioPreparationRoute(
    isCalling: Boolean,
    onBack: () -> Unit,
    onStartCall: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScenarioPreparationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scenario = state.scenario
    if (isCalling && scenario != null) {
        CallRoute(scenario, CallActivityType.FREE_PRACTICE, onBack, modifier)
    } else {
        ScenarioPreparationScreen(
            state = state,
            onBack = onBack,
            onStartCall = {
                if (!isCalling && viewModel.uiState.value.scenario != null) onStartCall()
            },
            onRetry = viewModel::loadScenarioDetails,
            modifier = modifier
        )
    }
}
