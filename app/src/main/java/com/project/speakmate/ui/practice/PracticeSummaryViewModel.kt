package com.project.speakmate.ui.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.project.speakmate.app.AppRoute
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeCompletionState
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.durationInSeconds
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.ui.components.transcript.TranscriptMessageUiMapper
import com.project.speakmate.ui.practice.state.PracticeSummaryPresentation
import com.project.speakmate.ui.practice.state.PracticeSummaryUiState
import com.project.speakmate.utils.extension.defaultEmpty
import com.project.speakmate.utils.extension.toDurationLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PracticeSummaryViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val completionRepository: PracticeCompletionRepository,
    private val historyRepository: HistoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val route =
        try {
            savedStateHandle.toRoute<AppRoute.PracticeSummary>()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    private val attemptId = route?.attemptId.defaultEmpty()
    private val isRouteValid = attemptId.isNotBlank() && '/' !in attemptId
    private val _uiState = MutableStateFlow(PracticeSummaryUiState(scenarioTitle = route?.scenarioTitle.defaultEmpty()))
    val uiState: StateFlow<PracticeSummaryUiState> = _uiState.asStateFlow()
    private var observationJob: Job? = null

    init {
        observeResult()
    }

    override fun onCleared() {
        if (isRouteValid) completionRepository.releaseCompletion(attemptId)
        super.onCleared()
    }

    fun retry() {
        if (!isRouteValid) return
        _uiState.update { it.copy(presentation = PracticeSummaryPresentation.FINALIZING) }
        observeResult()
    }

    fun retrySave() {
        if (_uiState.value.presentation == PracticeSummaryPresentation.SAVE_FAILED && _uiState.value.canRetrySave)
            completionRepository.retrySave(attemptId)
    }

    fun getSavedAttemptId(): String? =
        attemptId.takeIf { isRouteValid && _uiState.value.presentation == PracticeSummaryPresentation.SAVED }

    fun openTranscript(): String? {
        val savedAttemptId = getSavedAttemptId()
        if (savedAttemptId == null) showTranscript()
        return savedAttemptId
    }

    fun showTranscript() {
        _uiState.update { it.copy(isTranscriptVisible = it.messages.isNotEmpty()) }
    }

    fun hideTranscript() {
        _uiState.update { it.copy(isTranscriptVisible = false) }
    }

    private fun observeResult() {
        observationJob?.cancel()
        if (!isRouteValid) {
            _uiState.value =
                PracticeSummaryUiState(presentation = PracticeSummaryPresentation.UNAVAILABLE, canReload = false)
            return
        }
        observationJob =
            viewModelScope.launch {
                try {
                    val userId = authRepository.requireCurrentUserId()
                    completionRepository.observeCompletion(attemptId).distinctUntilChanged().collect { state ->
                        if (authRepository.requireCurrentUserId() != userId) return@collect
                        when (state) {
                            is PracticeCompletionState.Saving ->
                                showAttempt(state.completion.attempt, PracticeSummaryPresentation.SAVING)
                            is PracticeCompletionState.Saved ->
                                showAttempt(state.completion.attempt, PracticeSummaryPresentation.SAVED)
                            is PracticeCompletionState.SaveFailed -> {
                                showAttempt(
                                    state.completion.attempt,
                                    if (state.isSaving) PracticeSummaryPresentation.SAVING
                                    else PracticeSummaryPresentation.SAVE_FAILED
                                )
                                _uiState.update { it.copy(saveFailure = state.reason) }
                            }
                            PracticeCompletionState.Unavailable -> {
                                val saved =
                                    historyRepository
                                        .getSessionDetail(attemptId = attemptId, requireServer = true)
                                        .getOrThrow()
                                check(authRepository.requireCurrentUserId() == userId)
                                showAttempt(saved, PracticeSummaryPresentation.SAVED)
                            }
                            else -> _uiState.update { it.copy(presentation = PracticeSummaryPresentation.FINALIZING) }
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _uiState.update {
                        it.copy(
                            presentation = PracticeSummaryPresentation.UNAVAILABLE,
                            messages = emptyList(),
                            isTranscriptVisible = false
                        )
                    }
                }
            }
    }

    private fun showAttempt(attempt: PracticeAttempt, presentation: PracticeSummaryPresentation) {
        check(attempt.attemptId == attemptId && attempt.practiceType == PracticeType.FREE_PRACTICE)
        _uiState.update {
            it.copy(
                presentation = presentation,
                sessionDescription = attempt.durationInSeconds.toDurationLabel(),
                messages = TranscriptMessageUiMapper.toMessages(attempt.transcripts),
                saveFailure = null
            )
        }
    }
}
