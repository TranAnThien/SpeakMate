package com.project.speakmate.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingActivity
import com.project.speakmate.domain.model.TrainingActivityScenario
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.PronunciationRepository
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.domain.rules.TrainingProgressRules
import com.project.speakmate.ui.training.state.PreparationExampleUiState
import com.project.speakmate.ui.training.state.PreparationGrammarUiState
import com.project.speakmate.ui.training.state.PreparationModuleUiState
import com.project.speakmate.ui.training.state.PreparationOverviewUiState
import com.project.speakmate.ui.training.state.PreparationPatternUiState
import com.project.speakmate.ui.training.state.PreparationSection
import com.project.speakmate.ui.training.state.PreparationVocabularyUiState
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.ui.training.state.UnitPreparationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class UnitPreparationViewModel
@Inject
constructor(
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
    private var preparedUserId: String? = null
    private var shouldRequireServer = false
    private var hasRequestedCall = false

    init {
        viewModelScope.launch {
            pronunciationRepository.playback.collect { playback -> _uiState.update { it.copy(playback = playback) } }
        }
    }

    override fun onCleared() {
        pronunciationRepository.release()
        super.onCleared()
    }

    fun getPreparation(unitId: String, requireServer: Boolean = false) {
        hasRequestedCall = false
        _uiState.update { it.copy(activeCallActivity = null, activeCallIsReplay = false) }
        loadPreparation(unitId = unitId, requireServer = requireServer)
    }

    private fun loadPreparation(unitId: String, requireServer: Boolean) {
        shouldRequireServer = requireServer
        val selectedSection = _uiState.value.section
        val preserveContent =
            currentUnitId == unitId &&
                _uiState.value.status == TrainingContentStatus.CONTENT &&
                preparedUserId != null
        if (!preserveContent) {
            preparedUserId = null
            cancelStart()
            _uiState.update { it.copy(status = TrainingContentStatus.LOADING) }
        }
        stopPronunciation()
        currentUnitId = unitId
        preparationJob?.cancel()
        preparationJob =
            viewModelScope.launch {
                try {
                    authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                        if (uid != preparedUserId) {
                            preparedUserId = null
                            cancelStart()
                            stopPronunciation()
                            _uiState.update { UnitPreparationUiState() }
                        }
                        if (uid == null) return@collectLatest
                        userRepository.observeUser(uid).collectLatest { user ->
                            val hasContent =
                                _uiState.value.status == TrainingContentStatus.CONTENT && preparedUserId == uid
                            if (!hasContent) {
                                cancelStart()
                                preparedUserId = null
                                _uiState.update { it.copy(status = TrainingContentStatus.LOADING) }
                            }
                            val profile =
                                checkNotNull(
                                    if (requireServer)
                                        userRepository.getUser(userId = uid, requireServer = true).getOrThrow()
                                    else user
                                )
                            check(profile.level != SpeakingLevel.NOT_ASSESSED)
                            val path = trainingRepository.getTrainingPath(profile.level, requireServer).getOrThrow()
                            currentCoroutineContext().ensureActive()
                            check(authRepository.requireCurrentUserId() == uid && path.targetLevel == profile.level)
                            val progress = checkNotNull(profile.trainingProgress)
                            val location =
                                path.goals.indices.firstNotNullOfOrNull { goalIndex ->
                                    val unitIndex = path.goals[goalIndex].units.indexOfFirst { it.unitId == unitId }
                                    if (unitIndex < 0) null else TrainingActivity.UnitActivity(goalIndex, unitIndex)
                                }
                            if (location == null ||
                                !TrainingProgressRules.canOpen(path = path, progress = progress, activity = location)) {
                                cancelStart()
                                preparedUserId = null
                                _uiState.update {
                                    UnitPreparationUiState(status = TrainingContentStatus.ERROR, isLocked = true)
                                }
                                return@collectLatest
                            }
                            val unit = path.goals[location.goalIndex].units[location.unitIndex]
                            val scenario = trainingRepository.getScenario(unit.scenarioId, requireServer).getOrThrow()
                            check(
                                scenario.scenarioId == unit.scenarioId &&
                                    scenario.name.isNotBlank() &&
                                    scenario.promptTemplate.isNotBlank()
                            )
                            val imageUrl =
                                trainingRepository.getScenarioImageUrl(unit.scenarioId).getOrElse {
                                    if (it is CancellationException) throw it else ""
                                }
                            val vocabulary =
                                unit.preparation.vocabulary
                                    .map { word ->
                                        PreparationVocabularyUiState(
                                            id = word.word + word.partOfSpeech,
                                            word = word.word,
                                            pronunciationLabel =
                                                listOf(word.phonetic, word.partOfSpeech)
                                                    .filter(String::isNotBlank)
                                                    .joinToString(PRONUNCIATION_SEPARATOR),
                                            meaning = word.mean,
                                            example =
                                                PreparationExampleUiState(
                                                    word.example.sentence,
                                                    word.example.translate
                                                )
                                        )
                                    }
                                    .distinctBy { it.id }
                            val grammar =
                                unit.preparation.grammarPoints
                                    .map { point ->
                                        PreparationGrammarUiState(
                                            id = point.title,
                                            title = point.title,
                                            explanation = point.explanation,
                                            structure = point.structure,
                                            usageNote = point.usage,
                                            examples =
                                                point.examples.map {
                                                    PreparationExampleUiState(it.sentence, it.translate)
                                                }
                                        )
                                    }
                                    .distinctBy { it.id }
                            val patterns =
                                unit.preparation.sentencePatterns
                                    .map { pattern ->
                                        PreparationPatternUiState(
                                            id = pattern.pattern,
                                            pattern = pattern.pattern,
                                            meaning = pattern.mean,
                                            usage = pattern.usage,
                                            examples =
                                                pattern.examples.map {
                                                    PreparationExampleUiState(it.sentence, it.translate)
                                                }
                                        )
                                    }
                                    .distinctBy { it.id }
                            currentCoroutineContext().ensureActive()
                            check(authRepository.requireCurrentUserId() == uid)
                            preparedUserId = uid
                            val isReplay =
                                TrainingProgressRules.isReplay(
                                    path = path,
                                    progress = progress,
                                    activity = location
                                )
                            val activity =
                                TrainingActivityScenario(
                                    scenario = scenario,
                                    activity = location,
                                    targetLevel = path.targetLevel,
                                    communicativeGoal = unit.communicativeFocus,
                                    requiredActions = unit.requiredActions.toList(),
                                    difficultyCode = path.targetLevel.name,
                                    passConditionCode = AssessmentResult.PASS.name
                                )
                            val shouldLockCall = hasRequestedCall
                            _uiState.update { current ->
                                val keepContentState = current.status == TrainingContentStatus.CONTENT
                                UnitPreparationUiState(
                                    status = TrainingContentStatus.CONTENT,
                                    section = if (keepContentState) current.section else selectedSection,
                                    playback = pronunciationRepository.playback.value,
                                    isReplay = isReplay,
                                    isStarting = if (keepContentState) current.isStarting else false,
                                    isStartReady = if (keepContentState) current.isStartReady else false,
                                    title = unit.title,
                                    description = scenario.description,
                                    overview =
                                        PreparationOverviewUiState(
                                            imageUrl = imageUrl,
                                            communicativeFocus = unit.communicativeFocus,
                                            modules =
                                                listOf(
                                                    PreparationModuleUiState(
                                                        section = PreparationSection.VOCABULARY,
                                                        itemCount = unit.preparation.vocabulary.size
                                                    ),
                                                    PreparationModuleUiState(
                                                        section = PreparationSection.GRAMMAR,
                                                        itemCount = unit.preparation.grammarPoints.size
                                                    ),
                                                    PreparationModuleUiState(
                                                        section = PreparationSection.SENTENCE_PATTERNS,
                                                        itemCount = unit.preparation.sentencePatterns.size
                                                    )
                                                ),
                                            requiredActions = unit.requiredActions.map { it.description }
                                        ),
                                    vocabulary = vocabulary,
                                    grammar = grammar,
                                    sentencePatterns = patterns,
                                    activity = activity,
                                    activeCallActivity = if (shouldLockCall) activity else current.activeCallActivity,
                                    activeCallIsReplay = if (shouldLockCall) isReplay else current.activeCallIsReplay
                                )
                            }
                            if (shouldLockCall) this@launch.cancel()
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    preparedUserId = null
                    _uiState.update { UnitPreparationUiState(status = TrainingContentStatus.ERROR) }
                }
            }
    }

    fun prepareCall(unitId: String, isRetry: Boolean) {
        val state = _uiState.value
        if (state.activeCallActivity != null) return
        hasRequestedCall = true
        if (currentUnitId == unitId && state.canStartRoleplay && (!isRetry || shouldRequireServer)) {
            preparationJob?.cancel()
            _uiState.update {
                it.copy(
                    activeCallActivity = state.activity,
                    activeCallIsReplay = state.isReplay
                )
            }
            return
        }
        val needsServerReload = isRetry && !shouldRequireServer
        if (preparationJob?.isActive != true || currentUnitId != unitId || needsServerReload) {
            loadPreparation(unitId = unitId, requireServer = true)
        }
    }

    fun retry() {
        currentUnitId?.let { loadPreparation(unitId = it, requireServer = shouldRequireServer) }
    }

    fun selectSection(section: PreparationSection) {
        stopPronunciation()
        _uiState.update { it.copy(section = section) }
    }

    fun playText(text: String) {
        if (_uiState.value.status == TrainingContentStatus.CONTENT) pronunciationRepository.playText(text)
    }

    fun prepareStart() {
        val state = _uiState.value
        val activity = state.activity ?: return
        val uid = preparedUserId ?: return
        if (state.isStarting || state.isStartReady || state.isLocked || !state.canStartRoleplay) return
        val currentUid =
            try {
                authRepository.requireCurrentUserId()
            } catch (_: IllegalStateException) {
                return
            }
        if (currentUid != uid ||
            activity.targetLevel == SpeakingLevel.NOT_ASSESSED ||
            activity.scenario.scenarioId.isBlank() ||
            activity.scenario.name.isBlank() ||
            activity.scenario.promptTemplate.isBlank())
            return
        stopPronunciation()
        _uiState.update { it.copy(isStarting = true, isStartReady = true) }
    }

    fun consumeStartReady(): Boolean {
        val state = _uiState.value
        val uid =
            try {
                authRepository.requireCurrentUserId()
            } catch (_: IllegalStateException) {
                return false
            }
        return uid == preparedUserId &&
            state.isStartReady &&
            _uiState.compareAndSet(state, state.copy(isStartReady = false))
    }

    fun cancelStart() {
        _uiState.update { it.copy(isStarting = false, isStartReady = false) }
    }

    fun stopPronunciation() {
        pronunciationRepository.stop()
    }
}
