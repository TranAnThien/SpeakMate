package com.project.speakmate.ui.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.ScenarioSelection
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingActivity
import com.project.speakmate.domain.model.TrainingActivityScenario
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.domain.rules.TrainingProgressRules
import com.project.speakmate.domain.rules.VariantRules
import com.project.speakmate.ui.training.state.GoalAssessmentActionUiState
import com.project.speakmate.ui.training.state.GoalAssessmentUiState
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.utils.AppConstant
import com.project.speakmate.utils.extension.defaultEmpty
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
class GoalAssessmentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val trainingRepository: TrainingRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val goalId = savedStateHandle.get<String>(AppConstant.GOAL_ID_ARGUMENT).defaultEmpty()
    private val _uiState = MutableStateFlow(GoalAssessmentUiState())
    val uiState: StateFlow<GoalAssessmentUiState> = _uiState.asStateFlow()
    private var observationJob: Job? = null

    init {
        observeAssessment()
    }

    fun retry() {
        observeAssessment()
    }

    fun prepareRetry() {
        val original = _uiState.value.activity ?: return
        if (_uiState.value.status != TrainingContentStatus.CONTENT) return
        val selection = VariantRules.selectForRetry(original, emptyList(), null, Random.Default)
        val scenario = when (selection) {
            is ScenarioSelection.Original -> selection.scenario
            is ScenarioSelection.Variant -> original.scenario.copy(promptTemplate = selection.variant.promptTemplate)
        }
        _uiState.update { it.copy(activity = original.copy(scenario = scenario)) }
    }

    private fun observeAssessment() {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            try {
                authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                    _uiState.update { GoalAssessmentUiState() }
                    if (uid == null) return@collectLatest
                    userRepository.observeUser(uid).collectLatest { user ->
                        _uiState.update { GoalAssessmentUiState() }
                        val profile = checkNotNull(user)
                        check(profile.level != SpeakingLevel.NOT_ASSESSED)
                        val progress = checkNotNull(profile.trainingProgress)
                        val path = trainingRepository.getTrainingPath(profile.level).getOrThrow()
                        val index = path.goals.indexOfFirst { it.goalId == goalId }
                        val location = TrainingActivity.GoalAssessmentActivity(index)
                        if (!TrainingProgressRules.canOpen(path, progress, location)) {
                            _uiState.update { GoalAssessmentUiState(status = TrainingContentStatus.ERROR, isLocked = true) }
                            return@collectLatest
                        }
                        val goal = path.goals[index]
                        val scenario = trainingRepository.getScenario(goal.goalAssessment.scenarioId).getOrThrow()
                        val activity = TrainingActivityScenario(
                            scenario, location, path.targetLevel, goal.canDoDescription,
                            goal.goalAssessment.requiredActions.toList(), path.targetLevel.name, AssessmentResult.PASS.name
                        )
                        val isReplay = TrainingProgressRules.isReplay(path, progress, location)
                        _uiState.update {
                            GoalAssessmentUiState(
                                status = TrainingContentStatus.CONTENT,
                                title = goal.goalAssessment.title,
                                description = scenario.description,
                                requiredActions = activity.requiredActions.map { GoalAssessmentActionUiState(it.requiredActionId, it.description) },
                                activity = activity,
                                isReplay = isReplay
                            )
                        }
                        if (isReplay) prepareRetry()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { GoalAssessmentUiState(status = TrainingContentStatus.ERROR) }
            }
        }
    }
}
