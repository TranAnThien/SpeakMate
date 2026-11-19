package com.project.speakmate.ui.placement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentTask
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.ReassessmentTransition
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.AssessmentTaskRepository
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.ScenarioRepository
import com.project.speakmate.domain.repository.SpeakingAssessmentRepository
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.domain.rules.ReassessmentRules
import com.project.speakmate.domain.rules.TrainingProgressRules
import com.project.speakmate.ui.placement.state.ReassessmentPhase
import com.project.speakmate.ui.placement.state.ReassessmentUiState
import com.project.speakmate.utils.extension.default
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ReassessmentViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val taskRepository: AssessmentTaskRepository,
    private val scenarioRepository: ScenarioRepository,
    private val assessmentRepository: SpeakingAssessmentRepository,
    private val trainingRepository: TrainingRepository,
    private val clock: Clock
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReassessmentUiState())
    val uiState: StateFlow<ReassessmentUiState> = _uiState.asStateFlow()
    private var userId: String? = null
    private var tasks: List<AssessmentTask> = emptyList()
    private var operationJob: Job? = null
    private var generation = 0L

    init {
        viewModelScope.launch {
            try {
                authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                    operationJob?.cancel()
                    generation++
                    userId = uid
                    tasks = emptyList()
                    _uiState.update { ReassessmentUiState() }
                    if (uid != null) launchOperation { owner, token -> loadEligibility(owner, token) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { ReassessmentUiState(phase = ReassessmentPhase.ERROR) }
            }
        }
    }

    fun start() {
        if (_uiState.value.phase != ReassessmentPhase.INTRODUCTION) return
        launchOperation { uid, token ->
            val profile = getProfile(uid, token)
            if (!validateEligibility(profile, uid, token)) return@launchOperation
            _uiState.update {
                it.copy(currentLevel = profile.level, nextLevel = checkNotNull(profile.level.getNextLevel()))
            }
            tasks = taskRepository.getAssessmentTasks().toList()
            ensureCurrent(uid, token)
            when (val transition = ReassessmentRules.start(profile, tasks, Random.Default)) {
                is ReassessmentTransition.NextStep -> {
                    _uiState.update { it.copy(assessment = transition.state) }
                    openCurrentTask(uid, token)
                }
                else -> finish(transition)
            }
        }
    }

    fun continueAssessment() {
        if (_uiState.value.phase != ReassessmentPhase.NEXT_TASK) return
        launchOperation { uid, token -> openCurrentTask(uid, token) }
    }

    fun retryTask() {
        if (_uiState.value.phase != ReassessmentPhase.RESULT || _uiState.value.result !is ReassessmentTransition.RetryTask) return
        launchOperation { uid, token -> openCurrentTask(uid, token) }
    }

    fun retryLoad() {
        if (_uiState.value.phase != ReassessmentPhase.ERROR) return
        launchOperation { uid, token ->
            if (_uiState.value.assessment == null) loadEligibility(uid, token) else openCurrentTask(uid, token)
        }
    }

    fun retrySave() {
        if (_uiState.value.phase != ReassessmentPhase.RESULT || !_uiState.value.isSaveFailed || _uiState.value.isSaving) return
        launchOperation(shouldShowLoading = false) { uid, token -> saveResult(uid, token) }
    }

    fun submitCallResult(session: CallSessionState) {
        val state = _uiState.value
        val assessment = state.assessment ?: return
        val task = assessment.currentTask ?: return
        val uid = userId ?: return
        val callOutcome = session.outcome ?: return
        if (state.phase != ReassessmentPhase.CALLING || session.state !in listOf(CallState.ENDED, CallState.FAILED)) return
        val previousOperation = operationJob
        val token = ++generation
        _uiState.update { it.copy(phase = ReassessmentPhase.GRADING, scenario = null, hasReachedAudioLimit = session.hasReachedAssessmentAudioLimit) }
        operationJob = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            var hasClearedAudio = false
            try {
                previousOperation?.join()
                ensureCurrent(uid, token)
                val outcome = try {
                    assessmentRepository.evaluate(AssessmentEvidence(
                        targetLevel = task.targetLevel,
                        communicativeGoal = task.communicativeGoal,
                        requiredActions = emptyList(),
                        callOutcome = callOutcome,
                        failure = session.failure,
                        hasUserSpeech = session.hasUserSpeech,
                        transcript = session.transcript.toList(),
                        audioDurationMillis = session.assessmentAudioDurationMillis,
                        assessmentAudioId = session.assessmentAudioId
                    ))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_FAILED)
                }
                ensureCurrent(uid, token)
                session.assessmentAudioId?.let { audioId -> clearAudio(audioId) }
                hasClearedAudio = true
                ensureCurrent(uid, token)
                when (val transition = ReassessmentRules.applyOutcome(assessment, task, outcome, tasks, Random.Default, clock.millis())) {
                    is ReassessmentTransition.NextStep -> _uiState.update {
                        it.copy(phase = ReassessmentPhase.NEXT_TASK, assessment = transition.state, result = null)
                    }
                    is ReassessmentTransition.RetryTask -> _uiState.update {
                        it.copy(phase = ReassessmentPhase.RESULT, assessment = transition.state, result = transition)
                    }
                    is ReassessmentTransition.Promoted -> {
                        finish(transition)
                        saveResult(uid, token)
                    }
                    else -> finish(transition)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == token) _uiState.update { it.copy(phase = ReassessmentPhase.ERROR) }
            } finally {
                if (!hasClearedAudio) session.assessmentAudioId?.let { audioId -> clearAudio(audioId) }
            }
        }
    }

    fun exit() {
        generation++
        operationJob?.cancel()
        tasks = emptyList()
        _uiState.update { ReassessmentUiState(phase = ReassessmentPhase.CLOSED) }
    }

    private fun launchOperation(shouldShowLoading: Boolean = true, operation: suspend (String, Long) -> Unit) {
        val uid = userId ?: return
        if (_uiState.value.phase == ReassessmentPhase.CLOSED || operationJob?.isActive == true) return
        val token = ++generation
        if (shouldShowLoading) _uiState.update { it.copy(phase = ReassessmentPhase.LOADING) }
        operationJob = viewModelScope.launch {
            try {
                operation(uid, token)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == token) _uiState.update { it.copy(phase = ReassessmentPhase.ERROR) }
            }
        }
    }

    private suspend fun loadEligibility(uid: String, token: Long) {
        val profile = getProfile(uid, token)
        _uiState.update {
            ReassessmentUiState(currentLevel = profile.level, nextLevel = profile.level.getNextLevel().default(SpeakingLevel.NOT_ASSESSED))
        }
        if (validateEligibility(profile, uid, token)) _uiState.update { it.copy(phase = ReassessmentPhase.INTRODUCTION) }
    }

    private suspend fun getProfile(uid: String, token: Long): User {
        val profile = checkNotNull(userRepository.getUser(uid).getOrThrow())
        ensureCurrent(uid, token)
        check(profile.userId == uid)
        return profile
    }

    private suspend fun validateEligibility(profile: User, uid: String, token: Long): Boolean {
        if (!ReassessmentRules.canStart(profile)) {
            finish(ReassessmentRules.start(profile, emptyList(), Random.Default))
            return false
        }
        val path = trainingRepository.getTrainingPath(profile.level).getOrThrow()
        ensureCurrent(uid, token)
        check(path.targetLevel == profile.level)
        check(TrainingProgressRules.validate(path, checkNotNull(profile.trainingProgress)) == null)
        return true
    }

    private suspend fun openCurrentTask(uid: String, token: Long) {
        val state = checkNotNull(_uiState.value.assessment)
        val profile = getProfile(uid, token)
        check(profile.level == state.currentLevel)
        if (!validateEligibility(profile, uid, token)) return
        val task = checkNotNull(ReassessmentRules.selectTask(state, tasks, Random.Default))
        val scenario = checkNotNull(scenarioRepository.getScenario(task.scenarioId))
        ensureCurrent(uid, token)
        check(scenario.scenarioId == task.scenarioId)
        _uiState.update { it.copy(phase = ReassessmentPhase.CALLING, scenario = scenario, result = null) }
    }

    private fun finish(transition: ReassessmentTransition) {
        _uiState.update { it.copy(phase = ReassessmentPhase.RESULT, result = transition, scenario = null) }
    }

    private suspend fun saveResult(uid: String, token: Long) {
        val result = _uiState.value.result as? ReassessmentTransition.Promoted ?: return
        _uiState.update { it.copy(isSaving = true, isSaveFailed = false) }
        try {
            val profile = getProfile(uid, token)
            val isAlreadySaved = profile.level == result.newLevel &&
                profile.lastAssessedAt?.time == result.lastAssessedAtMillis && profile.latestAssessmentCriteria == result.latestAssessmentCriteria
            check(isAlreadySaved || (profile.level == _uiState.value.currentLevel && ReassessmentRules.canStart(profile)))
            ensureCurrent(uid, token)
            userRepository.saveAssessedLevel(result.newLevel, result.latestAssessmentCriteria, result.lastAssessedAtMillis, result.newProgress).getOrThrow()
            ensureCurrent(uid, token)
            _uiState.update { it.copy(isSaved = true) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (generation == token) _uiState.update { it.copy(isSaveFailed = true) }
        } finally {
            if (generation == token) _uiState.update { it.copy(isSaving = false) }
        }
    }

    private suspend fun ensureCurrent(uid: String, token: Long) {
        currentCoroutineContext().ensureActive()
        check(generation == token && userId == uid && authRepository.requireCurrentUserId() == uid)
    }

    private suspend fun clearAudio(audioId: String) = withContext(NonCancellable) {
        try {
            assessmentRepository.clearAudioEvidence(audioId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Unit
        }
    }
}
