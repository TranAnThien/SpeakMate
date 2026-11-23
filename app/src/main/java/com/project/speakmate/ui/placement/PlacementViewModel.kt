package com.project.speakmate.ui.placement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentOwnershipException
import com.project.speakmate.domain.model.AssessmentTask
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.PlacementCheckpoint
import com.project.speakmate.domain.model.PlacementStep
import com.project.speakmate.domain.model.PlacementStopReason
import com.project.speakmate.domain.model.PlacementTransition
import com.project.speakmate.domain.model.PracticeRecoveryReason
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.repository.AssessmentGradingRepository
import com.project.speakmate.domain.repository.AssessmentTaskRepository
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.PlacementCheckpointRepository
import com.project.speakmate.domain.repository.ScenarioRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.domain.rules.PlacementRules
import com.project.speakmate.domain.rules.PracticeCompletionRules
import com.project.speakmate.ui.placement.state.PlacementEffect
import com.project.speakmate.ui.placement.state.PlacementPhase
import com.project.speakmate.ui.placement.state.PlacementUiState
import com.project.speakmate.utils.extension.default
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class PlacementViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val taskRepository: AssessmentTaskRepository,
    private val checkpointRepository: PlacementCheckpointRepository,
    private val assessmentRepository: AssessmentGradingRepository,
    private val scenarioRepository: ScenarioRepository,
    private val clock: Clock,
    private val userRepository: UserRepository
) : ViewModel() {
    companion object {
        private const val NAVIGATION_TIMEOUT_MILLIS = 1_000L
    }

    private val _uiState = MutableStateFlow(PlacementUiState())
    val uiState: StateFlow<PlacementUiState> = _uiState.asStateFlow()
    private val _uiEffect = MutableSharedFlow<PlacementEffect>(replay = 0)
    val uiEffect = _uiEffect.asSharedFlow()
    private val random = Random.Default
    private var userId: String? = null
    private var checkpoint: PlacementCheckpoint? = null
    private var tasks: List<AssessmentTask> = emptyList()
    private var operationJob: Job? = null
    private var isLeaving = false
    private var resultExpiryJob: Job? = null

    init {
        viewModelScope.launch {
            assessmentRepository.gradingState.collect { grading ->
                if (grading.isExpired) {
                    operationJob?.cancel()
                    val saved = checkpoint
                    if (saved != null) try { expireResult(saved) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { showUndetermined() }
                }
            }
        }
        viewModelScope.launch {
            authRepository.observeSignedInUserId().distinctUntilChanged().catch { failure ->
                if (failure is CancellationException) throw failure
                operationJob?.cancelAndJoin()
                resultExpiryJob?.cancel()
                assessmentRepository.clear()
                showUndetermined()
            }.collectLatest { uid ->
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
                operationJob = viewModelScope.launch {
                    try {
                        loadAssessment(uid)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
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
        val profile = checkNotNull(userRepository.getUser(uid, requireServer = true).getOrThrow())
        ensureOwner(uid)
        check(profile.userId == uid)
        if (profile.level != SpeakingLevel.NOT_ASSESSED) {
            checkpointRepository.clearCheckpoint(uid)
            _uiState.update { it.copy(phase = PlacementPhase.ALREADY_ASSESSED) }
            return
        }
        checkpoint = restored.checkpoint
        _uiState.update {
            it.copy(phase = restored.phase, result = restored.checkpoint?.pendingResult,
                recoveryProblem = if (restored.shouldClearCheckpoint) PracticeRecoveryReason.ASSESSMENT_EXPIRED else null)
        }
        if (restored.phase == PlacementPhase.RESULT) saveResult()
    }

    fun start() {
        if (_uiState.value.phase != PlacementPhase.INTRODUCTION) return
        launchOperation { startNewAssessment() }
    }

    fun restartExpiredAssessment() {
        if (_uiState.value.recoveryProblem != null) _uiState.update { PlacementUiState(phase = PlacementPhase.INTRODUCTION) }
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
            } catch (_: Exception) {
                _uiState.update { it.copy(saveFailed = true) }
            } finally {
                _uiState.update { it.copy(isSavingResult = false) }
            }
        }
    }

    fun exit() {
        leave(shouldClearCheckpoint = false)
    }

    fun resume() {
        if (_uiState.value.phase != PlacementPhase.RESUME_PROMPT) return
        launchOperation {
            val saved = checkpoint ?: return@launchOperation
            if (PlacementRules.isExpired(saved, clock.millis())) expireResult(saved)
            else openCurrentTask()
        }
    }

    fun restart() {
        if (_uiState.value.phase != PlacementPhase.RESUME_PROMPT) return
        launchOperation { startNewAssessment() }
    }

    fun submitCallResult(session: CallSessionState) {
        gradeCallResult(session, false)
    }

    override fun onCleared() {
        assessmentRepository.clear()
        super.onCleared()
    }

    fun retryGrading() {
        if (_uiState.value.pendingGradingReason == null || operationJob?.isActive == true) return
        gradeCallResult(null, true)
    }

    private fun gradeCallResult(session: CallSessionState?, isRetry: Boolean) {
        val state = _uiState.value
        val task = state.currentTask ?: return
        val saved = checkpoint ?: return
        if ((!isRetry && (state.phase != PlacementPhase.CALLING || session?.outcome == null ||
                session.state !in listOf(CallState.ENDED, CallState.FAILED))) || isLeaving) return
        val previousOperation = operationJob
        _uiState.update {
            it.copy(phase = PlacementPhase.GRADING, pendingGradingReason = null, hasReachedAudioLimit = session?.hasReachedAssessmentAudioLimit.default(state.hasReachedAudioLimit))
        }
        operationJob = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                previousOperation?.join()
                ensureOwner(saved.userId)
                val assessment = if (isRetry) assessmentRepository.retry() else {
                    val completed = checkNotNull(session)
                    assessmentRepository.grade(AssessmentEvidence(
                        targetLevel = task.targetLevel,
                        communicativeGoal = task.communicativeGoal,
                        requiredActions = emptyList(),
                        callOutcome = checkNotNull(completed.outcome),
                        failure = completed.failure,
                        hasUserSpeech = completed.hasUserSpeech,
                        transcript = completed.transcript.toList(),
                        audioDurationMillis = completed.assessmentAudioDurationMillis,
                        assessmentAudioId = completed.assessmentAudioId,
                        taskId = task.taskId,
                        expiresAtMillis = saved.expiresAtMillis,
                        userId = saved.userId,
                        operationId = UUID.randomUUID().toString()
                    ))
                }
                if (assessmentRepository.gradingState.value.isExpired) return@launch
                ensureOwner(saved.userId)
                if (PlacementRules.isExpired(saved, clock.millis())) {
                    expireResult(saved)
                    return@launch
                }
                val transition = PlacementRules.applyOutcome(saved, task, assessment, tasks, random, clock.millis())
                val canRetryGrading = transition is PlacementTransition.RetryTask && assessmentRepository.gradingState.value.canRetry
                if (!canRetryGrading) assessmentRepository.clear()
                when (transition) {
                    is PlacementTransition.NextTask -> {
                        saveCheckpoint(transition.checkpoint)
                        _uiState.update {
                            it.copy(
                                phase = PlacementPhase.NEXT_TASK,
                                nextTask = PlacementNextTaskMapper.classify(saved.step, transition.checkpoint.step),
                                currentTask = tasks.singleOrNull { task -> task.taskId == transition.checkpoint.currentTaskId },
                                currentTaskNumber = transition.checkpoint.records.size + 1,
                                taskRole = transition.checkpoint.step.role,
                                scenario = null
                            )
                        }
                    }
                    is PlacementTransition.RetryTask -> {
                        saveCheckpoint(transition.checkpoint)
                        _uiState.update {
                            it.copy(phase = PlacementPhase.INSUFFICIENT_EVIDENCE, canRetry = true, insufficientReason = transition.reason,
                                pendingGradingReason = if (canRetryGrading) transition.reason else null)
                        }
                    }
                    is PlacementTransition.Confirmed -> {
                        saveCheckpoint(transition.checkpoint)
                        _uiState.update { it.copy(phase = PlacementPhase.RESULT, result = transition.result, scenario = null) }
                        saveResult()
                    }
                    is PlacementTransition.Stopped -> stopAssessment(transition.reason)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                showUndetermined()
            }
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

    private suspend fun ensureOwner(uid: String) {
        currentCoroutineContext().ensureActive()
        val currentUserId = try { authRepository.requireCurrentUserId() } catch (_: IllegalStateException) { null }
        if (isLeaving || userId != uid || currentUserId != uid) throw AssessmentOwnershipException()
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
        resultExpiryJob?.cancel()
        resultExpiryJob = viewModelScope.launch {
            delay((saved.expiresAtMillis - clock.millis()).coerceIn(0L, PracticeCompletionRules.AUDIO_RETENTION_MILLIS))
            if (checkpoint === saved && !_uiState.value.isResultSaved && userId == uid) {
                operationJob?.cancel()
                expireResult(saved)
            }
        }
        _uiState.update { it.copy(isSavingResult = true, saveFailed = false) }
        try {
            check(authRepository.requireCurrentUserId() == uid && saved.userId == uid)
            if (PlacementRules.isExpired(saved, clock.millis())) {
                expireResult(saved)
                return
            }
            val profile = checkNotNull(userRepository.getUser(uid, requireServer = true).getOrThrow())
            ensureOwner(uid)
            check(profile.userId == uid)
            val isAlreadySaved = profile.level == result.level && profile.lastAssessedAt?.time == result.assessedAtMillis &&
                profile.latestAssessmentCriteria == result.speakingProfile
            check(isAlreadySaved || profile.level == SpeakingLevel.NOT_ASSESSED)
            checkpointRepository.saveCheckpoint(saved)
            ensureOwner(uid)
            userRepository.saveAssessedLevel(
                result.level,
                result.speakingProfile,
                result.assessedAtMillis,
                TrainingProgress(0, 0, TrainingStage.UNIT)
            ).getOrThrow()
            ensureOwner(uid)
            checkpointRepository.clearCheckpoint(uid)
            ensureOwner(uid)
            checkpoint = null
            _uiState.update { it.copy(isResultSaved = true) }
            resultExpiryJob?.cancel()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            _uiState.update { it.copy(saveFailed = true) }
        } finally {
            _uiState.update { it.copy(isSavingResult = false) }
        }
    }

    private suspend fun expireResult(saved: PlacementCheckpoint) {
        ensureOwner(saved.userId)
        checkpoint = null
        try {
            checkpointRepository.clearCheckpoint(saved.userId)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { Unit }
        ensureOwner(saved.userId)
        _uiState.update { PlacementUiState(phase = PlacementPhase.INTRODUCTION, recoveryProblem = PracticeRecoveryReason.ASSESSMENT_EXPIRED) }
    }

    private fun launchOperation(shouldShowLoading: Boolean = true, operation: suspend () -> Unit) {
        if (isLeaving || operationJob?.isActive == true) return
        if (shouldShowLoading) _uiState.update { it.copy(phase = PlacementPhase.LOADING) }
        operationJob = viewModelScope.launch {
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                showUndetermined()
            }
        }
    }

    private suspend fun startNewAssessment() {
        val uid = userId ?: return
        ensureOwner(uid)
        val profile = checkNotNull(userRepository.getUser(uid, requireServer = true).getOrThrow())
        ensureOwner(uid)
        check(profile.userId == uid)
        if (profile.level != SpeakingLevel.NOT_ASSESSED) {
            _uiState.update { it.copy(phase = PlacementPhase.ALREADY_ASSESSED) }
            return
        }
        checkpointRepository.clearCheckpoint(uid)
        checkpoint = null
        tasks = taskRepository.getAssessmentTasks().toList()
        ensureOwner(uid)
        val task = PlacementRules.selectTask(PlacementStep.INITIAL_A2, tasks, null, random)
        if (task == null) stopAssessment(PlacementStopReason.NO_TASK_AVAILABLE)
        else {
            saveCheckpoint(PlacementRules.start(uid, task, clock.millis()))
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
        val profile = checkNotNull(userRepository.getUser(saved.userId, requireServer = true).getOrThrow())
        ensureOwner(saved.userId)
        check(profile.userId == saved.userId)
        if (profile.level != SpeakingLevel.NOT_ASSESSED) {
            _uiState.update { it.copy(phase = PlacementPhase.ALREADY_ASSESSED) }
            return
        }
        tasks = taskRepository.getAssessmentTasks().toList()
        ensureOwner(saved.userId)
        val task = PlacementRules.selectTask(saved.step, tasks, saved, random)
        val scenario = task?.let { scenarioRepository.getScenario(it.scenarioId, requireServer = true) }
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
        _uiState.update { PlacementUiState(phase = PlacementPhase.CALLING, currentTask = task, scenario = scenario,
            currentTaskNumber = saved.records.size + 1, taskRole = saved.step.role) }
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
        _uiState.update { it.copy(phase = PlacementPhase.UNDETERMINED, stopReason = reason, canRetry = false, scenario = null, canRetryLoad = false) }
    }

    private fun showUndetermined() {
        _uiState.update { it.copy(phase = PlacementPhase.UNDETERMINED, canRetry = false, scenario = null, stopReason = null,
            canRetryLoad = userId != null) }
    }

    private fun leave(shouldClearCheckpoint: Boolean) {
        if (isLeaving) return
        isLeaving = true
        val uid = userId
        viewModelScope.launch {
            operationJob?.cancelAndJoin()
            assessmentRepository.clear()
            try {
                if (shouldClearCheckpoint && uid != null) {
                    checkpointRepository.clearCheckpoint(uid)
                    checkpoint = null
                }
                navigateHome()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                isLeaving = false
                showUndetermined()
            }
        }
    }

    private suspend fun navigateHome() {
        isLeaving = true
        val delivered = withTimeoutOrNull(NAVIGATION_TIMEOUT_MILLIS) {
            _uiEffect.subscriptionCount.first { it > 0 }
            _uiEffect.emit(PlacementEffect.NavigateHome)
            true
        }
        if (delivered != true) {
            isLeaving = false
            _uiState.update {
                it.copy(phase = when {
                    it.result != null -> PlacementPhase.RESULT
                    checkpoint != null -> PlacementPhase.RESUME_PROMPT
                    else -> PlacementPhase.INTRODUCTION
                })
            }
        }
    }

}
