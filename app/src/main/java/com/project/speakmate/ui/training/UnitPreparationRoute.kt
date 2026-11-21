package com.project.speakmate.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.R
import com.project.speakmate.ui.training.components.TrainingUnavailableContent
import com.project.speakmate.ui.training.state.PreparationModuleUiState
import com.project.speakmate.ui.training.state.PreparationSection
import com.project.speakmate.ui.training.state.TrainingContentStatus

@Composable
fun UnitPreparationRoute(
    unitId: String,
    onBack: () -> Unit,
    onStartRoleplay: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UnitPreparationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(unitId, viewModel) { viewModel.getPreparation(unitId) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.stopPronunciation() }
    if (state.status == TrainingContentStatus.CONTENT) {
        val vocabularyTitle = stringResource(R.string.preparation_vocabulary)
        val grammarTitle = stringResource(R.string.preparation_grammar)
        val patternsTitle = stringResource(R.string.preparation_patterns)
        val vocabularyCount = stringResource(R.string.preparation_vocabulary_count, state.vocabulary.size)
        val grammarCount = stringResource(R.string.preparation_grammar_count, state.grammar.size)
        val patternsCount = stringResource(R.string.preparation_patterns_count, state.sentencePatterns.size)
        val contextLabel = stringResource(R.string.preparation_unit_context)
        val displayState = remember(state, vocabularyTitle, grammarTitle, patternsTitle, vocabularyCount, grammarCount, patternsCount, contextLabel) {
            state.copy(
                contextLabel = contextLabel,
                overview = state.overview.copy(modules = listOf(
                    PreparationModuleUiState(PreparationSection.VOCABULARY, vocabularyTitle, vocabularyCount),
                    PreparationModuleUiState(PreparationSection.GRAMMAR, grammarTitle, grammarCount),
                    PreparationModuleUiState(PreparationSection.SENTENCE_PATTERNS, patternsTitle, patternsCount)
                ))
            )
        }
        UnitPreparationScreen(
            state = displayState,
            onBack = onBack,
            onSelectSection = viewModel::selectSection,
            onStartRoleplay = {
                if (viewModel.uiState.value.canStartRoleplay) {
                    viewModel.stopPronunciation()
                    onStartRoleplay()
                }
            },
            onPlayWord = viewModel::playText,
            onPlayPattern = viewModel::playText,
            onPlayExample = viewModel::playText,
            modifier = modifier
        )
    } else {
        TrainingUnavailableContent(
            isLoading = state.status == TrainingContentStatus.LOADING,
            onRetry = viewModel::retry,
            errorMessage = if (state.isLocked) stringResource(R.string.preparation_unit_locked) else "",
            loadingTitleRes = R.string.preparation_loading_title,
            loadingDescriptionRes = R.string.preparation_loading_description,
            errorTitleRes = R.string.preparation_error_title,
            errorDescriptionRes = R.string.preparation_error_description,
            modifier = modifier
        )
    }
}
