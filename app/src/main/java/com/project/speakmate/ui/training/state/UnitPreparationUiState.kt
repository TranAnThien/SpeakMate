package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.PronunciationPlaybackState
import com.project.speakmate.domain.model.TrainingActivityScenario

@Immutable
data class UnitPreparationUiState(
    val status: TrainingContentStatus = TrainingContentStatus.LOADING,
    val section: PreparationSection = PreparationSection.OVERVIEW,
    val title: String = "",
    val description: String = "",
    val overview: PreparationOverviewUiState = PreparationOverviewUiState(),
    val vocabulary: List<PreparationVocabularyUiState> = emptyList(),
    val grammar: List<PreparationGrammarUiState> = emptyList(),
    val sentencePatterns: List<PreparationPatternUiState> = emptyList(),
    val activity: TrainingActivityScenario? = null,
    val isLocked: Boolean = false,
    val isReplay: Boolean = false,
    val isStarting: Boolean = false,
    val isStartReady: Boolean = false,
    val playback: PronunciationPlaybackState = PronunciationPlaybackState(),
    val activeCallActivity: TrainingActivityScenario? = null,
    val activeCallIsReplay: Boolean = false
) {
    val canStartRoleplay: Boolean
        get() = status == TrainingContentStatus.CONTENT && activity != null
}
