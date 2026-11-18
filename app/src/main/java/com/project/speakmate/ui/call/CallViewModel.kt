package com.project.speakmate.ui.call

import com.project.speakmate.utils.AppConstant
import kotlinx.coroutines.CancellationException

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CallInterruption
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.DictionaryLookupResult
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.PronunciationState
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TranscriptMessage
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.VocabularySaveResult
import com.project.speakmate.domain.repository.CallRepository
import com.project.speakmate.domain.repository.DictionaryRepository
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.SavedVocabularyRepository
import com.project.speakmate.domain.repository.SpeakingAssessmentRepository
import com.project.speakmate.ui.call.state.CallDictionaryStatus
import com.project.speakmate.ui.call.state.CallDictionaryUiState
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.call.state.CallTranscriptMessageUiState
import com.project.speakmate.ui.call.state.CallUiState
import com.project.speakmate.ui.call.state.CallVocabularySaveStatus
import com.project.speakmate.ui.call.state.MicrophonePermissionPresentation
import com.project.speakmate.ui.call.state.MicrophonePermissionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import java.util.Locale
import java.util.UUID
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
    private val savedVocabularyRepository: SavedVocabularyRepository,
    private val historyRepository: HistoryRepository,
    private val speakingAssessmentRepository: SpeakingAssessmentRepository
) : ViewModel() {

    companion object {
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

    private var currentScenario: Scenario? = null
    private var currentActivityType: CallActivityType = CallActivityType.FREE_PRACTICE
    private var currentCommunicativeGoal: String = ""
    private var activeStartedAt: Date? = null
    private var hasProcessedCurrentSession: Boolean = false
    private var dictionarySaveRequest = 0L

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

                if (session.state == CallState.ACTIVE && activeStartedAt == null) {
                    activeStartedAt = Date()
                }

                if ((session.outcome != null || session.state == CallState.ENDED || session.state == CallState.FAILED) &&
                    !hasProcessedCurrentSession && session.state != CallState.READY && session.state != CallState.CALLING
                ) {
                    hasProcessedCurrentSession = true
                    onCallEnded(isNetworkError = session.interruption == CallInterruption.NETWORK || session.failure != null)
                }

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
        repository.close()
        super.onCleared()
    }

    fun setForeground(isForeground: Boolean) {
        repository.setForeground(isForeground)
        if (!isForeground) dismissEndConfirmation()
    }

    fun startCall(
        scenario: Scenario,
        activityType: CallActivityType,
        communicativeGoal: String = ""
    ) {
        currentScenario = scenario
        currentActivityType = activityType
        currentCommunicativeGoal = communicativeGoal
        activeStartedAt = null
        hasProcessedCurrentSession = false

        _uiState.update {
            it.copy(
                microphonePermission = null,
                isTranscriptAvailable = activityType.isTranscriptAvailable,
                isStandaloneCall = activityType == CallActivityType.FREE_PRACTICE || activityType == CallActivityType.INITIAL_PLACEMENT,
                finishedSession = null
            )
        }
        repository.startCall(scenario, activityType, communicativeGoal)
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

            val result = savedVocabularyRepository.saveVocabulary(vocabItem)

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

    fun onCallEnded(isNetworkError: Boolean = false) {
        val session = repository.sessionState.value
        val finalTranscripts = extractFinalTranscripts(session.transcript)
        val hasUserSpeech = session.hasUserSpeech || finalTranscripts.any { it.senderType == SenderType.USER }
        val endedAt = Date()
        val startedAt = activeStartedAt ?: endedAt

        if (isNetworkError || session.failure != null) {
            return
        }

        if (!hasUserSpeech) {
            return
        }

        if (currentActivityType == CallActivityType.INITIAL_PLACEMENT || currentActivityType == CallActivityType.REASSESSMENT) {
            return
        }

        val practiceType = when (currentActivityType) {
            CallActivityType.FREE_PRACTICE -> PracticeType.FREE_PRACTICE
            CallActivityType.TRAINING_UNIT -> PracticeType.TRAINING_UNIT
            CallActivityType.GOAL_ASSESSMENT -> PracticeType.GOAL_ASSESSMENT
            else -> PracticeType.FREE_PRACTICE
        }

        processAssessmentAndSaveHistory(
            transcripts = finalTranscripts,
            startedAt = startedAt,
            endedAt = endedAt,
            scenarioId = currentScenario?.scenarioId ?: "",
            practiceType = practiceType
        )
    }

    fun extractFinalTranscripts(rawTranscripts: List<TranscriptMessage>): List<TranscriptMessage> {
        return rawTranscripts
            .filter { it.isFinal && it.content.isNotBlank() }
            .mapIndexed { index, raw ->
                TranscriptMessage(
                    messageId = raw.messageId.ifBlank { UUID.randomUUID().toString() },
                    senderType = raw.senderType,
                    content = raw.content.trim(),
                    orderIndex = index,
                    isFinal = true,
                    isInterrupted = raw.isInterrupted
                )
            }
    }

    fun processAssessmentAndSaveHistory(
        transcripts: List<TranscriptMessage>,
        startedAt: Date,
        endedAt: Date,
        scenarioId: String,
        practiceType: PracticeType
    ) {
        viewModelScope.launch {
            var finalResult: AssessmentResult? = null
            var criteriaResult: SpeakingCriteriaResult? = null
            var requiredActionResults: Map<String, Boolean> = emptyMap()
            var feedbackSummary: String? = null

            if (practiceType != PracticeType.FREE_PRACTICE) {
                try {
                    val outcome = speakingAssessmentRepository.evaluate(
                        AssessmentEvidence(
                            targetLevel = SpeakingLevel.A1,
                            communicativeGoal = currentCommunicativeGoal,
                            requiredActions = emptyList(),
                            callOutcome = CallOutcome.COMPLETED,
                            failure = null,
                            hasUserSpeech = true,
                            transcript = transcripts,
                            audioDurationMillis = endedAt.time - startedAt.time,
                            assessmentAudioId = repository.sessionState.value.assessmentAudioId
                        )
                    )
                    when (outcome) {
                        is AssessmentOutcome.Evaluated -> {
                            criteriaResult = outcome.criteria
                            requiredActionResults = outcome.requiredActionResults
                            finalResult = calculateSpeakingResult(criteriaResult, requiredActionResults)
                            feedbackSummary = "Hoàn tất đánh giá."
                        }
                        is AssessmentOutcome.InsufficientEvidence -> {
                            feedbackSummary = "Hệ thống chưa thể đánh giá được phiên này do thiếu dữ kiện hoặc lỗi AI."
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    feedbackSummary = "Hệ thống chưa thể đánh giá được phiên này do thiếu dữ kiện hoặc lỗi AI."
                }
            } else {
                feedbackSummary = "Luyện tập tự do không chấm điểm."
            }

            val attempt = PracticeAttempt(
                attemptId = UUID.randomUUID().toString(),
                scenarioId = scenarioId,
                practiceType = practiceType,
                startedAt = startedAt,
                endedAt = endedAt,
                result = finalResult,
                criteriaResult = criteriaResult,
                requiredActionResults = requiredActionResults,
                transcripts = transcripts,
                feedbackSummary = feedbackSummary
            )

            historyRepository.savePracticeAttempt(attempt)
        }
    }

    fun calculateSpeakingResult(
        criteria: SpeakingCriteriaResult,
        actions: Map<String, Boolean>
    ): AssessmentResult {
        if (actions.containsValue(false)) return AssessmentResult.FAIL
        return criteria.calculateSpeakingResult()
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
            .coerceAtLeast(0L) / AppConstant.MILLIS_PER_SECOND

        return String.format(
            Locale.ROOT,
            DURATION_FORMAT,
            seconds / AppConstant.SECONDS_PER_MINUTE,
            seconds % AppConstant.SECONDS_PER_MINUTE
        )
    }
}
