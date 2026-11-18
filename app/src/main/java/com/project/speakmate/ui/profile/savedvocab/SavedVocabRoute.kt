package com.project.speakmate.ui.profile.savedvocab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SavedVocabRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedVocabViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.stopPronunciation() }
    SavedVocabScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onClearSearch = { viewModel.updateSearchQuery("") },
        onPlayClick = { viewModel.playWord(it.word) },
        onDeleteClick = { viewModel.removeVocabulary(it.word, it.partOfSpeech) },
        modifier = modifier
    )
}
