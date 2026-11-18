package com.project.speakmate.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.ContentStatus
import com.project.speakmate.domain.model.CriterionJudgement
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.RequiredActionJudgement
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingEvaluation
import com.project.speakmate.domain.model.TrainingActivity
import com.project.speakmate.domain.model.TrainingGoal
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingProgressDecision
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.TrainingUnit
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.domain.rules.TrainingProgressRules
import com.project.speakmate.ui.training.state.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TrainingViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    constructor(trainingRepository: TrainingRepository) : this(
        trainingRepository,
        object : HistoryRepository {
            override suspend fun savePracticeAttempt(attempt: PracticeAttempt): Result<Unit> = Result.success(Unit)
            override suspend fun saveAttemptAndProgress(
                attempt: PracticeAttempt,
                newProgress: TrainingProgress?
            ): Result<Unit> = Result.success(Unit)
            override fun getPracticeHistoryList() = kotlinx.coroutines.flow.flowOf(emptyList<PracticeAttempt>())
            override suspend fun getSessionDetail(attemptId: String): Result<PracticeAttempt> = Result.failure(Exception())
        }
    )

    private val _uiState = MutableStateFlow(TrainingPathUiState())
    val uiState: StateFlow<TrainingPathUiState> = _uiState.asStateFlow()

    private val _preparationState = MutableStateFlow(UnitPreparationUiState())
    val preparationState: StateFlow<UnitPreparationUiState> = _preparationState.asStateFlow()

    private val _unitResultState = MutableStateFlow<UnitResultUiState?>(null)
    val unitResultState: StateFlow<UnitResultUiState?> = _unitResultState.asStateFlow()

    var currentLoadedScenario: Scenario? = null
        private set
    var currentLoadedUnit: TrainingUnit? = null
        private set

    private var currentUser: User? = null
    private var currentPath: TrainingPath? = null

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
            this@TrainingViewModel.currentPath = currentPath
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
                currentLoadedUnit = null
                currentLoadedScenario = null
                _preparationState.update { UnitPreparationUiState(status = TrainingContentStatus.ERROR, errorMessage = error.message.orEmpty()) }
                return@launch
            }
            val scenario = trainingRepository.getScenario(unit.scenarioId).getOrElse { error ->
                currentLoadedUnit = null
                currentLoadedScenario = null
                _preparationState.update { UnitPreparationUiState(status = TrainingContentStatus.ERROR, errorMessage = error.message.orEmpty()) }
                return@launch
            }
            currentLoadedUnit = unit
            currentLoadedScenario = scenario
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
                    description = scenario.description,
                    overview = PreparationOverviewUiState(
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

    fun resolveContentStatus(
        goalIndex: Int,
        unitIndex: Int,
        progress: TrainingProgress
    ): ContentStatus {
        return when {
            goalIndex < progress.currentGoalOrder -> ContentStatus.COMPLETED
            goalIndex > progress.currentGoalOrder -> ContentStatus.LOCKED
            else -> {
                when {
                    unitIndex >= 0 -> {
                        when {
                            unitIndex < progress.currentUnitOrder -> ContentStatus.COMPLETED
                            unitIndex == progress.currentUnitOrder -> {
                                if (progress.stage == TrainingStage.GOAL_ASSESSMENT || progress.stage == TrainingStage.PATH_COMPLETED) {
                                    ContentStatus.COMPLETED
                                } else {
                                    ContentStatus.CURRENT
                                }
                            }
                            else -> ContentStatus.LOCKED
                        }
                    }
                    else -> {
                        when (progress.stage) {
                            TrainingStage.UNIT -> ContentStatus.LOCKED
                            TrainingStage.GOAL_ASSESSMENT -> ContentStatus.CURRENT
                            TrainingStage.PATH_COMPLETED -> ContentStatus.COMPLETED
                        }
                    }
                }
            }
        }
    }

    fun onUnitCallFinished(session: CallSessionState) {
        val unit = currentLoadedUnit ?: return
        val user = currentUser ?: return
        val progress = user.trainingProgress ?: return
        val path = currentPath ?: return

        val goalIndex = path.goals.indexOfFirst { goal -> goal.units.any { it.unitId == unit.unitId } }
        if (goalIndex == -1) return
        val unitIndex = path.goals[goalIndex].units.indexOfFirst { it.unitId == unit.unitId }
        if (unitIndex == -1) return
        val activity = TrainingActivity.UnitActivity(goalIndex, unitIndex)

        val outcome = when {
            session.failure != null || !session.hasUserSpeech ->
                AssessmentOutcome.InsufficientEvidence(com.project.speakmate.domain.model.InsufficientEvidenceReason.NO_USER_SPEECH)
            session.outcome == null ->
                AssessmentOutcome.InsufficientEvidence(com.project.speakmate.domain.model.InsufficientEvidenceReason.EVALUATION_FAILED)
            else -> {
                val evalResult = if (session.outcome == com.project.speakmate.domain.model.CallOutcome.COMPLETED)
                    AssessmentResult.PASS else AssessmentResult.FAIL
                createEvaluatedOutcome(evalResult, null, emptyMap())
            }
        }

        val decision = TrainingProgressRules.applyOutcome(path, progress, activity, outcome)
        val newProgress = when (decision) {
            is TrainingProgressDecision.Advanced -> decision.progress
            is TrainingProgressDecision.PathCompleted -> decision.progress
            else -> null
        }

        val attempt = PracticeAttempt(
            attemptId = UUID.randomUUID().toString(),
            scenarioId = unit.scenarioId,
            practiceType = PracticeType.TRAINING_UNIT,
            startedAt = Date(session.startedAtMillis ?: System.currentTimeMillis()),
            endedAt = Date(session.endedAtMillis ?: System.currentTimeMillis()),
            result = if (outcome is AssessmentOutcome.Evaluated) outcome.result else null,
            criteriaResult = if (outcome is AssessmentOutcome.Evaluated) outcome.criteria else null,
            requiredActionResults = if (outcome is AssessmentOutcome.Evaluated) outcome.requiredActionResults else emptyMap(),
            transcripts = session.transcript.toList(),
            feedbackSummary = null
        )

        viewModelScope.launch {
            _unitResultState.update { UnitResultUiState(presentation = UnitResultPresentation.PROCESSING) }
            val saveResult = historyRepository.saveAttemptAndProgress(attempt, newProgress)
            if (saveResult.isSuccess) {
                if (newProgress != null) {
                    val updatedUser = user.copy(trainingProgress = newProgress)
                    updateUserData(updatedUser)
                }
                val presentation = when {
                    outcome !is AssessmentOutcome.Evaluated -> UnitResultPresentation.INSUFFICIENT_EVIDENCE
                    outcome.result == AssessmentResult.PASS -> UnitResultPresentation.PASS
                    else -> UnitResultPresentation.FAIL_CRITERIA
                }
                _unitResultState.update {
                    UnitResultUiState(
                        presentation = presentation,
                        title = unit.title,
                        explanation = if (presentation == UnitResultPresentation.PASS)
                            "Chúc mừng! Bạn đã hoàn thành tốt bài học."
                        else if (presentation == UnitResultPresentation.INSUFFICIENT_EVIDENCE)
                            "Không đủ dữ kiện để đánh giá bài học."
                        else
                            "Bạn chưa đạt yêu cầu của bài học. Hãy luyện tập lại nhé.",
                        criteriaHeading = "Tiêu chí đánh giá",
                        feedback = if (presentation == UnitResultPresentation.PASS) "Đạt yêu cầu bài học" else "Cần luyện tập thêm",
                        requiredActions = unit.requiredActions.map { action ->
                            UnitResultActionUiState(
                                id = action.requiredActionId,
                                text = action.description,
                                evaluation = if (presentation == UnitResultPresentation.PASS)
                                    UnitResultEvaluation.PASSED
                                else
                                    UnitResultEvaluation.NOT_PASSED
                            )
                        }
                    )
                }
            } else {
                _unitResultState.update {
                    UnitResultUiState(
                        presentation = UnitResultPresentation.INSUFFICIENT_EVIDENCE,
                        title = unit.title,
                        explanation = "Lưu tiến độ thất bại. Vui lòng thử lại.",
                        feedback = "Lỗi kết nối khi lưu kết quả."
                    )
                }
            }
        }
    }

    fun onUnitCallFinished(
        result: AssessmentResult?,
        criteria: com.project.speakmate.domain.model.SpeakingCriteriaResult?,
        actionResults: Map<String, Boolean> = emptyMap()
    ) {
        val unit = currentLoadedUnit ?: return
        val user = currentUser ?: return
        val progress = user.trainingProgress ?: return
        val path = currentPath ?: return

        val goalIndex = path.goals.indexOfFirst { goal -> goal.units.any { it.unitId == unit.unitId } }
        if (goalIndex == -1) return
        val unitIndex = path.goals[goalIndex].units.indexOfFirst { it.unitId == unit.unitId }
        if (unitIndex == -1) return
        val activity = TrainingActivity.UnitActivity(goalIndex, unitIndex)

        val outcome = if (result == null) {
            AssessmentOutcome.InsufficientEvidence(com.project.speakmate.domain.model.InsufficientEvidenceReason.EVALUATION_FAILED)
        } else {
            createEvaluatedOutcome(result, criteria, actionResults)
        }

        val decision = TrainingProgressRules.applyOutcome(path, progress, activity, outcome)
        val newProgress = when (decision) {
            is TrainingProgressDecision.Advanced -> decision.progress
            is TrainingProgressDecision.PathCompleted -> decision.progress
            else -> null
        }

        val now = Date()
        val attempt = PracticeAttempt(
            attemptId = UUID.randomUUID().toString(),
            scenarioId = unit.scenarioId,
            practiceType = PracticeType.TRAINING_UNIT,
            startedAt = now,
            endedAt = now,
            result = result,
            criteriaResult = criteria,
            requiredActionResults = actionResults,
            transcripts = emptyList(),
            feedbackSummary = null
        )

        viewModelScope.launch {
            _unitResultState.update { UnitResultUiState(presentation = UnitResultPresentation.PROCESSING) }
            val saveResult = historyRepository.saveAttemptAndProgress(attempt, newProgress)
            if (saveResult.isSuccess) {
                if (newProgress != null) {
                    val updatedUser = user.copy(trainingProgress = newProgress)
                    updateUserData(updatedUser)
                }
                val presentation = when {
                    result == null -> UnitResultPresentation.INSUFFICIENT_EVIDENCE
                    result == AssessmentResult.PASS -> UnitResultPresentation.PASS
                    result == AssessmentResult.PARTIAL -> UnitResultPresentation.PARTIAL
                    actionResults.values.any { !it } -> UnitResultPresentation.FAIL_REQUIRED_ACTION
                    else -> UnitResultPresentation.FAIL_CRITERIA
                }
                _unitResultState.update {
                    UnitResultUiState(
                        presentation = presentation,
                        title = unit.title,
                        explanation = if (presentation == UnitResultPresentation.PASS)
                            "Chúc mừng! Bạn đã hoàn thành tốt bài học."
                        else if (presentation == UnitResultPresentation.INSUFFICIENT_EVIDENCE)
                            "Không đủ dữ kiện để đánh giá bài học."
                        else
                            "Bạn chưa đạt yêu cầu của bài học. Hãy luyện tập lại nhé.",
                        criteriaHeading = "Tiêu chí đánh giá",
                        feedback = if (presentation == UnitResultPresentation.PASS) "Đạt yêu cầu bài học" else "Cần luyện tập thêm",
                        requiredActions = unit.requiredActions.map { action ->
                            UnitResultActionUiState(
                                id = action.requiredActionId,
                                text = action.description,
                                evaluation = if (actionResults[action.requiredActionId] == true)
                                    UnitResultEvaluation.PASSED
                                else
                                    UnitResultEvaluation.NOT_PASSED
                            )
                        }
                    )
                }
            } else {
                _unitResultState.update {
                    UnitResultUiState(
                        presentation = UnitResultPresentation.INSUFFICIENT_EVIDENCE,
                        title = unit.title,
                        explanation = "Lưu tiến độ thất bại. Vui lòng thử lại.",
                        feedback = "Lỗi kết nối khi lưu kết quả."
                    )
                }
            }
        }
    }

    fun dismissUnitResult() {
        _unitResultState.update { null }
    }

    private fun createEvaluatedOutcome(
        result: AssessmentResult,
        criteria: SpeakingCriteriaResult?,
        actionResults: Map<String, Boolean>
    ): AssessmentOutcome.Evaluated {
        val resolvedCriteria = criteria ?: SpeakingCriteriaResult(false, false, false, false, false)
        val defaultJudgement = CriterionJudgement(false, "", "", null, null)
        val evaluation = SpeakingEvaluation(
            range = defaultJudgement.copy(isMet = resolvedCriteria.rangeMet),
            accuracy = defaultJudgement.copy(isMet = resolvedCriteria.accuracyMet),
            fluency = defaultJudgement.copy(isMet = resolvedCriteria.fluencyMet),
            interaction = defaultJudgement.copy(isMet = resolvedCriteria.interactionMet),
            coherence = defaultJudgement.copy(isMet = resolvedCriteria.coherenceMet),
            requiredActions = actionResults.mapValues { RequiredActionJudgement(it.value, "") }
        )
        return AssessmentOutcome.Evaluated(
            result = result,
            criteria = resolvedCriteria,
            requiredActionResults = actionResults,
            evaluation = evaluation
        )
    }
}
