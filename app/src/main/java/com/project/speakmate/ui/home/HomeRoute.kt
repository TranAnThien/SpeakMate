package com.project.speakmate.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeRoute(
    onStartAssessment: () -> Unit,
    onContinuePath: () -> Unit,
    onStartReassessment: () -> Unit,
    onSelectScenario: () -> Unit,
    onViewProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onStartAssessment = onStartAssessment,
        onContinuePath = onContinuePath,
        onStartReassessment = onStartReassessment,
        onReviewPath = onContinuePath,
        onSelectScenario = onSelectScenario,
        onRetry = viewModel::retry,
        onViewProfile = onViewProfile,
        modifier = modifier
    )
}
