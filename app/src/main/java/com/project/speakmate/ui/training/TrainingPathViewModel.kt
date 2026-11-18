package com.project.speakmate.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.ContentStatus
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.domain.rules.TrainingProgressRules
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.ui.training.state.TrainingGoalUiState
import com.project.speakmate.ui.training.state.TrainingPathUiState
import com.project.speakmate.ui.training.state.TrainingProgressStatus
import com.project.speakmate.ui.training.state.TrainingUnitUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
class TrainingPathViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    companion object {
        private const val ASSESSMENT_ID_PREFIX = "assessment:"
    }

    private val _uiState = MutableStateFlow(TrainingPathUiState())
    val uiState: StateFlow<TrainingPathUiState> = _uiState.asStateFlow()
    private var userObservationJob: Job? = null

    init {
        observeCurrentUser()
    }

    fun toggleGoalExpansion(goalId: String) {
        _uiState.update { state ->
            state.copy(goals = state.goals.map { if (it.id == goalId) it.copy(isExpanded = !it.isExpanded) else it })
        }
    }

    fun retry() {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        userObservationJob?.cancel()
        userObservationJob = viewModelScope.launch {
            try {
                authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                    _uiState.update { TrainingPathUiState() }
                    if (uid == null) return@collectLatest
                    try {
                        userRepository.observeUser(uid).collectLatest { user ->
                            _uiState.update { TrainingPathUiState() }
                            val profile = checkNotNull(user)
                            check(profile.level != SpeakingLevel.NOT_ASSESSED)
                            val progress = checkNotNull(profile.trainingProgress)
                            val path = trainingRepository.getTrainingPath(profile.level).getOrThrow()
                            val problem = TrainingProgressRules.validate(path, progress)
                            if (problem != null) {
                                _uiState.update { it.copy(status = TrainingContentStatus.ERROR, progressProblem = problem) }
                            } else {
                                _uiState.update {
                                    it.copy(
                                        status = TrainingContentStatus.CONTENT,
                                        level = profile.level.name,
                                        goals = path.goals.indices.map { index -> mapGoal(path, progress, index) },
                                        canContinue = progress.stage != TrainingStage.PATH_COMPLETED
                                    )
                                }
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        _uiState.update { it.copy(status = TrainingContentStatus.ERROR) }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(status = TrainingContentStatus.ERROR) }
            }
        }
    }

    private fun mapGoal(path: TrainingPath, progress: TrainingProgress, goalIndex: Int): TrainingGoalUiState {
        val goal = path.goals[goalIndex]
        val goalStatus = toUiStatus(TrainingProgressRules.getGoalStatus(path, progress, goalIndex))
        val units = goal.units.mapIndexed { index, unit ->
            val status = toUiStatus(TrainingProgressRules.getUnitStatus(path, progress, goalIndex, index))
            TrainingUnitUiState(unit.unitId, unit.title, unit.communicativeFocus, status, false, status != TrainingProgressStatus.LOCKED)
        }
        val assessmentStatus = toUiStatus(TrainingProgressRules.getGoalAssessmentStatus(path, progress, goalIndex))
        val assessment = TrainingUnitUiState(
            id = ASSESSMENT_ID_PREFIX + goal.goalId,
            title = goal.goalAssessment.title,
            status = assessmentStatus,
            isAssessment = true,
            canOpen = assessmentStatus != TrainingProgressStatus.LOCKED
        )
        return TrainingGoalUiState(
            id = goal.goalId,
            title = goal.title,
            summary = goal.canDoDescription,
            status = goalStatus,
            isExpanded = goalStatus == TrainingProgressStatus.CURRENT,
            units = units + assessment,
            order = goalIndex
        )
    }

    private fun toUiStatus(status: ContentStatus): TrainingProgressStatus = when (status) {
        ContentStatus.COMPLETED -> TrainingProgressStatus.COMPLETED
        ContentStatus.CURRENT -> TrainingProgressStatus.CURRENT
        ContentStatus.LOCKED -> TrainingProgressStatus.LOCKED
    }
}
