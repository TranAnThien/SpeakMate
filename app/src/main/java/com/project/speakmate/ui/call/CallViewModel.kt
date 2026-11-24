package com.project.speakmate.ui.call

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.CompletedPractice
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.TrainingActivity
import com.project.speakmate.domain.model.TranscriptMessage
import com.project.speakmate.domain.repository.CallRepository
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.domain.rules.CallTimingRules
import com.project.speakmate.ui.call.state.CallDictionaryUiState
import com.project.speakmate.ui.call.state.CallDialog
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.call.state.CallTranscriptMessageUiState
import com.project.speakmate.ui.call.state.CallUiState
import com.project.speakmate.ui.call.state.MicrophonePermissionPresentation
import com.project.speakmate.ui.call.state.MicrophonePermissionUiState
import com.project.speakmate.utils.AppConstant
import com.project.speakmate.utils.extension.toDurationLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CallViewModel
@Inject
constructor(
    private val repository: CallRepository,
    private val dictionaryManager: CallDictionaryManager,
    private val completionRepository: PracticeCompletionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallUiState(isTranscriptAvailable = false))

    private val currentDictionary = { _uiState.value.dictionary }
    private val updateDictionary: ((CallDictionaryUiState?) -> CallDictionaryUiState?) -> Unit = { transform ->
        _uiState.update { it.copy(dictionary = transform(it.dictionary)) }
    }
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()
    val audioLevel: StateFlow<Float> = repository.audioLevel

    private var currentContext: CallActivityContext? = null
    private var shouldSavePractice = true
    private var hasProcessedCurrentSession: Boolean = false

    val durationLabel: StateFlow<String> =
        combine(
            repository.sessionState,
            flow {
                while (true) {
                    emit(Unit)
                    delay(AppConstant.MILLIS_PER_SECOND.toLong())
                }
            }
        ) { session, _ ->
            formatDuration(session)
        }
            .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(), initialValue = "")

    init {
        viewModelScope.launch {
            repository.sessionState.collect { session ->
                val isCallOpen = CallUiMapper.isCallOpen(session)
                if (CallUiMapper.shouldComplete(session) && !hasProcessedCurrentSession) {
                    hasProcessedCurrentSession = true
                    completePractice(session)
                }
                _uiState.update { CallUiMapper.applySession(state = it, session = session, isCallOpen = isCallOpen) }
            }
        }

        viewModelScope.launch {
            repository.sessionState
                .map { it.transcript }
                .distinctUntilChanged()
                .collect { transcript ->
                    val messages = transcript.map(::toMessageUiState)
                    _uiState.update { it.copy(messages = messages) }
                }
        }
    }

    override fun onCleared() {
        if (!_uiState.value.hasHandedOffCompletion)
            _uiState.value.completionAttemptId?.let(completionRepository::releaseCompletion)
        repository.close()
        super.onCleared()
    }

    fun setForeground(isForeground: Boolean) {
        repository.setForeground(isForeground)
        if (!isForeground) dismissEndConfirmation()
    }

    fun consumeStartReady(): Boolean {
        val state = _uiState.value
        if (!state.isStartReady ||
            state.presentation != CallPresentation.READY ||
            state.microphonePermission != null ||
            state.finishedSession != null ||
            state.isExiting)
            return false
        return _uiState.compareAndSet(state, state.copy(isStartReady = false))
    }

    fun startCall(context: CallActivityContext, shouldSavePractice: Boolean = true) {
        if (_uiState.value.isExiting ||
            (_uiState.value.presentation != CallPresentation.READY &&
                _uiState.value.presentation != CallPresentation.FAILED))
            return
        dictionaryManager.reset()
        if (!_uiState.value.hasHandedOffCompletion)
            _uiState.value.completionAttemptId?.let(completionRepository::releaseCompletion)
        currentContext = context
        this.shouldSavePractice = shouldSavePractice
        hasProcessedCurrentSession = false
        val activityType = context.activityType
        _uiState.update {
            it.copy(
                microphonePermission = null,
                presentation = CallUiMapper.getPresentation(repository.sessionState.value),
                isStartReady = false,
                dictionary = null,
                messages = emptyList(),
                isTranscriptVisible = false,
                hasCompletionError = false,
                activityType = activityType,
                activityIndex =
                    ((context as? CallActivityContext.Training)?.activityScenario?.activity
                            as? TrainingActivity.UnitActivity)
                        ?.unitIndex
                        ?.plus(1),
                isTranscriptAvailable = activityType.isTranscriptAvailable,
                activeDialog = null,
                isFinishedSessionReady = false,
                hasConsumedExit = false,
                hasHandedOffCompletion = false,
                finishedSession = null,
                completionAttemptId = null
            )
        }
        repository.startCall(context)
    }

    fun handoffCompletion(): Boolean {
        val state = _uiState.value
        if (state.hasHandedOffCompletion || state.completionAttemptId == null || state.isExiting) return false
        return _uiState.compareAndSet(state, state.copy(hasHandedOffCompletion = true))
    }

    fun consumeFinishedSession(
        hasCallResultConsumer: Boolean = false,
        hasPracticeResultConsumer: Boolean = false
    ): Boolean {
        val state = _uiState.value
        val session = state.finishedSession ?: return false
        if (!state.isFinishedSessionReady || state.isExiting) return false
        val shouldDeliver =
            hasCallResultConsumer && !hasPracticeResultConsumer ||
                session.outcome == CallOutcome.CANCELLED ||
                !hasCallResultConsumer && !hasPracticeResultConsumer && session.outcome == CallOutcome.COMPLETED
        return shouldDeliver && _uiState.compareAndSet(state, state.copy(isFinishedSessionReady = false))
    }

    fun consumeExit(): Boolean {
        val state = _uiState.value
        if (!state.isExitReady || state.hasConsumedExit) return false
        return _uiState.compareAndSet(state, state.copy(isExitReady = false, hasConsumedExit = true))
    }

    fun showTitle() {
        _uiState.update { it.copy(activeDialog = CallDialog.Title) }
    }

    fun hideTitle() {
        _uiState.update { if (it.activeDialog == CallDialog.Title) it.copy(activeDialog = null) else it }
    }

    fun retryCompletion() {
        val session = _uiState.value.finishedSession ?: return
        if (!_uiState.value.hasCompletionError) return
        _uiState.update { it.copy(hasCompletionError = false, presentation = CallPresentation.FINALIZING) }
        completePractice(session)
        if (_uiState.value.hasCompletionError) _uiState.update { it.copy(presentation = CallPresentation.FAILED) }
    }

    fun openTranscript() {
        _uiState.update { it.copy(isTranscriptVisible = it.isTranscriptAvailable) }
    }

    fun closeTranscript() {
        dictionaryManager.cancelLookup()
        _uiState.update { it.copy(isTranscriptVisible = false, dictionary = null) }
    }

    fun selectWord(messageId: String, selectionStart: Int, word: String) {
        val message = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        val session = repository.sessionState.value
        if (session.isFinalizing || session.state != CallState.ACTIVE && session.state != CallState.PAUSED) return
        if (!_uiState.value.isTranscriptAvailable ||
            word.isBlank() ||
            selectionStart !in message.text.indices ||
            !message.text.regionMatches(selectionStart, word, 0, word.length))
            return
        dictionaryManager.selectWord(
            scope = viewModelScope,
            message = message,
            selectionStart = selectionStart,
            word = word,
            updateDictionary = updateDictionary
        )
    }

    fun saveDictionaryWord() = dictionaryManager.saveWord(
        scope = viewModelScope,
        currentDictionary = currentDictionary,
        updateDictionary = updateDictionary
    )

    fun closeDictionary() = dictionaryManager.close(updateDictionary)

    fun retryLookup() {
        val currentDict = dictionaryManager.retryLookup(currentDictionary, updateDictionary) ?: return
        selectWord(
            messageId = currentDict.messageId,
            selectionStart = currentDict.selectionStart,
            word = currentDict.word
        )
    }

    fun playDictionaryWord() = dictionaryManager.playWord(currentDictionary, updateDictionary)

    fun showConfigurationError() {
        _uiState.update { it.copy(presentation = CallPresentation.FAILED, failure = CallFailure.CONFIGURATION) }
    }

    fun stopDictionaryWord() = dictionaryManager.stopWord()

    fun showMicrophonePermission(presentation: MicrophonePermissionPresentation) {
        _uiState.update { it.copy(microphonePermission = MicrophonePermissionUiState(presentation)) }
    }

    fun dismissMicrophonePermission() {
        _uiState.update { it.copy(microphonePermission = null) }
    }

    fun toggleMute() {
        repository.toggleMute()
    }

    fun requestEndCall() {
        val session = repository.sessionState.value
        if (session.isFinalizing) return

        when (session.state) {
            CallState.READY -> {
                repository.endCall()
                exitCall()
            }
            CallState.CALLING -> repository.endCall()
            CallState.ACTIVE,
            CallState.PAUSED -> {
                _uiState.update { it.copy(activeDialog = CallDialog.EndConfirmation) }
            }

            else -> Unit
        }
    }

    fun confirmEndCall() {
        dismissEndConfirmation()
        repository.endCall()
    }

    fun dismissEndConfirmation() {
        _uiState.update { if (it.activeDialog == CallDialog.EndConfirmation) it.copy(activeDialog = null) else it }
    }

    fun resumeCall() {
        repository.resumeCall()
    }

    fun exitCall() {
        if (_uiState.value.isExiting) return

        _uiState.update {
            it.copy(microphonePermission = null, isExiting = true, isExitReady = repository.sessionState.value.isClosed)
        }

        repository.close()
    }

    private fun completePractice(session: CallSessionState) {
        val context = currentContext ?: return
        if (!shouldSavePractice || context is CallActivityContext.Assessment) return
        val attemptId =
            try {
                completionRepository.completePractice(CompletedPractice(context, session))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        if (attemptId == null) {
            if (session.outcome == CallOutcome.COMPLETED) _uiState.update { it.copy(hasCompletionError = true) }
            return
        }
        _uiState.update { it.copy(completionAttemptId = attemptId, hasCompletionError = false) }
    }

    private fun toMessageUiState(message: TranscriptMessage): CallTranscriptMessageUiState =
        CallTranscriptMessageUiState(
            id = message.messageId,
            text = message.content,
            isUser = message.senderType == SenderType.USER,
            isPartial = !message.isFinal,
            isInterrupted = message.isInterrupted
        )

    private fun formatDuration(session: CallSessionState): String {
        val durationMillis = CallTimingRules.getDurationMillis(session, SystemClock.elapsedRealtime()) ?: return ""
        val seconds = durationMillis / AppConstant.MILLIS_PER_SECOND

        return seconds.toDurationLabel()
    }
}
