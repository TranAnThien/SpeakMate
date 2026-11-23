package com.project.speakmate.ui.training

import androidx.navigation.toRoute
import com.project.speakmate.app.AppRoute
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
import com.project.speakmate.utils.extension.defaultEmpty
import com.project.speakmate.utils.extension.defaultFalse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
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
    private val route = try { savedStateHandle.toRoute<AppRoute.GoalAssessment>() } catch (_: IllegalArgumentException) { null }
    private val goalId = route?.goalId.defaultEmpty()
    private val shouldRequireServer = route?.isCalling.defaultFalse()
    private val _uiState = MutableStateFlow(GoalAssessmentUiState())
    val uiState: StateFlow<GoalAssessmentUiState> = _uiState.asStateFlow()
    private var observationJob: Job? = null
    private var preparedUserId: String? = null

    init {
        observeAssessment()
    }

    fun retry() {
        observeAssessment()
    }

    fun stopObservingAssessment() {
        observationJob?.cancel()
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

    fun prepareStart() {
        val state = _uiState.value
        val activity = state.activity ?: return
        val uid = try { authRepository.requireCurrentUserId() } catch (_: IllegalStateException) { return }
        if (uid != preparedUserId || state.status != TrainingContentStatus.CONTENT || state.isLocked || state.isStarting ||
            activity.targetLevel == SpeakingLevel.NOT_ASSESSED || activity.scenario.scenarioId.isBlank() ||
            activity.scenario.name.isBlank() || activity.scenario.promptTemplate.isBlank()) return
        _uiState.update { it.copy(isStarting = true, isStartReady = true) }
    }

    fun consumeStartReady(): Boolean {
        val state = _uiState.value
        val uid = try { authRepository.requireCurrentUserId() } catch (_: IllegalStateException) { return false }
        return uid == preparedUserId && state.isStartReady && _uiState.compareAndSet(state, state.copy(isStartReady = false))
    }

    fun cancelStart() { _uiState.update { it.copy(isStarting = false, isStartReady = false) } }

    private fun observeAssessment() {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            try {
                authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                    preparedUserId = null
                    _uiState.update { GoalAssessmentUiState() }
                    if (uid == null) return@collectLatest
                    userRepository.observeUser(uid).collectLatest { user ->
                        preparedUserId = null
                        _uiState.update { GoalAssessmentUiState() }
                        val profile = checkNotNull(if (shouldRequireServer) userRepository.getUser(uid, requireServer = true).getOrThrow() else user)
                        check(profile.level != SpeakingLevel.NOT_ASSESSED)
                        val progress = checkNotNull(profile.trainingProgress)
                        val path = trainingRepository.getTrainingPath(profile.level, requireServer = shouldRequireServer).getOrThrow()
                        kotlinx.coroutines.currentCoroutineContext().ensureActive()
                        check(authRepository.requireCurrentUserId() == uid && path.targetLevel == profile.level)
                        val index = path.goals.indexOfFirst { it.goalId == goalId }
                        val location = TrainingActivity.GoalAssessmentActivity(index)
                        if (!TrainingProgressRules.canOpen(path, progress, location)) {
                            _uiState.update { GoalAssessmentUiState(status = TrainingContentStatus.ERROR, isLocked = true) }
                            return@collectLatest
                        }
                        val goal = path.goals[index]
                        val scenario = trainingRepository.getScenario(goal.goalAssessment.scenarioId, requireServer = shouldRequireServer).getOrThrow()
                        kotlinx.coroutines.currentCoroutineContext().ensureActive()
                        check(authRepository.requireCurrentUserId() == uid &&
                            scenario.scenarioId == goal.goalAssessment.scenarioId &&
                            scenario.name.isNotBlank() && scenario.promptTemplate.isNotBlank())
                        val activity = TrainingActivityScenario(
                            scenario, location, path.targetLevel, goal.canDoDescription,
                            goal.goalAssessment.requiredActions.toList(), path.targetLevel.name, AssessmentResult.PASS.name
                        )
                        val isReplay = TrainingProgressRules.isReplay(path, progress, location)
                        preparedUserId = uid
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
