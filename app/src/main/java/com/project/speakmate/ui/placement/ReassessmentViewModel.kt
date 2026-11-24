package com.project.speakmate.ui.placement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.ReassessmentState
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentOwnershipException
import com.project.speakmate.domain.model.AssessmentTask
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.PracticeRecoveryReason
import com.project.speakmate.domain.model.ReassessmentTransition
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.AssessmentGradingRepository
import com.project.speakmate.domain.repository.AssessmentTaskRepository
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.ScenarioRepository
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.domain.rules.PracticeCompletionRules
import com.project.speakmate.domain.rules.ReassessmentRules
import com.project.speakmate.domain.rules.TrainingProgressRules
import com.project.speakmate.ui.placement.state.ReassessmentNavigation
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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class ReassessmentViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val taskRepository: AssessmentTaskRepository,
    private val scenarioRepository: ScenarioRepository,
    private val assessmentRepository: AssessmentGradingRepository,
    private val trainingRepository: TrainingRepository,
    private val clock: Clock
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReassessmentUiState())
    val uiState: StateFlow<ReassessmentUiState> = _uiState.asStateFlow()
    private var userId: String? = null
    private var tasks: List<AssessmentTask> = emptyList()
    private var operationJob: Job? = null
    private var generation = 0L
    private var startedAtMillis: Long? = null

    init {
        viewModelScope.launch {
            assessmentRepository.gradingState.collect { grading ->
                if (grading.isExpired) {
                    operationJob?.cancel()
                    if (_uiState.value.phase != ReassessmentPhase.CLOSED) {
                        _uiState.update {
                            it.copy(
                                phase = ReassessmentPhase.ERROR,
                                assessment = null,
                                result = null,
                                pendingGradingReason = null,
                                recoveryProblem = PracticeRecoveryReason.ASSESSMENT_EXPIRED
                            )
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            try {
                authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                    generation++
                    operationJob?.cancelAndJoin()
                    assessmentRepository.clear()
                    startedAtMillis = null
                    userId = uid
                    tasks = emptyList()
                    _uiState.update {
                        ReassessmentUiState(
                            phase = if (uid == null) ReassessmentPhase.ERROR else ReassessmentPhase.LOADING
                        )
                    }
                    if (uid != null) launchOperation { owner, token -> loadEligibility(owner, token) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                generation++
                operationJob?.cancelAndJoin()
                assessmentRepository.clear()
                _uiState.update { ReassessmentUiState(phase = ReassessmentPhase.ERROR) }
            }
        }
    }

    fun start() {
        if (_uiState.value.phase != ReassessmentPhase.INTRODUCTION) return
        launchOperation { uid, token ->
            val profile = getProfile(uid, token)
            if (!validateEligibility(profile = profile, uid = uid, token = token)) return@launchOperation
            ensureCurrent(uid, token)
            _uiState.update {
                it.copy(currentLevel = profile.level, nextLevel = checkNotNull(profile.level.getNextLevel()))
            }
            tasks = taskRepository.getAssessmentTasks().toList()
            ensureCurrent(uid, token)
            when (val transition = ReassessmentRules.start(user = profile, tasks = tasks, random = Random.Default)) {
                is ReassessmentTransition.NextStep -> {
                    startedAtMillis = clock.millis()
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
        if (_uiState.value.pendingGradingReason != null) {
            retryGrading()
            return
        }
        if (_uiState.value.phase != ReassessmentPhase.RESULT ||
            _uiState.value.result !is ReassessmentTransition.RetryTask)
            return
        launchOperation { uid, token -> openCurrentTask(uid, token) }
    }

    fun retryLoad() {
        if (_uiState.value.phase != ReassessmentPhase.ERROR) return
        _uiState.update { it.copy(recoveryProblem = null) }
        launchOperation { uid, token ->
            if (_uiState.value.assessment == null) loadEligibility(uid, token) else openCurrentTask(uid, token)
        }
    }

    fun retrySave() {
        if (_uiState.value.phase != ReassessmentPhase.RESULT || !_uiState.value.isSaveFailed || _uiState.value.isSaving)
            return
        launchOperation(shouldShowLoading = false) { uid, token -> saveResult(uid, token) }
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
        val assessment = state.assessment ?: return
        val task = assessment.currentTask ?: return
        val uid = userId ?: return
        val startedAt = startedAtMillis ?: return
        if (!isRetry &&
            (state.phase != ReassessmentPhase.CALLING ||
                session?.outcome == null ||
                session.state !in listOf(CallState.ENDED, CallState.FAILED)))
            return
        val previousOperation = operationJob
        val token = ++generation
        showGrading(state, session)
        operationJob = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                previousOperation?.join()
                gradeCurrentTask(
                    session = session,
                    isRetry = isRetry,
                    task = task,
                    assessment = assessment,
                    uid = uid,
                    token = token,
                    startedAt = startedAt
                )
            } catch (failure: Exception) {
                handleGradingFailure(failure, token)
            }
        }
    }

    private fun showGrading(state: ReassessmentUiState, session: CallSessionState?) {
        _uiState.update {
            it.copy(
                phase = ReassessmentPhase.GRADING,
                pendingGradingReason = null,
                hasReachedAudioLimit = session?.hasReachedAssessmentAudioLimit.default(state.hasReachedAudioLimit)
            )
        }
    }

    private fun handleGradingFailure(failure: Exception, token: Long) {
        when (failure) {
            is CancellationException -> throw failure
            is AssessmentOwnershipException -> Unit
            else -> if (generation == token)
                _uiState.update { it.copy(phase = ReassessmentPhase.ERROR, pendingGradingReason = null) }
        }
    }

    private suspend fun gradeCurrentTask(
        session: CallSessionState?,
        isRetry: Boolean,
        task: AssessmentTask,
        assessment: ReassessmentState,
        uid: String,
        token: Long,
        startedAt: Long
    ) {
        ensureCurrent(uid, token)
        val outcome = getAssessmentOutcome(
            session = session,
            isRetry = isRetry,
            task = task,
            uid = uid,
            startedAt = startedAt
        )
        ensureCurrent(uid, token)
        if (expireGradingResult(uid = uid, token = token, startedAt = startedAt)) return
        val transition =
            ReassessmentRules.applyOutcome(
                state = assessment,
                task = task,
                outcome = outcome,
                tasks = tasks,
                random = Random.Default,
                nowMillis = clock.millis()
            )
        applyGradingTransition(
            transition = transition,
            outcome = outcome,
            uid = uid,
            token = token
        )
    }

    private suspend fun getAssessmentOutcome(
        session: CallSessionState?,
        isRetry: Boolean,
        task: AssessmentTask,
        uid: String,
        startedAt: Long
    ): AssessmentOutcome {
        return if (isRetry) assessmentRepository.retry()
        else {
            val completed = checkNotNull(session)
            assessmentRepository.gradeCallResult(
                task = task,
                session = completed,
                userId = uid,
                expiresAtMillis = ReassessmentRules.getEvidenceExpiryMillis(startedAt)
            )
        }
    }

    private suspend fun applyGradingTransition(
        transition: ReassessmentTransition,
        outcome: AssessmentOutcome,
        uid: String,
        token: Long
    ) {
        val pendingReason = (outcome as? AssessmentOutcome.InsufficientEvidence)?.reason
        val canRetryGrading =
            transition is ReassessmentTransition.RetryTask &&
                assessmentRepository.gradingState.value.canRetry
        if (!canRetryGrading) assessmentRepository.clear()
        when (transition) {
            is ReassessmentTransition.NextStep ->
                _uiState.update { ReassessmentUiMapper.toNextStep(it, transition) }
            is ReassessmentTransition.RetryTask -> {
                _uiState.update {
                    it.copy(
                        phase = ReassessmentPhase.RESULT,
                        assessment = transition.state,
                        result = transition,
                        pendingGradingReason = if (canRetryGrading) pendingReason else null
                    )
                }
            }
            is ReassessmentTransition.Promoted -> {
                finish(transition)
                saveResult(uid, token)
            }
            else -> finish(transition)
        }
    }

    private suspend fun expireGradingResult(uid: String, token: Long, startedAt: Long): Boolean {
        if (PracticeCompletionRules.isEvidenceExpired(startedAt, clock.millis()) ||
            clock.millis() < startedAt) {
            ensureCurrent(uid, token)
            _uiState.update {
                it.copy(
                    phase = ReassessmentPhase.ERROR,
                    assessment = null,
                    result = null,
                    recoveryProblem = PracticeRecoveryReason.ASSESSMENT_EXPIRED
                )
            }
            return true
        }
        return false
    }

    fun exit() {
        val state = _uiState.value
        val destination =
            when {
                state.currentLevel == SpeakingLevel.NOT_ASSESSED -> ReassessmentNavigation.OPEN_HOME
                state.isSaved -> ReassessmentNavigation.OPEN_PROMOTED_PATH
                else -> ReassessmentNavigation.RETURN_TO_PATH
            }
        leave(destination)
    }

    fun openProfile() {
        leave(ReassessmentNavigation.OPEN_PROFILE)
    }

    fun consumePendingNavigation(): ReassessmentNavigation? {
        while (true) {
            val current = _uiState.value
            val destination = current.pendingNavigation ?: return null
            if (_uiState.compareAndSet(current, current.copy(pendingNavigation = null))) {
                return destination
            }
        }
    }

    private fun leave(destination: ReassessmentNavigation) {
        if (_uiState.value.phase == ReassessmentPhase.CLOSED) {
            _uiState.update { it.copy(pendingNavigation = destination) }
            return
        }
        generation++
        operationJob?.cancel()
        assessmentRepository.clear()
        tasks = emptyList()
        _uiState.update {
            ReassessmentUiState(
                phase = ReassessmentPhase.CLOSED,
                currentLevel = it.currentLevel,
                isSaved = it.isSaved,
                pendingNavigation = destination
            )
        }
    }

    private fun launchOperation(shouldShowLoading: Boolean = true, operation: suspend (String, Long) -> Unit) {
        val uid = userId ?: return
        if (_uiState.value.phase == ReassessmentPhase.CLOSED || operationJob?.isActive == true) return
        val token = ++generation
        if (shouldShowLoading) _uiState.update { it.copy(phase = ReassessmentPhase.LOADING) }
        operationJob =
            viewModelScope.launch {
                try {
                    operation(uid, token)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: AssessmentOwnershipException) {
                    Unit
                } catch (_: Exception) {
                    if (generation == token)
                        _uiState.update { it.copy(phase = ReassessmentPhase.ERROR, pendingGradingReason = null) }
                }
            }
    }

    private suspend fun loadEligibility(uid: String, token: Long) {
        val profile = getProfile(uid, token)
        _uiState.update {
            ReassessmentUiState(
                currentLevel = profile.level,
                nextLevel = profile.level.getNextLevel().default(SpeakingLevel.NOT_ASSESSED)
            )
        }
        ensureCurrent(uid, token)
        if (validateEligibility(profile = profile, uid = uid, token = token))
            _uiState.update { it.copy(phase = ReassessmentPhase.INTRODUCTION) }
    }

    private suspend fun getProfile(uid: String, token: Long): User {
        val profile = checkNotNull(userRepository.getUser(userId = uid, requireServer = true).getOrThrow())
        ensureCurrent(uid, token)
        check(profile.userId == uid)
        return profile
    }

    private suspend fun validateEligibility(profile: User, uid: String, token: Long): Boolean {
        if (!ReassessmentRules.canStart(profile)) {
            finish(ReassessmentRules.start(user = profile, tasks = emptyList(), random = Random.Default))
            return false
        }
        val path = trainingRepository.getTrainingPath(level = profile.level, requireServer = true).getOrThrow()
        ensureCurrent(uid, token)
        check(path.targetLevel == profile.level)
        check(TrainingProgressRules.validate(path, checkNotNull(profile.trainingProgress)) == null)
        return true
    }

    private suspend fun openCurrentTask(uid: String, token: Long) {
        val startedAt = startedAtMillis ?: throw IllegalStateException()
        ensureCurrent(uid, token)
        if (PracticeCompletionRules.isEvidenceExpired(startedAt, clock.millis())) {
            ensureCurrent(uid, token)
            _uiState.update {
                it.copy(
                    phase = ReassessmentPhase.ERROR,
                    assessment = null,
                    result = null,
                    pendingGradingReason = null,
                    recoveryProblem = PracticeRecoveryReason.ASSESSMENT_EXPIRED
                )
            }
            return
        }
        val state = checkNotNull(_uiState.value.assessment)
        val profile = getProfile(uid, token)
        check(profile.level == state.currentLevel)
        if (!validateEligibility(profile = profile, uid = uid, token = token)) return
        tasks = taskRepository.getAssessmentTasks().toList()
        ensureCurrent(uid, token)
        val task = ReassessmentRules.selectTask(state = state, tasks = tasks, random = Random.Default)
        val scenario = task?.let { scenarioRepository.getScenario(scenarioId = it.scenarioId, requireServer = true) }
        ensureCurrent(uid, token)
        if (task == null || scenario == null) {
            finish(ReassessmentTransition.Stopped(ReassessmentTransition.StopReason.NO_TASK_AVAILABLE))
            return
        }
        if (PracticeCompletionRules.isEvidenceExpired(startedAt, clock.millis())) {
            expireResult(uid, PracticeRecoveryReason.ASSESSMENT_EXPIRED)
            return
        }
        check(scenario.scenarioId == task.scenarioId)
        _uiState.update { it.copy(phase = ReassessmentPhase.CALLING, scenario = scenario, result = null) }
    }

    private fun finish(transition: ReassessmentTransition) {
        _uiState.update { it.copy(phase = ReassessmentPhase.RESULT, result = transition, scenario = null) }
    }

    private suspend fun saveResult(uid: String, token: Long) {
        val result = _uiState.value.result as? ReassessmentTransition.Promoted ?: return
        ensureCurrent(uid, token)
        _uiState.update { it.copy(isSaving = true, isSaveFailed = false) }
        try {
            val profile = getProfile(uid, token)
            check(
                ReassessmentRules.canSaveResult(
                    profile = profile,
                    result = result,
                    currentLevel = _uiState.value.currentLevel
                )
            )
            ensureCurrent(uid, token)
            userRepository
                .saveAssessedLevel(
                    level = result.newLevel,
                    criteria = result.latestAssessmentCriteria,
                    assessedAtMillis = result.lastAssessedAtMillis,
                    trainingProgress = result.newProgress
                )
                .getOrThrow()
            ensureCurrent(uid, token)
            _uiState.update { it.copy(isSaved = true) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: AssessmentOwnershipException) {
            Unit
        } catch (_: Exception) {
            if (generation == token) _uiState.update { it.copy(isSaveFailed = true) }
        } finally {
            if (generation == token) _uiState.update { it.copy(isSaving = false) }
        }
    }

    private suspend fun expireResult(
        uid: String,
        reason: PracticeRecoveryReason = PracticeRecoveryReason.ASSESSMENT_EXPIRED
    ) {
        val token = generation
        ensureCurrent(uid, token)
        _uiState.update {
            it.copy(
                phase = ReassessmentPhase.ERROR,
                assessment = null,
                result = null,
                isSaveFailed = false,
                isSaving = false,
                recoveryProblem = reason
            )
        }
    }

    private suspend fun ensureCurrent(uid: String, token: Long) {
        currentCoroutineContext().ensureActive()
        val currentUserId =
            try {
                authRepository.requireCurrentUserId()
            } catch (_: IllegalStateException) {
                null
            }
        if (generation != token || userId != uid || currentUserId != uid) throw AssessmentOwnershipException()
    }
}
