package com.project.speakmate.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import com.project.speakmate.ui.components.BackAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
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
    viewModel: UnitPreparationViewModel = hiltViewModel(),
    noticeContent: @Composable () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val startRoleplay by rememberUpdatedState(onStartRoleplay)
    LaunchedEffect(state.isStartReady, viewModel, lifecycleOwner) {
        if (state.isStartReady) lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (viewModel.consumeStartReady()) startRoleplay()
        }
    }
    LaunchedEffect(unitId, viewModel) { viewModel.getPreparation(unitId) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.stopPronunciation(); viewModel.cancelStart() }
    if (state.status == TrainingContentStatus.CONTENT) {
        val vocabularyTitle = stringResource(R.string.preparation_vocabulary)
        val grammarTitle = stringResource(R.string.preparation_grammar)
        val patternsTitle = stringResource(R.string.preparation_patterns)
        val vocabularyCount = pluralStringResource(R.plurals.preparation_vocabulary_items, state.vocabulary.size, state.vocabulary.size)
        val grammarCount = pluralStringResource(R.plurals.preparation_grammar_items, state.grammar.size, state.grammar.size)
        val patternsCount = pluralStringResource(R.plurals.preparation_pattern_items, state.sentencePatterns.size, state.sentencePatterns.size)
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
            onStartRoleplay = viewModel::prepareStart,
            onPlayWord = viewModel::playText,
            onPlayPattern = viewModel::playText,
            onPlayExample = viewModel::playText,
            modifier = modifier,
            noticeContent = noticeContent
        )
    } else {
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
        noticeContent()
        }
    }
}
