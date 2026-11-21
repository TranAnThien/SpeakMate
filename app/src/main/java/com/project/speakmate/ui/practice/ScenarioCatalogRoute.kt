package com.project.speakmate.ui.practice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ScenarioCatalogRoute(
    onSelectScenario: (String) -> Unit,
    onReturnToPath: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PracticeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScenarioCatalogScreen(
        state = state,
        onSelectCategory = viewModel::selectCategory,
        onSelectScenario = onSelectScenario,
        onRetry = viewModel::loadCatalog,
        onDismissError = {},
        onReturnToPath = onReturnToPath,
        modifier = modifier
    )
}
