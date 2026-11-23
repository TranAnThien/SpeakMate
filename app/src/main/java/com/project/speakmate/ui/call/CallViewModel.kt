package com.project.speakmate.ui.call

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.CallInterruption
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.CompletedPractice
import com.project.speakmate.domain.model.DictionaryLookupResult
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.PronunciationState
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.TrainingActivity
import com.project.speakmate.domain.model.TranscriptMessage
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.VocabularySaveResult
import com.project.speakmate.domain.repository.CallRepository
import com.project.speakmate.domain.repository.DictionaryRepository
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.domain.repository.SavedVocabularyRepository
import com.project.speakmate.domain.rules.CallTimingRules
import com.project.speakmate.ui.call.state.CallDictionaryStatus
import com.project.speakmate.ui.call.state.CallDictionaryUiState
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.call.state.CallTranscriptMessageUiState
import com.project.speakmate.ui.call.state.CallUiState
import com.project.speakmate.ui.call.state.CallVocabularySaveStatus
import com.project.speakmate.ui.call.state.MicrophonePermissionPresentation
import com.project.speakmate.ui.call.state.MicrophonePermissionUiState
import com.project.speakmate.utils.AppConstant
import com.project.speakmate.utils.extension.toDurationLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
class CallViewModel @Inject constructor(
    private val repository: CallRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val savedVocabularyRepository: SavedVocabularyRepository,
    private val completionRepository: PracticeCompletionRepository
) : ViewModel() {

    companion object {
        private val SENTENCE_DELIMITERS = charArrayOf('.', '!', '?')
    }

    private val _uiState = MutableStateFlow(
        CallUiState(
            isTranscriptAvailable = false,
            isStandaloneCall = true
        )
    )

    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()
    val audioLevel: StateFlow<Float> = repository.audioLevel

    private var currentContext: CallActivityContext? = null
    private var hasHandedOffCompletion = false
    private var hasProcessedCurrentSession: Boolean = false
    private var dictionarySaveRequest = 0L
    private var dictionaryLookupRequest = 0L
    private var dictionaryLookupJob: Job? = null
    private var hasDeliveredFinishedSession = false
    private var hasDeliveredExit = false

    val durationLabel: StateFlow<String> = combine(
        repository.sessionState,
        flow {
            while (true) {
                emit(Unit)
                delay(AppConstant.MILLIS_PER_SECOND.toLong())
            }
        }
    ) { session, _ ->
        formatDuration(session)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    init {
        viewModelScope.launch {
            repository.sessionState.collect { session ->
                val isCallOpen = session.state == CallState.CALLING ||
                        session.state == CallState.ACTIVE ||
                        session.state == CallState.PAUSED

                if ((session.outcome != null || session.state == CallState.ENDED || session.state == CallState.FAILED) &&
                    !hasProcessedCurrentSession && session.state != CallState.READY && session.state != CallState.CALLING
                ) {
                    hasProcessedCurrentSession = true
                    completePractice(session)
                }

                _uiState.update {
                    it.copy(
                        isTranscriptVisible = it.isTranscriptVisible && isCallOpen,
                        dictionary = it.dictionary.takeIf { isCallOpen }?.let { dictionary ->
                            val isCurrent = dictionary.word == session.pronunciationText
                            dictionary.copy(
                                isPronouncing = isCurrent && (session.pronunciation == PronunciationState.PLAYING || session.pronunciation == PronunciationState.LOADING),
                                isPronunciationLoading = isCurrent && session.pronunciation == PronunciationState.LOADING,
                                hasPronunciationError = isCurrent && session.pronunciation == PronunciationState.FAILED
                            )
                        },
                        presentation = if (it.hasCompletionError) CallPresentation.FAILED else getPresentation(session),
                        hasUserSpeech = session.hasUserSpeech,
                        isMuted = session.isMuted,
                        isResuming = session.isResuming,
                        failure = session.failure,
                        isEndConfirmationVisible = it.isEndConfirmationVisible &&
                            (session.state == CallState.ACTIVE && !session.isFinalizing ||
                                session.state == CallState.PAUSED && (it.presentation == CallPresentation.PAUSED ||
                                    it.presentation == CallPresentation.RECONNECTING)),
                        isExitReady = it.isExiting && session.isClosed,
                        finishedSession = if (session.outcome != null) session else it.finishedSession
                    )
                }
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
        if (!hasHandedOffCompletion) _uiState.value.completionAttemptId?.let(completionRepository::releaseCompletion)
        repository.close()
        super.onCleared()
    }

    fun setForeground(isForeground: Boolean) {
        repository.setForeground(isForeground)
        if (!isForeground) dismissEndConfirmation()
    }

    fun consumeStartReady(): Boolean {
        val state = _uiState.value
        if (!state.isStartReady || state.presentation != CallPresentation.READY || state.microphonePermission != null ||
            state.finishedSession != null || state.isExiting) return false
        return _uiState.compareAndSet(state, state.copy(isStartReady = false))
    }

    fun startCall(context: CallActivityContext) {
        if (_uiState.value.isExiting || (_uiState.value.presentation != CallPresentation.READY && _uiState.value.presentation != CallPresentation.FAILED)) return
        dictionaryLookupJob?.cancel()
        dictionaryLookupRequest++
        dictionarySaveRequest++
        if (!hasHandedOffCompletion) _uiState.value.completionAttemptId?.let(completionRepository::releaseCompletion)
        currentContext = context
        hasHandedOffCompletion = false
        hasProcessedCurrentSession = false
        hasDeliveredFinishedSession = false
        hasDeliveredExit = false
        val activityType = context.activityType
        _uiState.update {
            it.copy(
                microphonePermission = null,
                presentation = CallPresentation.CONNECTING,
                isStartReady = false,
                dictionary = null,
                messages = emptyList(),
                isTranscriptVisible = false,
                hasCompletionError = false,
                activityType = activityType,
                activityIndex = ((context as? CallActivityContext.Training)?.activityScenario?.activity as? TrainingActivity.UnitActivity)?.unitIndex?.plus(1),
                isTranscriptAvailable = activityType.isTranscriptAvailable,
                isStandaloneCall = activityType == CallActivityType.FREE_PRACTICE || activityType == CallActivityType.INITIAL_PLACEMENT,
                finishedSession = null,
                completionAttemptId = null
            )
        }
        repository.startCall(context)
    }

    fun handoffCompletion(): Boolean {
        if (hasHandedOffCompletion || _uiState.value.completionAttemptId == null || _uiState.value.isExiting) return false
        hasHandedOffCompletion = true
        return true
    }

    fun consumeFinishedSession(): Boolean {
        if (hasDeliveredFinishedSession || _uiState.value.finishedSession == null || _uiState.value.isExiting) return false
        hasDeliveredFinishedSession = true
        return true
    }

    fun consumeExit(): Boolean {
        if (hasDeliveredExit) return false
        hasDeliveredExit = true
        return true
    }

    fun showTitle() { _uiState.update { it.copy(isTitleVisible = true) } }

    fun hideTitle() { _uiState.update { it.copy(isTitleVisible = false) } }

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
        dictionaryLookupRequest++
        dictionaryLookupJob?.cancel()
        repository.stopPronunciation()
        _uiState.update { it.copy(isTranscriptVisible = false, dictionary = null) }
    }

    fun selectWord(messageId: String, selectionStart: Int, word: String) {
        val message = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        val session = repository.sessionState.value
        if (session.isFinalizing || session.state != CallState.ACTIVE && session.state != CallState.PAUSED) return
        if (!_uiState.value.isTranscriptAvailable || word.isBlank() || selectionStart !in message.text.indices ||
            !message.text.regionMatches(selectionStart, word, 0, word.length)) return
        val request = ++dictionaryLookupRequest
        dictionaryLookupJob?.cancel()

        repository.stopPronunciation()

        val sentenceContext = getSentence(message.text, selectionStart)

        _uiState.update {
            it.copy(
                dictionary = CallDictionaryUiState(
                    word = word,
                    sentence = sentenceContext,
                    messageId = messageId,
                    selectionStart = selectionStart,
                    status = CallDictionaryStatus.LOADING
                )
            )
        }

        dictionaryLookupJob = viewModelScope.launch {
            val result = try { dictionaryRepository.lookUp(word, sentenceContext) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { DictionaryLookupResult.Error(failure) }
            if (request != dictionaryLookupRequest) return@launch

            _uiState.update { state ->
                val currentDict = state.dictionary ?: return@update state

                if (
                    currentDict.messageId != messageId ||
                    currentDict.selectionStart != selectionStart
                ) {
                    return@update state
                }

                val updatedDict = when (result) {
                    is DictionaryLookupResult.Found -> currentDict.copy(
                        status = CallDictionaryStatus.SUCCESS,
                        phonetic = result.item.phonetic,
                        partOfSpeech = result.item.partOfSpeech,
                        meaning = result.item.mean,
                        exampleSentence = result.item.example.sentence,
                        exampleTranslation = result.item.example.translate
                    )

                    is DictionaryLookupResult.NotFound -> currentDict.copy(
                        status = CallDictionaryStatus.NOT_FOUND
                    )

                    is DictionaryLookupResult.Error -> currentDict.copy(
                        status = CallDictionaryStatus.ERROR
                    )
                }

                state.copy(dictionary = updatedDict)
            }
        }
    }

    fun saveDictionaryWord() {
        val currentDict = _uiState.value.dictionary ?: return
        if (!currentDict.canSave || currentDict.saveStatus == CallVocabularySaveStatus.SAVING) return
        val saveRequest = ++dictionarySaveRequest

        _uiState.update {
            it.copy(
                dictionary = it.dictionary?.copy(
                    saveStatus = CallVocabularySaveStatus.SAVING
                )
            )
        }

        viewModelScope.launch {
            val vocabItem = VocabularyItem(
                word = currentDict.word,
                mean = currentDict.meaning,
                phonetic = currentDict.phonetic,
                partOfSpeech = currentDict.partOfSpeech,
                example = LearningExample(
                    sentence = currentDict.exampleSentence,
                    translate = currentDict.exampleTranslation
                )
            )

            val result = try { savedVocabularyRepository.saveVocabulary(vocabItem) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { Result.failure(failure) }

            _uiState.update { state ->
                val dict = state.dictionary ?: return@update state
                if (
                    saveRequest != dictionarySaveRequest ||
                    dict.messageId != currentDict.messageId ||
                    dict.word != currentDict.word ||
                    dict.selectionStart != currentDict.selectionStart ||
                    dict.saveStatus != CallVocabularySaveStatus.SAVING
                ) {
                    return@update state
                }

                val updatedSaveStatus = result.fold(
                    onSuccess = { saveStatusResult ->
                        when (saveStatusResult) {
                            VocabularySaveResult.SAVED ->
                                CallVocabularySaveStatus.SAVED

                            VocabularySaveResult.MOVED_TO_TOP ->
                                CallVocabularySaveStatus.MOVED_TO_TOP

                            VocabularySaveResult.SAVED_REPLACING_OLDEST ->
                                CallVocabularySaveStatus.SAVED_REPLACING_OLDEST
                        }
                    },
                    onFailure = { CallVocabularySaveStatus.FAILED }
                )

                state.copy(dictionary = dict.copy(saveStatus = updatedSaveStatus))
            }
        }
    }

    fun closeDictionary() {
        dictionaryLookupRequest++
        dictionaryLookupJob?.cancel()
        repository.stopPronunciation()
        _uiState.update { it.copy(dictionary = null) }
    }

    fun retryLookup() {
        _uiState.update { state ->
            val dictionary = state.dictionary
            if (dictionary?.status != CallDictionaryStatus.ERROR) {
                state
            } else {
                state.copy(
                    dictionary = dictionary.copy(status = CallDictionaryStatus.LOADING)
                )
            }
        }

        val currentDict = _uiState.value.dictionary ?: return
        selectWord(currentDict.messageId, currentDict.selectionStart, currentDict.word)
    }

    fun playDictionaryWord() {
        val dictionary = _uiState.value.dictionary ?: return
        if (dictionary.status != CallDictionaryStatus.SUCCESS || dictionary.isPronouncing) return
        val session = repository.sessionState.value
        if (session.isFinalizing || session.state != CallState.ACTIVE && session.state != CallState.PAUSED) return
        _uiState.update { it.copy(dictionary = it.dictionary?.copy(isPronouncing = true, isPronunciationLoading = true, hasPronunciationError = false)) }
        repository.playPronunciation(dictionary.word)
    }

    fun showConfigurationError() {
        _uiState.update { it.copy(presentation = CallPresentation.FAILED, failure = CallFailure.CONFIGURATION) }
    }

    fun stopDictionaryWord() { repository.stopPronunciation() }

    fun showMicrophonePermission(presentation: MicrophonePermissionPresentation) {
        _uiState.update {
            it.copy(microphonePermission = MicrophonePermissionUiState(presentation))
        }
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
            CallState.CALLING -> repository.endCall()
            CallState.ACTIVE, CallState.PAUSED -> {
                _uiState.update { it.copy(isEndConfirmationVisible = true) }
            }

            else -> Unit
        }
    }

    fun confirmEndCall() {
        dismissEndConfirmation()
        repository.endCall()
    }

    fun dismissEndConfirmation() {
        _uiState.update { it.copy(isEndConfirmationVisible = false) }
    }

    fun resumeCall() {
        repository.resumeCall()
    }

    fun exitCall() {
        if (_uiState.value.isExiting) return

        _uiState.update {
            it.copy(
                microphonePermission = null,
                isExiting = true,
                isExitReady = repository.sessionState.value.isClosed
            )
        }

        repository.close()
    }

    private fun completePractice(session: CallSessionState) {
        val context = currentContext ?: return
        if (context is CallActivityContext.Assessment) return
        val attemptId = try {
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

    private fun getPresentation(session: CallSessionState): CallPresentation =
        when (session.state) {
            CallState.READY -> CallPresentation.READY
            CallState.ENDED -> if (session.outcome == CallOutcome.COMPLETED) CallPresentation.FINALIZING else CallPresentation.READY
            CallState.CALLING -> CallPresentation.CONNECTING
            CallState.PAUSED -> {
                if (session.interruption == CallInterruption.NETWORK) {
                    CallPresentation.RECONNECTING
                } else {
                    CallPresentation.PAUSED
                }
            }

            CallState.FAILED -> CallPresentation.FAILED
            CallState.ACTIVE -> when {
                session.isFinalizing -> CallPresentation.FINALIZING
                session.interruption == CallInterruption.MICROPHONE ->
                    CallPresentation.MICROPHONE_BLOCKED

                session.isPlaying -> CallPresentation.SPEAKING
                session.isUserSpeaking && !session.isMuted -> CallPresentation.USER_SPEAKING
                session.isProcessing -> CallPresentation.PROCESSING
                session.isAiInterrupted -> CallPresentation.BARGE_IN
                else -> CallPresentation.LISTENING
            }
        }

    private fun toMessageUiState(message: TranscriptMessage): CallTranscriptMessageUiState =
        CallTranscriptMessageUiState(
            id = message.messageId,
            text = message.content,
            isUser = message.senderType == SenderType.USER,
            isPartial = !message.isFinal,
            isInterrupted = message.isInterrupted
        )

    private fun getSentence(text: String, index: Int): String {
        val start = text.lastIndexOfAny(
            SENTENCE_DELIMITERS,
            (index - 1).coerceAtLeast(0)
        ).let {
            if (it < 0 || it >= index) 0 else it + 1
        }

        val end = text.indexOfAny(SENTENCE_DELIMITERS, index).let {
            if (it < 0) text.length else it + 1
        }

        return text.substring(start, end.coerceAtLeast(start)).trim()
    }

    private fun formatDuration(session: CallSessionState): String {
        val durationMillis = CallTimingRules.getDurationMillis(session, SystemClock.elapsedRealtime()) ?: return ""
        val seconds = durationMillis / AppConstant.MILLIS_PER_SECOND

        return seconds.toDurationLabel()
    }
}
