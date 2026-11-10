package com.project.speakmate.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.TrainingGoal
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.ui.training.state.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TrainingViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingPathUiState())
    val uiState: StateFlow<TrainingPathUiState> = _uiState.asStateFlow()

    private val _preparationState = MutableStateFlow(UnitPreparationUiState())
    val preparationState: StateFlow<UnitPreparationUiState> = _preparationState.asStateFlow()

    private var currentUser: User? = null

    fun updateUserData(currentUser: User?) {
        this.currentUser = currentUser
        viewModelScope.launch {
            if (currentUser == null) {
                _uiState.update { it.copy(status = TrainingContentStatus.LOADING) }
                return@launch
            }

            if (currentUser.trainingProgress == null) {
                _uiState.update {
                    it.copy(
                        status = TrainingContentStatus.ERROR,
                        errorMessage = "Hồ sơ trình độ ${currentUser.level.name} chưa có trainingProgress. " +
                            "Hãy tạo currentGoalOrder = 0, currentUnitOrder = 0, stage = UNIT."
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(status = TrainingContentStatus.LOADING) }
            val currentPath: TrainingPath = trainingRepository.getTrainingPath(currentUser.level).getOrElse { error ->
                _uiState.update { state ->
                    state.copy(
                        status = TrainingContentStatus.ERROR,
                        errorMessage = error.message ?: "Không thể nạp cấu hình lộ trình từ Firestore."
                    )
                }
                return@launch
            }
            val progress = currentUser.trainingProgress

            val progressError = validateProgress(currentPath, progress)
            if (progressError != null) {
                _uiState.update { state -> state.copy(status = TrainingContentStatus.ERROR, errorMessage = progressError) }
                return@launch
            }

            val goalUiStates = currentPath.goals.mapIndexed { index, goal ->
                mapGoalToUiState(goal, index, progress.currentGoalOrder, progress.currentUnitOrder, progress.stage)
            }

            _uiState.update {
                it.copy(
                    status = TrainingContentStatus.CONTENT,
                    level = currentUser.level.name,
                    goals = goalUiStates,
                    canContinue = true
                )
            }
        }
    }

    private fun mapGoalToUiState(
        goal: TrainingGoal,
        goalIndex: Int,
        currentGoalOrder: Int,
        currentUnitOrder: Int,
        stage: TrainingStage
    ): TrainingGoalUiState {
        val goalStatus = when {
            goalIndex < currentGoalOrder -> TrainingProgressStatus.COMPLETED
            goalIndex == currentGoalOrder -> TrainingProgressStatus.CURRENT
            else -> TrainingProgressStatus.LOCKED
        }

        val unitUiStates = goal.units.mapIndexed { uIndex, unit ->
            val isCompleted = goalIndex < currentGoalOrder ||
                (goalIndex == currentGoalOrder && (uIndex < currentUnitOrder || stage == TrainingStage.GOAL_ASSESSMENT))
            val isCurrent = goalIndex == currentGoalOrder && uIndex == currentUnitOrder && stage == TrainingStage.UNIT

            val unitStatus = when {
                isCompleted -> TrainingProgressStatus.COMPLETED
                isCurrent -> TrainingProgressStatus.CURRENT
                else -> TrainingProgressStatus.LOCKED
            }

            TrainingUnitUiState(
                id = unit.unitId,
                title = unit.title,
                description = unit.communicativeFocus,
                status = unitStatus,
                isAssessment = false,
                canOpen = unitStatus != TrainingProgressStatus.LOCKED
            )
        }

        val assessmentStatus = when {
            goalIndex < currentGoalOrder -> TrainingProgressStatus.COMPLETED
            goalIndex == currentGoalOrder && stage == TrainingStage.GOAL_ASSESSMENT -> TrainingProgressStatus.CURRENT
            else -> TrainingProgressStatus.LOCKED
        }
        val assessment = TrainingUnitUiState(
            id = "assessment:${goal.goalId}",
            title = goal.goalAssessment.title,
            description = "Hoàn thành bài kiểm tra để mở khóa mục tiêu tiếp theo.",
            status = assessmentStatus,
            isAssessment = true,
            canOpen = assessmentStatus != TrainingProgressStatus.LOCKED
        )

        return TrainingGoalUiState(
            id = goal.goalId,
            label = "Mục tiêu ${goalIndex + 1}",
            title = goal.title,
            summary = goal.canDoDescription,
            unitsHeading = "Danh sách bài học",
            status = goalStatus,
            isExpanded = goalIndex == currentGoalOrder,
            units = unitUiStates + assessment
        )
    }

    fun toggleGoalExpansion(goalId: String) {
        _uiState.update { state ->
            val updatedGoals = state.goals.map { goal ->
                if (goal.id == goalId) goal.copy(isExpanded = !goal.isExpanded) else goal
            }
            state.copy(goals = updatedGoals)
        }
    }

    fun loadUnitPreparation(unitId: String) {
        viewModelScope.launch {
            _preparationState.update { UnitPreparationUiState(status = TrainingContentStatus.LOADING) }
            val unit = trainingRepository.getTrainingUnit(unitId).getOrElse { error ->
                _preparationState.update { UnitPreparationUiState(status = TrainingContentStatus.ERROR, errorMessage = error.message.orEmpty()) }
                return@launch
            }
            val scenario = trainingRepository.getScenario(unit.scenarioId).getOrElse { error ->
                _preparationState.update { UnitPreparationUiState(status = TrainingContentStatus.ERROR, errorMessage = error.message.orEmpty()) }
                return@launch
            }
            val imageUrl = trainingRepository.getScenarioImageUrl(unit.scenarioId).getOrElse { error ->
                _preparationState.update { UnitPreparationUiState(status = TrainingContentStatus.ERROR, errorMessage = error.message.orEmpty()) }
                return@launch
            }

            val vocabList = unit.preparation.vocabulary.map { vocab ->
                PreparationVocabularyUiState(
                    id = vocab.word,
                    word = vocab.word,
                    pronunciationLabel = "${vocab.phonetic} • ${vocab.partOfSpeech}",
                    meaning = vocab.mean,
                    example = PreparationExampleUiState(vocab.example.sentence, vocab.example.translate)
                )
            }

            val grammarList = unit.preparation.grammarPoints.map { grammar ->
                PreparationGrammarUiState(
                    id = grammar.title,
                    title = grammar.title,
                    explanation = grammar.explanation,
                    structure = grammar.structure,
                    example = grammar.examples.firstOrNull()?.let { PreparationExampleUiState(it.sentence, it.translate) }
                        ?: PreparationExampleUiState(),
                    usageNote = grammar.usage
                )
            }

            val patternList = unit.preparation.sentencePatterns.map { pattern ->
                PreparationPatternUiState(
                    id = pattern.pattern,
                    label = "Mẫu câu",
                    pattern = pattern.pattern,
                    meaning = pattern.mean,
                    usage = pattern.usage,
                    example = pattern.examples.firstOrNull()?.let { PreparationExampleUiState(it.sentence, it.translate) }
                        ?: PreparationExampleUiState()
                )
            }

            _preparationState.update {
                it.copy(
                    status = TrainingContentStatus.CONTENT,
                    section = PreparationSection.OVERVIEW,
                    contextLabel = "Học liệu Bài học",
                    title = unit.title,
                    description = unit.communicativeFocus,
                    overview = PreparationOverviewUiState(
                        scenarioTitle = scenario.name,
                        scenarioRoles = scenario.description,
                        imageUrl = imageUrl,
                        communicativeFocus = unit.communicativeFocus,
                        modules = listOf(
                            PreparationModuleUiState(PreparationSection.VOCABULARY, "Từ vựng", "${vocabList.size} mục cần dùng"),
                            PreparationModuleUiState(PreparationSection.GRAMMAR, "Ngữ pháp", "${grammarList.size} điểm ngữ pháp"),
                            PreparationModuleUiState(PreparationSection.SENTENCE_PATTERNS, "Mẫu câu", "${patternList.size} mẫu câu")
                        ),
                        requiredActions = unit.requiredActions.map { it.description }
                    ),
                    vocabulary = vocabList,
                    grammar = grammarList,
                    sentencePatterns = patternList
                )
            }
        }
    }

    fun selectPreparationSection(section: PreparationSection) {
        _preparationState.update { it.copy(section = section) }
    }

    fun retry() = updateUserData(currentUser)

    private fun validateProgress(path: TrainingPath, progress: com.project.speakmate.domain.model.TrainingProgress): String? {
        val goal = path.goals.getOrNull(progress.currentGoalOrder)
            ?: return "trainingProgress.currentGoalOrder = ${progress.currentGoalOrder} không tồn tại trong Path ${path.pathId}."
        if (progress.stage == TrainingStage.UNIT && goal.units.getOrNull(progress.currentUnitOrder) == null) {
            return "trainingProgress.currentUnitOrder = ${progress.currentUnitOrder} không tồn tại trong Goal ${goal.goalId}."
        }
        return null
    }
}
