package com.project.speakmate.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.project.speakmate.app.AppRoute
import com.project.speakmate.domain.model.HistoryReadException
import com.project.speakmate.domain.model.HistoryReadFailure
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeCompletion
import com.project.speakmate.domain.model.PracticeCompletionState
import com.project.speakmate.domain.model.PracticeCompletionStatus
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TranscriptMessage
import com.project.speakmate.domain.model.durationInSeconds
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.domain.rules.PracticeHistoryRules
import com.project.speakmate.ui.components.AssessmentCriterionType
import com.project.speakmate.ui.components.TranscriptMessageUiMapper
import com.project.speakmate.ui.history.state.SessionActionUiState
import com.project.speakmate.ui.history.state.SessionCriterionUiState
import com.project.speakmate.ui.history.state.SessionDetailPresentation
import com.project.speakmate.ui.history.state.SessionDetailUiState
import com.project.speakmate.utils.extension.defaultEmpty
import com.project.speakmate.utils.extension.toDurationLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val historyRepository: HistoryRepository,
    private val practiceRepository: PracticeRepository,
    private val completionRepository: PracticeCompletionRepository,
    private val trainingRepository: TrainingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    companion object {
        private const val CONTENT_TIMEOUT_MILLIS = 3_000L
    }

    private val attemptId = try { savedStateHandle.toRoute<AppRoute.SessionDetail>().attemptId } catch (_: IllegalArgumentException) { "" }
    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var messagesJob: Job? = null
    private var metadataJob: Job? = null
    private var currentUserId: String? = null
    private var revision = 0L

    init { loadSessionDetail() }

    override fun onCleared() {
        completionRepository.releaseCompletion(attemptId)
        super.onCleared()
    }

    fun loadSessionDetail() {
        loadJob?.cancel()
        messagesJob?.cancel()
        metadataJob?.cancel()
        revision++
        loadJob = viewModelScope.launch {
            try {
                require(attemptId.isNotBlank() && !attemptId.contains('/'))
                authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                    if (uid != currentUserId) {
                        revision++
                        messagesJob?.cancel()
                        metadataJob?.cancel()
                        _uiState.value = SessionDetailUiState()
                    }
                    currentUserId = uid
                    if (uid == null) {
                        _uiState.value = SessionDetailUiState(presentation = SessionDetailPresentation.LOAD_ERROR, loadFailure = HistoryReadFailure.ACCESS_DENIED)
                        return@collectLatest
                    }
                    completionRepository.observeCompletion(attemptId).collectLatest { state ->
                        check(authRepository.requireCurrentUserId() == uid)
                        revision++
                        messagesJob?.cancel()
                        metadataJob?.cancel()
                        when (state) {
                            is PracticeCompletionState.Saving -> showTemporary(state.completion, uid, PracticeCompletionStatus.SAVING)
                            is PracticeCompletionState.SaveFailed -> {
                                showTemporary(state.completion, uid, if (state.isSaving) PracticeCompletionStatus.SAVING else PracticeCompletionStatus.SAVE_FAILED)
                                _uiState.update { it.copy(saveFailure = state.reason) }
                            }
                            is PracticeCompletionState.Saved -> {
                                if (_uiState.value.isTemporary || _uiState.value.result == null) showTemporary(state.completion, uid, PracticeCompletionStatus.SAVING)
                                _uiState.update { it.copy(isSaved = true) }
                                loadPersisted(uid)
                            }
                            PracticeCompletionState.Unavailable -> {
                                if (_uiState.value.isTemporary) _uiState.value = SessionDetailUiState()
                                loadPersisted(uid)
                            }
                            PracticeCompletionState.Grading -> _uiState.update {
                                if (it.result != null) it.copy(hasSavedRefreshError = false, isTranscriptLoading = false)
                                else SessionDetailUiState(pendingStatus = PracticeCompletionStatus.GRADING)
                            }
                            is PracticeCompletionState.AwaitingGrading -> _uiState.update { SessionDetailUiState(presentation = SessionDetailPresentation.COMPLETED,
                                isTemporary = true, hasPendingContent = false, pendingStatus = PracticeCompletionStatus.AWAITING_GRADING) }
                        }
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                revision++
                messagesJob?.cancel()
                metadataJob?.cancel()
                _uiState.update { if (it.result != null &&
                    runCatching { authRepository.requireCurrentUserId() }.getOrNull() == currentUserId) it.copy(hasSavedRefreshError = true,
                        isTranscriptLoading = false) else SessionDetailUiState(presentation = SessionDetailPresentation.LOAD_ERROR) }
            }
        }
    }

    fun retry() {
        loadSessionDetail()
    }

    fun retryTranscript() {
        val uid = currentUserId ?: return
        if (!_uiState.value.isTemporary && _uiState.value.result != null && runCatching { authRepository.requireCurrentUserId() }.getOrNull() == uid) loadMessages(uid, revision)
    }

    fun retryGrading() {
        if (_uiState.value.pendingStatus == PracticeCompletionStatus.AWAITING_GRADING) completionRepository.retryGrading(attemptId)
    }

    fun saveUnassessed() {
        if (_uiState.value.pendingStatus == PracticeCompletionStatus.AWAITING_GRADING) completionRepository.saveUnassessed(attemptId)
    }

    fun retrySave() {
        if (_uiState.value.pendingStatus == PracticeCompletionStatus.SAVE_FAILED && _uiState.value.canRetrySave) completionRepository.retrySave(attemptId)
    }

    private fun showTemporary(completion: PracticeCompletion, uid: String, status: PracticeCompletionStatus) {
        check(completion.attempt.attemptId == attemptId)
        messagesJob?.cancel()
        metadataJob?.cancel()
        revision++
        val actions = completion.training?.requiredActions.defaultEmpty()
            .associate { it.requiredActionId to it.description } + completion.attempt.requiredActionDescriptions
        val state = toDetailUiState(completion.attempt, actions)
        _uiState.value = state.copy(title = completion.training?.scenario?.name.defaultEmpty().ifBlank { _uiState.value.title },
            messages = toMessageUiState(completion.attempt.transcripts), isTemporary = true, isTranscriptTemporary = true, pendingStatus = status)
        resolveContent(completion.attempt, uid, revision, actions)
    }

    private suspend fun loadPersisted(uid: String) {
        try {
            val attempt = historyRepository.getPracticeAttempt(attemptId, requireServer = true).getOrThrow()
            currentCoroutineContext().ensureActive()
            check(authRepository.requireCurrentUserId() == uid && attempt.attemptId == attemptId)
            revision++
            metadataJob?.cancel()
            val previous = _uiState.value
            val actions = previous.requiredActions.filter { it.text.isNotBlank() }
                .associate { it.id to it.text } + attempt.requiredActionDescriptions
            _uiState.value = toDetailUiState(attempt, actions).copy(title = previous.title, messages = previous.messages,
                isSaved = true, isTranscriptLoading = true, isTranscriptTemporary = previous.isTemporary || previous.isTranscriptTemporary)
            resolveContent(attempt, uid, revision, actions)
            loadMessages(uid, revision)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            currentCoroutineContext().ensureActive()
            if (authRepository.requireCurrentUserId() != uid) return
            val reason = (failure as? HistoryReadException)?.reason
            if (reason == HistoryReadFailure.NOT_FOUND || reason == HistoryReadFailure.ACCESS_DENIED) {
                messagesJob?.cancel()
                metadataJob?.cancel()
                revision++
                _uiState.value = SessionDetailUiState(presentation = SessionDetailPresentation.LOAD_ERROR, loadFailure = reason)
            } else if (_uiState.value.result != null) _uiState.update { it.copy(hasSavedRefreshError = true, isTranscriptLoading = false) }
            else _uiState.update { SessionDetailUiState(presentation = SessionDetailPresentation.LOAD_ERROR, loadFailure = reason) }
        }
    }

    private fun loadMessages(uid: String, request: Long) {
        messagesJob?.cancel()
        _uiState.update { it.copy(isTranscriptLoading = true) }
        messagesJob = viewModelScope.launch {
            try {
                val messages = historyRepository.getSessionMessages(attemptId, requireServer = true).getOrThrow()
                ensureActive()
                if (request != revision || authRepository.requireCurrentUserId() != uid) return@launch
                _uiState.update { it.copy(presentation = SessionDetailPresentation.COMPLETED, messages = toMessageUiState(messages),
                    isTranscriptLoading = false, isTranscriptTemporary = false, transcriptFailure = null) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (request == revision && runCatching { authRepository.requireCurrentUserId() }.getOrNull() == uid) _uiState.update { it
                    .copy(presentation = SessionDetailPresentation.TRANSCRIPT_ERROR, isTranscriptLoading = false, transcriptFailure = (failure as? HistoryReadException)?.reason) }
            }
        }
    }

    private fun resolveContent(attempt: PracticeAttempt, uid: String, request: Long, knownActions: Map<String, String>) {
        metadataJob?.cancel()
        metadataJob = viewModelScope.launch {
            val title = try {
                withTimeoutOrNull(CONTENT_TIMEOUT_MILLIS) { practiceRepository.getScenario(attempt.scenarioId).getOrThrow().name }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
            ensureActive()
            if (request != revision || runCatching { authRepository.requireCurrentUserId() }.getOrNull() != uid) return@launch
            if (!title.isNullOrBlank()) _uiState.update { it.copy(title = title) }
            if (knownActions.keys.containsAll(attempt.requiredActionResults.keys)) return@launch
            val paths = SpeakingLevel.entries.filter { it != SpeakingLevel.NOT_ASSESSED }.map { level ->
                try { withTimeoutOrNull(CONTENT_TIMEOUT_MILLIS) { trainingRepository.getTrainingPath(level).getOrThrow() } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
            }
            ensureActive()
            if (paths.any { it == null } || request != revision || runCatching { authRepository.requireCurrentUserId() }.getOrNull() != uid) return@launch
            val descriptions = PracticeHistoryRules.getActionDescriptions(attempt.scenarioId, attempt.practiceType, paths.filterNotNull()) + knownActions
            _uiState.update { it.copy(requiredActions = it.requiredActions.map { action -> action.copy(text = descriptions[action.id].defaultEmpty()) }) }
        }
    }

    private fun toDetailUiState(attempt: PracticeAttempt, actions: Map<String, String>): SessionDetailUiState {
        val values = attempt.criteriaResult?.let { listOf(it.rangeMet, it.accuracyMet, it.fluencyMet, it.interactionMet, it.coherenceMet) }
        return SessionDetailUiState(presentation = SessionDetailPresentation.COMPLETED,
            startedAtMillis = attempt.startedAt.time, duration = attempt.durationInSeconds.toDurationLabel(),
            result = HistoryResultMapper.toPresentation(attempt.result, attempt.practiceType), isAssessed = attempt.result != null,
                feedbackSummary = attempt.feedbackSummary.takeIf { attempt.result != null },
            requiredActions = attempt.requiredActionResults.map { (id, passed) -> SessionActionUiState(id, actions[id].defaultEmpty(), passed) },
            criteria = if (values == null) emptyList() else AssessmentCriterionType.entries.mapIndexed { index, type ->
                SessionCriterionUiState(id = type.name, isPassed = values[index], criterion = type) })
    }

    private fun toMessageUiState(messages: List<TranscriptMessage>) =
        TranscriptMessageUiMapper.toMessages(messages)
}
