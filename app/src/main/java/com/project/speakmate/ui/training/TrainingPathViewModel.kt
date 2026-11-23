package com.project.speakmate.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.ContentStatus
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.domain.rules.TrainingProgressRules
import com.project.speakmate.ui.training.state.TrainingActivityDestination
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.ui.training.state.TrainingGoalUiState
import com.project.speakmate.ui.training.state.TrainingPathEffect
import com.project.speakmate.ui.training.state.TrainingPathFailure
import com.project.speakmate.ui.training.state.TrainingPathNotice
import com.project.speakmate.ui.training.state.TrainingPathUiState
import com.project.speakmate.ui.training.state.TrainingProgressStatus
import com.project.speakmate.ui.training.state.TrainingUnitUiState
import com.project.speakmate.utils.extension.defaultEmpty
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val _uiState = MutableStateFlow(TrainingPathUiState())
    val uiState: StateFlow<TrainingPathUiState> = _uiState.asStateFlow()
    private val _uiEffect = MutableSharedFlow<TrainingPathEffect>(replay = 0)
    val uiEffect: SharedFlow<TrainingPathEffect> = _uiEffect.asSharedFlow()
    private var userObservationJob: Job? = null
    private var activityJob: Job? = null
    private var hasShownRequestedGoal = false
    private var observedUserId: String? = null

    init {
        observeCurrentUser()
    }

    fun toggleGoalExpansion(goalId: String) {
        _uiState.update { state -> state.copy(goals = state.goals.map { if (it.id == goalId) it.copy(isExpanded = !it.isExpanded) else it }) }
    }

    fun showRequestedGoal(goalId: String) {
        if (hasShownRequestedGoal || _uiState.value.goals.none { it.id == goalId }) return
        hasShownRequestedGoal = true
        showGoal(goalId)
    }

    fun showGoal(goalId: String) {
        _uiState.update { it.copy(selectedGoalId = goalId, isLevelInformationVisible = false) }
    }

    fun hideGoal() {
        _uiState.update { it.copy(selectedGoalId = null, previewUnit = null, notice = null) }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(previewUnit = null, notice = null) }
    }

    fun retry() {
        if (_uiState.value.isRefreshing) return
        observeCurrentUser()
    }

    fun showLevelInformation() {
        _uiState.update { it.copy(isLevelInformationVisible = true, selectedGoalId = null, previewUnit = null, notice = null) }
    }

    fun hideLevelInformation() {
        _uiState.update { it.copy(isLevelInformationVisible = false) }
    }

    fun continuePath() {
        openActivity(null)
    }

    fun prepareSelectedGoal() {
        val goal = _uiState.value.selectedGoalId ?: return
        val destination = _uiState.value.goals.firstOrNull { it.id == goal }?.units
            ?.firstOrNull { it.status == TrainingProgressStatus.CURRENT }?.destination
        if (destination == null) _uiState.update { it.copy(notice = TrainingPathNotice.LOCKED) }
        else openActivity(destination)
    }

    fun openActivity(destination: TrainingActivityDestination?) {
        val uid = observedUserId ?: return
        val state = _uiState.value
        if (activityJob?.isActive == true || state.isOpeningActivity) return
        val preview = state.goals.asSequence().flatMap { it.units.asSequence() }.firstOrNull { it.destination == destination }
        if (state.isStale || state.isRefreshing) {
            _uiState.update { it.copy(previewUnit = preview, notice = TrainingPathNotice.STALE) }
            return
        }
        val shownGoal = state.goals.firstOrNull { goal -> goal.units.any { it.destination == destination } }
        if (shownGoal != null && !shownGoal.isConfigured) {
            _uiState.update { it.copy(selectedGoalId = shownGoal.id) }
            return
        }
        val shownUnit = state.goals.asSequence().flatMap { it.units.asSequence() }.firstOrNull { it.destination == destination }
        if (destination != null && shownUnit?.status == TrainingProgressStatus.LOCKED) {
            _uiState.update { it.copy(previewUnit = shownUnit, notice = TrainingPathNotice.LOCKED) }
            return
        }
        _uiState.update { it.copy(isOpeningActivity = true) }
        activityJob = viewModelScope.launch {
            try {
                check(isCurrentUser(uid))
                val snapshot = _uiState.value
                if (snapshot.status != TrainingContentStatus.CONTENT || snapshot.isStale || snapshot.isRefreshing || snapshot.isUnassessed) return@launch
                val target = destination ?: snapshot.goals.asSequence().flatMap { it.units.asSequence() }
                    .firstOrNull { it.status == TrainingProgressStatus.CURRENT && it.canOpen }?.destination
                val item = snapshot.goals.asSequence().filter { it.isConfigured }.flatMap { it.units.asSequence() }
                    .firstOrNull { it.destination == target && it.canOpen }
                if (item == null) {
                    _uiState.update { it.copy(notice = TrainingPathNotice.LOCKED) }
                    return@launch
                }
                currentCoroutineContext().ensureActive()
                if (target != null && isCurrentUser(uid)) _uiEffect.emit(TrainingPathEffect.OpenActivity(target))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (isCurrentUser(uid)) publishFailure(TrainingPathFailure.PROFILE)
            } finally {
                if (isCurrentUser(uid)) _uiState.update { it.copy(isOpeningActivity = false) }
            }
        }
    }

    private fun observeCurrentUser() {
        userObservationJob?.cancel()
        activityJob?.cancel()
        _uiState.update { it.copy(isRefreshing = it.status == TrainingContentStatus.CONTENT, isStale = it.status == TrainingContentStatus.CONTENT, isOpeningActivity = false) }
        userObservationJob = viewModelScope.launch {
            try {
                authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                    if (observedUserId != uid) {
                        observedUserId = uid
                        _uiState.update { TrainingPathUiState() }
                    }
                    if (uid == null) return@collectLatest
                    try {
                        userRepository.observeUser(uid).collectLatest { profile ->
                            activityJob?.cancel()
                            _uiState.update { it.copy(isRefreshing = it.status == TrainingContentStatus.CONTENT,
                                isStale = it.status == TrainingContentStatus.CONTENT, isOpeningActivity = false) }
                            if (profile == null || profile.userId != uid) {
                                publishFailure(TrainingPathFailure.PROFILE)
                            } else if (profile.level == SpeakingLevel.NOT_ASSESSED) {
                                _uiState.update { TrainingPathUiState(status = TrainingContentStatus.CONTENT, isUnassessed = true) }
                            } else if (profile.trainingProgress == null) {
                                publishFailure(TrainingPathFailure.PROFILE)
                            } else {
                                try {
                                    val path = trainingRepository.getTrainingPath(profile.level, requireServer = true).getOrThrow()
                                    currentCoroutineContext().ensureActive()
                                    if (isCurrentUser(uid)) publishPath(profile, path, profile.trainingProgress)
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (error: Exception) {
                                    if (isCurrentUser(uid)) publishFailure(if (error is IllegalStateException) TrainingPathFailure.UNCONFIGURED else TrainingPathFailure.CONTENT)
                                }
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        if (isCurrentUser(uid)) publishFailure(TrainingPathFailure.PROFILE)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                publishFailure(TrainingPathFailure.PROFILE)
            }
        }
    }

    private fun publishPath(profile: User, path: TrainingPath, progress: TrainingProgress) {
        val problem = TrainingProgressRules.validate(path, progress)
        val total = TrainingProgressRules.getTotalActivityCount(path)
        if (path.targetLevel != profile.level || total == 0 || path.goals.map { it.goalId }.distinct().size != path.goals.size ||
            path.goals.any { goal -> goal.goalId.isBlank() } ||
            path.goals.flatMap { it.units }.map { it.unitId }.distinct().size != path.goals.sumOf { it.units.size }) {
            publishFailure(TrainingPathFailure.UNCONFIGURED)
            return
        }
        if (problem != null) {
            publishFailure(TrainingPathFailure.PROGRESS)
            _uiState.update { it.copy(progressProblem = problem) }
            return
        }
        _uiState.update { previous ->
            val samePath = previous.level == profile.level.name
            val goals = path.goals.indices.map { index ->
                val goal = mapGoal(path, progress, index)
                val old = if (samePath) previous.goals.firstOrNull { it.id == goal.id } else null
                if (old != null) goal.copy(isExpanded = old.isExpanded) else goal
            }
            val current = goals.asSequence().flatMap { it.units.asSequence() }.firstOrNull { it.status == TrainingProgressStatus.CURRENT }
            previous.copy(status = TrainingContentStatus.CONTENT, level = profile.level.name, title = path.title,
                goals = goals, canContinue = progress.stage != TrainingStage.PATH_COMPLETED && current?.canOpen == true,
                isUnassessed = false, isPathCompleted = progress.stage == TrainingStage.PATH_COMPLETED,
                completedActivityCount = TrainingProgressRules.getCompletedActivityCount(path, progress), totalActivityCount = total,
                currentActivityTitle = current?.title.defaultEmpty(), isAssessmentReady = current?.isAssessment == true,
                isRefreshing = false, isStale = false, failure = null, progressProblem = null,
                selectedGoalId = if (samePath) previous.selectedGoalId else null,
                previewUnit = if (samePath) previous.previewUnit else null, notice = if (samePath) previous.notice else null)
        }
    }

    private fun publishFailure(failure: TrainingPathFailure) {
        _uiState.update {
            val hasContent = it.status == TrainingContentStatus.CONTENT
            it.copy(status = if (hasContent) TrainingContentStatus.CONTENT else TrainingContentStatus.ERROR,
                failure = failure, isStale = hasContent, isRefreshing = false, isOpeningActivity = false, canContinue = false)
        }
    }

    private fun isCurrentUser(uid: String): Boolean = try {
        observedUserId == uid && authRepository.requireCurrentUserId() == uid
    } catch (_: IllegalStateException) {
        false
    }

    private fun mapGoal(path: TrainingPath, progress: TrainingProgress, goalIndex: Int): TrainingGoalUiState {
        val goal = path.goals[goalIndex]
        val goalStatus = toUiStatus(TrainingProgressRules.getGoalStatus(path, progress, goalIndex))
        val isConfigured = goal.units.isNotEmpty() && goal.goalAssessment.scenarioId.isNotBlank() && goal.units.all { it.unitId.isNotBlank() && it.scenarioId.isNotBlank() }
        val units = goal.units.mapIndexed { index, unit ->
            val status = toUiStatus(TrainingProgressRules.getUnitStatus(path, progress, goalIndex, index))
            TrainingUnitUiState(unit.unitId, unit.title, unit.communicativeFocus, status, false,
                isConfigured && status != TrainingProgressStatus.LOCKED, TrainingActivityDestination.Unit(unit.unitId))
        }
        val assessmentStatus = toUiStatus(TrainingProgressRules.getGoalAssessmentStatus(path, progress, goalIndex))
        val assessment = TrainingUnitUiState(id = goal.goalId, title = goal.goalAssessment.title, status = assessmentStatus,
            isAssessment = true, canOpen = isConfigured && assessmentStatus != TrainingProgressStatus.LOCKED,
            destination = TrainingActivityDestination.GoalAssessment(goal.goalId))
        return TrainingGoalUiState(id = goal.goalId, title = goal.title, summary = goal.canDoDescription, status = goalStatus,
            isExpanded = goalStatus == TrainingProgressStatus.CURRENT, units = units + assessment, order = goalIndex, isConfigured = isConfigured)
    }

    private fun toUiStatus(status: ContentStatus): TrainingProgressStatus = when (status) {
        ContentStatus.COMPLETED -> TrainingProgressStatus.COMPLETED
        ContentStatus.CURRENT -> TrainingProgressStatus.CURRENT
        ContentStatus.LOCKED -> TrainingProgressStatus.LOCKED
    }
}
