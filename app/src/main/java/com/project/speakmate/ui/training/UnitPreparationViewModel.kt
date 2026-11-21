package com.project.speakmate.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.ScenarioSelection
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingActivity
import com.project.speakmate.domain.model.TrainingActivityScenario
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.PronunciationRepository
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.domain.rules.TrainingProgressRules
import com.project.speakmate.domain.rules.VariantRules
import com.project.speakmate.ui.training.state.PreparationExampleUiState
import com.project.speakmate.ui.training.state.PreparationGrammarUiState
import com.project.speakmate.ui.training.state.PreparationOverviewUiState
import com.project.speakmate.ui.training.state.PreparationPatternUiState
import com.project.speakmate.ui.training.state.PreparationSection
import com.project.speakmate.ui.training.state.PreparationVocabularyUiState
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.ui.training.state.UnitPreparationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class UnitPreparationViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val pronunciationRepository: PronunciationRepository
) : ViewModel() {
    companion object {
        private const val PRONUNCIATION_SEPARATOR = " • "
    }

    private val _uiState = MutableStateFlow(UnitPreparationUiState())
    val uiState: StateFlow<UnitPreparationUiState> = _uiState.asStateFlow()
    private var preparationJob: Job? = null
    private var currentUnitId: String? = null

    override fun onCleared() {
        pronunciationRepository.release()
        super.onCleared()
    }

    fun getPreparation(unitId: String) {
        currentUnitId = unitId
        preparationJob?.cancel()
        preparationJob = viewModelScope.launch {
            try {
                authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                    _uiState.update { UnitPreparationUiState() }
                    if (uid == null) return@collectLatest
                    userRepository.observeUser(uid).collectLatest { user ->
                        val profile = checkNotNull(user)
                        check(profile.level != SpeakingLevel.NOT_ASSESSED)
                        val path = trainingRepository.getTrainingPath(profile.level).getOrThrow()
                        val progress = checkNotNull(profile.trainingProgress)
                        val location = path.goals.indices.firstNotNullOfOrNull { goalIndex ->
                            val unitIndex = path.goals[goalIndex].units.indexOfFirst { it.unitId == unitId }
                            if (unitIndex < 0) null else TrainingActivity.UnitActivity(goalIndex, unitIndex)
                        }
                        if (location == null || !TrainingProgressRules.canOpen(path, progress, location)) {
                            _uiState.update { UnitPreparationUiState(status = TrainingContentStatus.ERROR, isLocked = true) }
                            return@collectLatest
                        }
                        val unit = path.goals[location.goalIndex].units[location.unitIndex]
                        val scenario = trainingRepository.getScenario(unit.scenarioId).getOrThrow()
                        val imageUrl = trainingRepository.getScenarioImageUrl(unit.scenarioId).getOrThrow()
                        val vocabulary = unit.preparation.vocabulary.map { word ->
                            PreparationVocabularyUiState(
                                id = word.word,
                                word = word.word,
                                pronunciationLabel = listOf(word.phonetic, word.partOfSpeech).filter(String::isNotBlank).joinToString(PRONUNCIATION_SEPARATOR),
                                meaning = word.mean,
                                example = PreparationExampleUiState(word.example.sentence, word.example.translate)
                            )
                        }
                        val grammar = unit.preparation.grammarPoints.map { point ->
                            PreparationGrammarUiState(
                                id = point.title,
                                title = point.title,
                                explanation = point.explanation,
                                structure = point.structure,
                                usageNote = point.usage,
                                examples = point.examples.map { PreparationExampleUiState(it.sentence, it.translate) }
                            )
                        }
                        val patterns = unit.preparation.sentencePatterns.map { pattern ->
                            PreparationPatternUiState(
                                id = pattern.pattern,
                                pattern = pattern.pattern,
                                meaning = pattern.mean,
                                usage = pattern.usage,
                                examples = pattern.examples.map { PreparationExampleUiState(it.sentence, it.translate) }
                            )
                        }
                        _uiState.update {
                            UnitPreparationUiState(
                                status = TrainingContentStatus.CONTENT,
                                title = unit.title,
                                description = scenario.description,
                                overview = PreparationOverviewUiState(imageUrl, unit.communicativeFocus, requiredActions = unit.requiredActions.map { it.description }),
                                vocabulary = vocabulary,
                                grammar = grammar,
                                sentencePatterns = patterns,
                                activity = TrainingActivityScenario(
                                    scenario, location, path.targetLevel, unit.communicativeFocus, unit.requiredActions.toList(),
                                    path.targetLevel.name, AssessmentResult.PASS.name
                                )
                            )
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { UnitPreparationUiState(status = TrainingContentStatus.ERROR) }
            }
        }
    }

    fun retry() {
        currentUnitId?.let(::getPreparation)
    }

    fun stopObservingPreparation() {
        preparationJob?.cancel()
    }

    fun selectSection(section: PreparationSection) {
        _uiState.update { it.copy(section = section) }
    }

    fun prepareRetry() {
        val original = _uiState.value.activity ?: return
        if (!_uiState.value.canStartRoleplay) return
        val selection = VariantRules.selectForRetry(original, emptyList(), null, Random.Default)
        val scenario = when (selection) {
            is ScenarioSelection.Original -> selection.scenario
            is ScenarioSelection.Variant -> original.scenario.copy(promptTemplate = selection.variant.promptTemplate)
        }
        _uiState.update { it.copy(activity = original.copy(scenario = scenario)) }
    }

    fun playText(text: String) {
        if (_uiState.value.status == TrainingContentStatus.CONTENT) pronunciationRepository.playText(text)
    }

    fun stopPronunciation() {
        pronunciationRepository.stop()
    }
}
