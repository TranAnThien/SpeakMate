package com.project.speakmate.ui.placement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentTask
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.PlacementCheckpoint
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.PlacementStep
import com.project.speakmate.domain.model.PlacementStopReason
import com.project.speakmate.domain.model.PlacementTransition
import com.project.speakmate.domain.repository.AssessmentTaskRepository
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.PlacementCheckpointRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.domain.repository.ScenarioRepository
import com.project.speakmate.domain.repository.SpeakingAssessmentRepository
import com.project.speakmate.domain.rules.PlacementRules
import com.project.speakmate.ui.placement.state.PlacementEffect
import com.project.speakmate.ui.placement.state.PlacementPhase
import com.project.speakmate.ui.placement.state.PlacementUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class PlacementViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val taskRepository: AssessmentTaskRepository,
    private val checkpointRepository: PlacementCheckpointRepository,
    private val assessmentRepository: SpeakingAssessmentRepository,
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

    init {
        viewModelScope.launch {
            authRepository.observeSignedInUserId().distinctUntilChanged().catch { failure ->
                if (failure is CancellationException) throw failure
                showUndetermined()
            }.collectLatest { uid ->
                operationJob?.cancelAndJoin()
                userId = uid
                checkpoint = null
                tasks = emptyList()
                _uiState.update { PlacementUiState() }
                if (uid == null || isLeaving) return@collectLatest
                operationJob = viewModelScope.launch {
                    try {
                        val saved = checkpointRepository.getCheckpoint(uid)
                        val restored = PlacementRestoreMapper.restore(saved, clock.millis())
                        if (restored.shouldClearCheckpoint) {
                            checkpointRepository.clearCheckpoint(uid)
                        }
                        checkpoint = restored.checkpoint
                        _uiState.update {
                            it.copy(phase = restored.phase, result = restored.checkpoint?.pendingResult)
                        }
                        if (restored.phase == PlacementPhase.RESULT) saveResult()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (exception: Exception) {
                        showUndetermined()
                    }
                }
            }
        }
    }

    fun start() {
        if (_uiState.value.phase != PlacementPhase.INTRODUCTION) return
        launchOperation { startNewAssessment() }
    }

    fun skip() {
        if (_uiState.value.phase !in listOf(PlacementPhase.INTRODUCTION, PlacementPhase.RESUME_PROMPT)) return
        launchOperation(shouldShowLoading = false) {
            _uiState.update { it.copy(isSavingResult = true, saveFailed = false) }
            try {
                val uid = userId ?: return@launchOperation
                check(authRepository.requireCurrentUserId() == uid)
                userRepository.markNotAssessed().getOrThrow()
                checkpointRepository.clearCheckpoint(uid)
                checkpoint = null
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
            if (PlacementRules.isExpired(saved, clock.millis())) startNewAssessment()
            else openCurrentTask()
        }
    }

    fun restart() {
        if (_uiState.value.phase != PlacementPhase.RESUME_PROMPT) return
        launchOperation { startNewAssessment() }
    }

    fun submitCallResult(session: CallSessionState) {
        val state = _uiState.value
        val task = state.currentTask ?: return
        val saved = checkpoint ?: return
        val outcome = session.outcome ?: return
        if (state.phase != PlacementPhase.CALLING || session.state !in listOf(CallState.ENDED, CallState.FAILED) || isLeaving) return
        val previousOperation = operationJob
        _uiState.update {
            it.copy(phase = PlacementPhase.GRADING, hasReachedAudioLimit = session.hasReachedAssessmentAudioLimit)
        }
        operationJob = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                previousOperation?.join()
                if (PlacementRules.isExpired(saved, clock.millis())) {
                    startNewAssessment()
                    return@launch
                }
                val evidence = AssessmentEvidence(
                    targetLevel = task.targetLevel,
                    communicativeGoal = task.communicativeGoal,
                    requiredActions = emptyList(),
                    callOutcome = outcome,
                    failure = session.failure,
                    hasUserSpeech = session.hasUserSpeech,
                    transcript = session.transcript.toList(),
                    audioDurationMillis = session.assessmentAudioDurationMillis,
                    assessmentAudioId = session.assessmentAudioId
                )
                val assessment = assessmentRepository.evaluate(evidence)
                if (PlacementRules.isExpired(saved, clock.millis())) {
                    startNewAssessment()
                    return@launch
                }
                when (val transition = PlacementRules.applyOutcome(saved, task, assessment, tasks, random, clock.millis())) {
                    is PlacementTransition.NextTask -> {
                        saveCheckpoint(transition.checkpoint)
                        _uiState.update {
                            it.copy(
                                phase = PlacementPhase.NEXT_TASK,
                                nextTask = PlacementNextTaskMapper.classify(saved.step, transition.checkpoint.step),
                                scenario = null
                            )
                        }
                    }
                    is PlacementTransition.RetryTask -> {
                        saveCheckpoint(transition.checkpoint)
                        _uiState.update {
                            it.copy(phase = PlacementPhase.INSUFFICIENT_EVIDENCE, canRetry = true, insufficientReason = transition.reason)
                        }
                    }
                    is PlacementTransition.Confirmed -> {
                        checkpoint = transition.checkpoint
                        _uiState.update { it.copy(phase = PlacementPhase.RESULT, result = transition.result, scenario = null) }
                        saveResult()
                    }
                    is PlacementTransition.Stopped -> stopAssessment(transition.reason)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                showUndetermined()
            } finally {
                session.assessmentAudioId?.let { clearAudio(it) }
            }
        }
    }

    fun retry() {
        if (_uiState.value.phase != PlacementPhase.INSUFFICIENT_EVIDENCE || !_uiState.value.canRetry) return
        launchOperation { openCurrentTask() }
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
        _uiState.update { it.copy(isSavingResult = true, saveFailed = false) }
        try {
            check(authRepository.requireCurrentUserId() == uid && saved.userId == uid)
            checkpointRepository.saveCheckpoint(saved)
            userRepository.saveAssessedLevel(
                result.level,
                result.speakingProfile,
                result.assessedAtMillis,
                TrainingProgress(0, 0, TrainingStage.UNIT)
            ).getOrThrow()
            checkpointRepository.clearCheckpoint(uid)
            checkpoint = null
            _uiState.update { it.copy(isResultSaved = true) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            _uiState.update { it.copy(saveFailed = true) }
        } finally {
            _uiState.update { it.copy(isSavingResult = false) }
        }
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
        checkpointRepository.clearCheckpoint(uid)
        checkpoint = null
        tasks = taskRepository.getAssessmentTasks().toList()
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
            startNewAssessment()
            return
        }
        if (tasks.isEmpty()) tasks = taskRepository.getAssessmentTasks().toList()
        val task = PlacementRules.selectTask(saved.step, tasks, saved, random)
        val scenario = task?.let { scenarioRepository.getScenario(it.scenarioId) }
        if (task == null || scenario == null) {
            stopAssessment(PlacementStopReason.NO_TASK_AVAILABLE)
            return
        }
        _uiState.update { PlacementUiState(phase = PlacementPhase.CALLING, currentTask = task, scenario = scenario) }
    }

    private suspend fun saveCheckpoint(updated: PlacementCheckpoint) {
        checkpointRepository.saveCheckpoint(updated)
        checkpoint = updated
    }

    private suspend fun stopAssessment(reason: PlacementStopReason) {
        val uid = userId
        if (uid != null) checkpointRepository.clearCheckpoint(uid)
        checkpoint = null
        _uiState.update { it.copy(phase = PlacementPhase.UNDETERMINED, stopReason = reason, canRetry = false, scenario = null) }
    }

    private fun showUndetermined() {
        _uiState.update { it.copy(phase = PlacementPhase.UNDETERMINED, canRetry = false, scenario = null) }
    }

    private fun leave(shouldClearCheckpoint: Boolean) {
        if (isLeaving) return
        isLeaving = true
        val uid = userId
        viewModelScope.launch {
            operationJob?.cancelAndJoin()
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

    private suspend fun clearAudio(audioId: String) {
        withContext(NonCancellable) {
            try {
                assessmentRepository.clearAudioEvidence(audioId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                Unit
            }
        }
    }
}
