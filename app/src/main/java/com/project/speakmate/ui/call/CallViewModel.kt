package com.project.speakmate.ui.call

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CallInterruption
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.PronunciationState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.TranscriptMessage
import com.project.speakmate.domain.repository.CallRepository
import com.project.speakmate.domain.repository.DictionaryRepository
import com.project.speakmate.domain.repository.SavedVocabularyRepository
import com.project.speakmate.domain.model.DictionaryLookupResult
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.VocabularySaveResult
import com.project.speakmate.ui.call.state.CallDictionaryStatus
import com.project.speakmate.ui.call.state.CallDictionaryUiState
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.call.state.CallTranscriptMessageUiState
import com.project.speakmate.ui.call.state.CallUiState
import com.project.speakmate.ui.call.state.MicrophonePermissionPresentation
import com.project.speakmate.ui.call.state.MicrophonePermissionUiState
import com.project.speakmate.ui.call.state.CallVocabularySaveStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
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
    private val savedVocabularyRepository: SavedVocabularyRepository
) : ViewModel() {

    companion object {
        private const val MILLIS_PER_SECOND = 1_000L
        private const val SECONDS_PER_MINUTE = 60L
        private const val DURATION_FORMAT = "%02d:%02d"
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

    val durationLabel: StateFlow<String> = combine(
        repository.sessionState,
        flow {
            while (true) {
                emit(Unit)
                delay(MILLIS_PER_SECOND)
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

                _uiState.update {
                    it.copy(
                        isTranscriptVisible = it.isTranscriptVisible && isCallOpen,
                        dictionary = it.dictionary.takeIf { isCallOpen }?.copy(
                            isPronouncing = session.pronunciation == PronunciationState.PLAYING,
                            hasPronunciationError = session.pronunciation == PronunciationState.FAILED
                        ),
                        presentation = getPresentation(session),
                        isMuted = session.isMuted,
                        isResuming = session.isResuming,
                        failure = session.failure,
                        isEndConfirmationVisible = it.isEndConfirmationVisible &&
                                (
                                        session.state == CallState.ACTIVE && !session.isFinalizing ||
                                                session.state == CallState.PAUSED &&
                                                (
                                                        it.presentation == CallPresentation.PAUSED ||
                                                                it.presentation == CallPresentation.RECONNECTING
                                                        )
                                        ),
                        isExitReady = it.isExiting && session.isClosed
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
        repository.close()
        super.onCleared()
    }

    fun setForeground(isForeground: Boolean) {
        repository.setForeground(isForeground)
        if (!isForeground) dismissEndConfirmation()
    }

    fun startCall(scenario: Scenario, activityType: CallActivityType) {
        _uiState.update {
            it.copy(
                microphonePermission = null,
                isTranscriptAvailable = activityType.isTranscriptAvailable
            )
        }
        repository.startCall(scenario)
    }

    fun openTranscript() {
        _uiState.update { it.copy(isTranscriptVisible = it.isTranscriptAvailable) }
    }

    fun closeTranscript() {
        repository.stopPronunciation()
        _uiState.update { it.copy(isTranscriptVisible = false, dictionary = null) }
    }

    fun selectWord(messageId: String, selectionStart: Int, word: String) {
        val message = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        if (word.isBlank() || selectionStart !in message.text.indices) return

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

        viewModelScope.launch {
            val result = dictionaryRepository.lookUp(word, sentenceContext)

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
        if (!currentDict.canSave) return

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

            val result = savedVocabularyRepository.saveVocabulary(vocabItem)

            _uiState.update { state ->
                val dict = state.dictionary ?: return@update state

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
        val word = _uiState.value.dictionary?.word ?: return
        repository.playPronunciation(word)
    }

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

    private fun getPresentation(session: CallSessionState): CallPresentation =
        when (session.state) {
            CallState.READY, CallState.ENDED -> CallPresentation.READY
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
                session.isFinalizing -> CallPresentation.PROCESSING
                session.interruption == CallInterruption.MICROPHONE ->
                    CallPresentation.MICROPHONE_BLOCKED

                session.isPlaying -> CallPresentation.SPEAKING
                session.isMuted -> CallPresentation.MUTED
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
        val startedAt = session.startedAtMillis ?: return ""
        val until = session.endedAtMillis
            ?: session.pausedAtMillis
            ?: SystemClock.elapsedRealtime()

        val seconds = (until - startedAt - session.totalPausedMillis)
            .coerceAtLeast(0L) / MILLIS_PER_SECOND

        return String.format(
            Locale.ROOT,
            DURATION_FORMAT,
            seconds / SECONDS_PER_MINUTE,
            seconds % SECONDS_PER_MINUTE
        )
    }
}