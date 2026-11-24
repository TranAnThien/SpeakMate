package com.project.speakmate.ui.placement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.PlacementResult
import com.project.speakmate.domain.model.AssessmentOwnershipException
import com.project.speakmate.domain.model.AssessmentTask
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.PlacementCheckpoint
import com.project.speakmate.domain.model.PlacementStep
import com.project.speakmate.domain.model.PlacementStopReason
import com.project.speakmate.domain.model.PlacementTransition
import com.project.speakmate.domain.model.PracticeRecoveryReason
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.repository.AssessmentGradingRepository
import com.project.speakmate.domain.repository.AssessmentTaskRepository
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.PlacementCheckpointRepository
import com.project.speakmate.domain.repository.ScenarioRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.domain.rules.PlacementRules
import com.project.speakmate.ui.placement.state.PlacementNavigation
import com.project.speakmate.ui.placement.state.PlacementPhase
import com.project.speakmate.ui.placement.state.PlacementUiState
import com.project.speakmate.utils.extension.default
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlacementViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val taskRepository: AssessmentTaskRepository,
    private val checkpointRepository: PlacementCheckpointRepository,
    private val assessmentRepository: AssessmentGradingRepository,
    private val scenarioRepository: ScenarioRepository,
    private val clock: Clock,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlacementUiState())
    val uiState: StateFlow<PlacementUiState> = _uiState.asStateFlow()
    private val random = Random.Default
    private var userId: String? = null
    private var checkpoint: PlacementCheckpoint? = null
    private var tasks: List<AssessmentTask> = emptyList()
    private var operationJob: Job? = null
    private var leaveJob: Job? = null
    private var isLeaving = false
    private var resultExpiryJob: Job? = null

    init {
        viewModelScope.launch {
            assessmentRepository.gradingState.collect { grading ->
                if (grading.isExpired) {
                    operationJob?.cancel()
                    val saved = checkpoint
                    if (saved != null && !isLeaving)
                        try {
                            expireResult(saved)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: AssessmentOwnershipException) {
                            Unit
                        } catch (_: Exception) {
                            showUndetermined()
                        }
                }
            }
        }
        viewModelScope.launch {
            authRepository
                .observeSignedInUserId()
                .distinctUntilChanged()
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    operationJob?.cancelAndJoin()
                    resultExpiryJob?.cancel()
                    assessmentRepository.clear()
                    showUndetermined()
                }
                .collectLatest { uid ->
                    operationJob?.cancelAndJoin()
                    resultExpiryJob?.cancel()
                    assessmentRepository.clear()
                    userId = uid
                    checkpoint = null
                    tasks = emptyList()
                    _uiState.update { PlacementUiState() }
                    if (uid == null || isLeaving) {
                        showUndetermined()
                        return@collectLatest
                    }
                    operationJob =
                        viewModelScope.launch {
                            try {
                                loadAssessment(uid)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: AssessmentOwnershipException) {
                                Unit
                            } catch (exception: Exception) {
                                showUndetermined()
                            }
                        }
                }
        }
    }

    fun retryLoad() {
        if (_uiState.value.phase != PlacementPhase.UNDETERMINED || !_uiState.value.canRetryLoad) return
        val uid = userId ?: return
        launchOperation { loadAssessment(uid) }
    }

    private suspend fun loadAssessment(uid: String) {
        ensureOwner(uid)
        check(authRepository.requireCurrentUserId() == uid)
        val saved = checkpointRepository.getCheckpoint(uid)
        val restored = PlacementRestoreMapper.restore(saved, clock.millis())
        if (restored.shouldClearCheckpoint) {
            checkpointRepository.clearCheckpoint(uid)
        }
        check(authRepository.requireCurrentUserId() == uid)
        val profile = checkNotNull(userRepository.getUser(userId = uid, requireServer = true).getOrThrow())
        ensureOwner(uid)
        check(profile.userId == uid)
        if (!PlacementRules.canStart(profile)) {
            checkpointRepository.clearCheckpoint(uid)
            finishAlreadyAssessed()
            return
        }
        checkpoint = restored.checkpoint
        _uiState.update {
            it.copy(
                phase = restored.phase,
                result = restored.checkpoint?.pendingResult,
                recoveryProblem =
                    if (restored.shouldClearCheckpoint) PracticeRecoveryReason.ASSESSMENT_EXPIRED else null
            )
        }
        if (restored.phase == PlacementPhase.RESULT) saveResult()
    }

    fun start() {
        if (_uiState.value.phase != PlacementPhase.INTRODUCTION) return
        launchOperation { startNewAssessment() }
    }

    fun restartExpiredAssessment() {
        if (_uiState.value.recoveryProblem != null)
            _uiState.update { PlacementUiState(phase = PlacementPhase.INTRODUCTION) }
    }

    fun skip() {
        if (_uiState.value.phase !in listOf(PlacementPhase.INTRODUCTION, PlacementPhase.RESUME_PROMPT)) return
        launchOperation(shouldShowLoading = false) {
            _uiState.update { it.copy(isSavingResult = true, saveFailed = false) }
            try {
                val uid = userId ?: return@launchOperation
                check(authRepository.requireCurrentUserId() == uid)
                ensureOwner(uid)
                userRepository.markNotAssessed().getOrThrow()
                ensureOwner(uid)
                checkpointRepository.clearCheckpoint(uid)
                ensureOwner(uid)
                checkpoint = null
                assessmentRepository.clear()
                navigateHome()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: AssessmentOwnershipException) {
                Unit
            } catch (_: Exception) {
                _uiState.update { it.copy(saveFailed = true) }
            } finally {
                _uiState.update { it.copy(isSavingResult = false) }
            }
        }
    }

    fun exit() {
        leave()
    }

    fun resume() {
        if (_uiState.value.phase != PlacementPhase.RESUME_PROMPT) return
        launchOperation {
            val saved = checkpoint ?: return@launchOperation
            if (PlacementRules.isExpired(saved, clock.millis())) expireResult(saved) else openCurrentTask()
        }
    }

    fun restart() {
        if (_uiState.value.phase != PlacementPhase.RESUME_PROMPT) return
        launchOperation { startNewAssessment() }
    }

    fun submitCallResult(session: CallSessionState) {
        gradeCallResult(session = session, isRetry = false)
    }

    override fun onCleared() {
        assessmentRepository.clear()
        super.onCleared()
    }

    fun retryGrading() {
        if (_uiState.value.pendingGradingReason == null || operationJob?.isActive == true) return
        gradeCallResult(session = null, isRetry = true)
    }

    private fun gradeCallResult(session: CallSessionState?, isRetry: Boolean) {
        val state = _uiState.value
        val task = state.currentTask ?: return
        val saved = checkpoint ?: return
        if ((!isRetry &&
            (state.phase != PlacementPhase.CALLING ||
                session?.outcome == null ||
                session.state !in listOf(CallState.ENDED, CallState.FAILED))) || isLeaving)
            return
        val previousOperation = operationJob
        _uiState.update {
            it.copy(
                phase = PlacementPhase.GRADING,
                pendingGradingReason = null,
                hasReachedAudioLimit = session?.hasReachedAssessmentAudioLimit.default(state.hasReachedAudioLimit)
            )
        }
        operationJob = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                previousOperation?.join()
                gradeCurrentTask(session = session, isRetry = isRetry, task = task, saved = saved)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: AssessmentOwnershipException) {
                Unit
            } catch (_: Exception) {
                showUndetermined()
            }
        }
    }

    private suspend fun gradeCurrentTask(
        session: CallSessionState?,
        isRetry: Boolean,
        task: AssessmentTask,
        saved: PlacementCheckpoint
    ) {
        ensureOwner(saved.userId)
        val assessment = getAssessmentOutcome(
            session = session,
            isRetry = isRetry,
            task = task,
            saved = saved
        )
        if (assessmentRepository.gradingState.value.isExpired) return
        ensureOwner(saved.userId)
        if (PlacementRules.isExpired(saved, clock.millis())) {
            expireResult(saved)
            return
        }
        val transition =
            PlacementRules.applyOutcome(
                checkpoint = saved,
                task = task,
                outcome = assessment,
                tasks = tasks,
                random = random,
                nowMillis = clock.millis()
            )
        val canRetryGrading =
            transition is PlacementTransition.RetryTask && assessmentRepository.gradingState.value.canRetry
        if (!canRetryGrading) assessmentRepository.clear()
        applyGradingTransition(saved = saved, transition = transition, canRetryGrading = canRetryGrading)
    }

    private suspend fun getAssessmentOutcome(
        session: CallSessionState?,
        isRetry: Boolean,
        task: AssessmentTask,
        saved: PlacementCheckpoint
    ): AssessmentOutcome {
        return if (isRetry) assessmentRepository.retry()
        else {
            val completed = checkNotNull(session)
            assessmentRepository.gradeCallResult(
                task = task,
                session = completed,
                userId = saved.userId,
                expiresAtMillis = saved.expiresAtMillis
            )
        }
    }

    private suspend fun applyGradingTransition(
        saved: PlacementCheckpoint,
        transition: PlacementTransition,
        canRetryGrading: Boolean
    ) {
        when (transition) {
            is PlacementTransition.NextTask -> {
                saveCheckpoint(transition.checkpoint)
                _uiState.update {
                    PlacementUiMapper.toNextTask(
                        state = it,
                        previousStep = saved.step,
                        transition = transition,
                        tasks = tasks
                    )
                }
            }
            is PlacementTransition.RetryTask -> {
                saveCheckpoint(transition.checkpoint)
                _uiState.update {
                    it.copy(
                        phase = PlacementPhase.INSUFFICIENT_EVIDENCE,
                        canRetry = true,
                        insufficientReason = transition.reason,
                        pendingGradingReason = if (canRetryGrading) transition.reason else null
                    )
                }
            }
            is PlacementTransition.Confirmed -> {
                saveCheckpoint(transition.checkpoint)
                _uiState.update {
                    it.copy(phase = PlacementPhase.RESULT, result = transition.result, scenario = null)
                }
                saveResult()
            }
            is PlacementTransition.Stopped -> stopAssessment(transition.reason)
        }
    }

    fun retry() {
        if (_uiState.value.pendingGradingReason != null) {
            retryGrading()
            return
        }
        if (_uiState.value.phase != PlacementPhase.INSUFFICIENT_EVIDENCE || !_uiState.value.canRetry) return
        launchOperation { openCurrentTask() }
    }

    private fun isCurrentOwner(uid: String): Boolean {
        val currentUserId =
            try {
                authRepository.requireCurrentUserId()
            } catch (_: IllegalStateException) {
                null
            }
        return !isLeaving && userId == uid && currentUserId == uid
    }

    private suspend fun ensureOwner(uid: String) {
        currentCoroutineContext().ensureActive()
        if (!isCurrentOwner(uid)) throw AssessmentOwnershipException()
    }

    fun continueAssessment() {
        if (_uiState.value.phase != PlacementPhase.NEXT_TASK) return
        launchOperation { openCurrentTask() }
    }

    fun confirmResultSaved() {
        if (_uiState.value.phase != PlacementPhase.RESULT || _uiState.value.isSavingResult) return
        if (_uiState.value.isResultSaved) {
            exit()
        } else {
            launchOperation(shouldShowLoading = false) { saveResult() }
        }
    }

    private suspend fun saveResult() {
        val saved = checkpoint ?: return
        val result = saved.pendingResult ?: return
        val uid = userId ?: return
        scheduleResultExpiry(saved, uid)
        _uiState.update { it.copy(isSavingResult = true, saveFailed = false) }
        try {
            saveCurrentResult(saved = saved, result = result, uid = uid)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: AssessmentOwnershipException) {
            Unit
        } catch (_: Exception) {
            _uiState.update { it.copy(saveFailed = true) }
        } finally {
            _uiState.update { it.copy(isSavingResult = false) }
        }
    }

    private fun scheduleResultExpiry(saved: PlacementCheckpoint, uid: String) {
        resultExpiryJob?.cancel()
        resultExpiryJob =
            viewModelScope.launch {
                delay(PlacementRules.getResultDelayMillis(saved, clock.millis()))
                if (isCurrentOwner(uid) && checkpoint === saved && !_uiState.value.isResultSaved) {
                    operationJob?.cancel()
                    try {
                        expireResult(saved)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: AssessmentOwnershipException) {
                        Unit
                    } catch (_: Exception) {
                        if (isCurrentOwner(uid)) showUndetermined()
                    }
                }
            }
    }

    private suspend fun saveCurrentResult(saved: PlacementCheckpoint, result: PlacementResult, uid: String) {
        check(authRepository.requireCurrentUserId() == uid && saved.userId == uid)
        if (PlacementRules.isExpired(saved, clock.millis())) {
            expireResult(saved)
            return
        }
        val profile = checkNotNull(userRepository.getUser(userId = uid, requireServer = true).getOrThrow())
        ensureOwner(uid)
        check(profile.userId == uid)
        check(PlacementRules.canSaveResult(profile, result))
        checkpointRepository.saveCheckpoint(saved)
        ensureOwner(uid)
        userRepository
            .saveAssessedLevel(
                level = result.level,
                criteria = result.speakingProfile,
                assessedAtMillis = result.assessedAtMillis,
                trainingProgress =
                    TrainingProgress(currentGoalOrder = 0, currentUnitOrder = 0, stage = TrainingStage.UNIT)
            )
            .getOrThrow()
        ensureOwner(uid)
        checkpointRepository.clearCheckpoint(uid)
        ensureOwner(uid)
        checkpoint = null
        _uiState.update { it.copy(isResultSaved = true) }
        resultExpiryJob?.cancel()
    }

    private suspend fun expireResult(saved: PlacementCheckpoint) {
        ensureOwner(saved.userId)
        checkpoint = null
        try {
            checkpointRepository.clearCheckpoint(saved.userId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Unit
        }
        ensureOwner(saved.userId)
        _uiState.update {
            PlacementUiState(
                phase = PlacementPhase.INTRODUCTION,
                recoveryProblem = PracticeRecoveryReason.ASSESSMENT_EXPIRED
            )
        }
    }

    private fun launchOperation(shouldShowLoading: Boolean = true, operation: suspend () -> Unit) {
        if (isLeaving || operationJob?.isActive == true) return
        if (shouldShowLoading) _uiState.update { it.copy(phase = PlacementPhase.LOADING) }
        operationJob =
            viewModelScope.launch {
                try {
                    operation()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: AssessmentOwnershipException) {
                    Unit
                } catch (exception: Exception) {
                    showUndetermined()
                }
            }
    }

    private suspend fun startNewAssessment() {
        val uid = userId ?: return
        ensureOwner(uid)
        val profile = checkNotNull(userRepository.getUser(userId = uid, requireServer = true).getOrThrow())
        ensureOwner(uid)
        check(profile.userId == uid)
        if (!PlacementRules.canStart(profile)) {
            finishAlreadyAssessed()
            return
        }
        checkpointRepository.clearCheckpoint(uid)
        checkpoint = null
        tasks = taskRepository.getAssessmentTasks().toList()
        ensureOwner(uid)
        val task =
            PlacementRules.selectTask(
                step = PlacementStep.INITIAL_A2,
                tasks = tasks,
                checkpoint = null,
                random = random
            )
        if (task == null) stopAssessment(PlacementStopReason.NO_TASK_AVAILABLE)
        else {
            saveCheckpoint(PlacementRules.start(userId = uid, task = task, nowMillis = clock.millis()))
            openCurrentTask()
        }
    }

    private suspend fun openCurrentTask() {
        val saved = checkpoint ?: return
        if (PlacementRules.isExpired(saved, clock.millis())) {
            expireResult(saved)
            return
        }
        ensureOwner(saved.userId)
        val profile = checkNotNull(userRepository.getUser(userId = saved.userId, requireServer = true).getOrThrow())
        ensureOwner(saved.userId)
        check(profile.userId == saved.userId)
        if (!PlacementRules.canStart(profile)) {
            finishAlreadyAssessed()
            return
        }
        tasks = taskRepository.getAssessmentTasks().toList()
        ensureOwner(saved.userId)
        val task = PlacementRules.selectTask(step = saved.step, tasks = tasks, checkpoint = saved, random = random)
        val scenario = task?.let { scenarioRepository.getScenario(scenarioId = it.scenarioId, requireServer = true) }
        ensureOwner(saved.userId)
        if (task == null || scenario == null) {
            stopAssessment(PlacementStopReason.NO_TASK_AVAILABLE)
            return
        }
        if (PlacementRules.isExpired(saved, clock.millis())) {
            expireResult(saved)
            return
        }
        check(scenario.scenarioId == task.scenarioId)
        _uiState.update {
            PlacementUiState(
                phase = PlacementPhase.CALLING,
                currentTask = task,
                scenario = scenario,
                currentTaskNumber = saved.records.size + 1,
                taskRole = saved.step.role
            )
        }
    }

    private suspend fun saveCheckpoint(updated: PlacementCheckpoint) {
        ensureOwner(updated.userId)
        checkpointRepository.saveCheckpoint(updated)
        ensureOwner(updated.userId)
        checkpoint = updated
    }

    private suspend fun stopAssessment(reason: PlacementStopReason) {
        val uid = userId
        if (uid != null) checkpointRepository.clearCheckpoint(uid)
        checkpoint = null
        _uiState.update {
            it.copy(
                phase = PlacementPhase.UNDETERMINED,
                stopReason = reason,
                canRetry = false,
                scenario = null,
                canRetryLoad = false
            )
        }
    }

    private fun showUndetermined() {
        _uiState.update {
            it.copy(
                phase = PlacementPhase.UNDETERMINED,
                canRetry = false,
                scenario = null,
                stopReason = null,
                canRetryLoad = userId != null
            )
        }
    }

    fun consumePendingNavigation(): PlacementNavigation? {
        while (true) {
            val current = _uiState.value
            val destination = current.pendingNavigation ?: return null
            if (_uiState.compareAndSet(current, current.copy(pendingNavigation = null))) {
                return destination
            }
        }
    }

    private fun leave() {
        if (isLeaving) {
            if (leaveJob?.isActive != true && _uiState.value.pendingNavigation == null) {
                _uiState.update { it.copy(pendingNavigation = PlacementNavigation.HOME) }
            }
            return
        }
        isLeaving = true
        resultExpiryJob?.cancel()
        leaveJob =
            viewModelScope.launch {
                operationJob?.cancelAndJoin()
                assessmentRepository.clear()
                navigateHome()
            }
    }

    private fun finishAlreadyAssessed() {
        isLeaving = true
        _uiState.update {
            it.copy(
                phase = PlacementPhase.ALREADY_ASSESSED,
                pendingNavigation = PlacementNavigation.HOME
            )
        }
    }

    private fun navigateHome() {
        isLeaving = true
        _uiState.update { it.copy(pendingNavigation = PlacementNavigation.HOME) }
    }
}
