package com.project.speakmate.ui.profile.savedvocab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SavedVocabRoute(
    onNavigateBack: () -> Unit,
    onStartPractice: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedVocabViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.leaveScreen() }
    DisposableEffect(viewModel) { onDispose { viewModel.leaveScreen() } }
    BackHandler { viewModel.leaveScreen(); onNavigateBack() }
    SavedVocabScreen(
        uiState = uiState,
        onNavigateBack = { viewModel.leaveScreen(); onNavigateBack() },
        onSearchQueryChange = viewModel::updateSearchQuery,
        onClearSearch = { viewModel.updateSearchQuery("") },
        onPlayClick = { viewModel.playWord(it.word, it.partOfSpeech) },
        onDeleteClick = { viewModel.removeVocabulary(it.word, it.partOfSpeech) },
        modifier = modifier,
        onRetry = viewModel::retry,
        onRetryDelete = viewModel::retryDelete,
        onDismissDeleteError = viewModel::clearError,
        onStartPractice = { viewModel.leaveScreen(); onStartPractice() }
    )
}
