package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class UnitPreparationUiState(
    val status: TrainingContentStatus = TrainingContentStatus.LOADING,
    val errorMessage: String = "",
    val section: PreparationSection = PreparationSection.OVERVIEW,
    val contextLabel: String = "",
    val title: String = "",
    val description: String = "",
    val overview: PreparationOverviewUiState = PreparationOverviewUiState(),
    val vocabulary: List<PreparationVocabularyUiState> = emptyList(),
    val grammar: List<PreparationGrammarUiState> = emptyList(),
    val sentencePatterns: List<PreparationPatternUiState> = emptyList()
)
