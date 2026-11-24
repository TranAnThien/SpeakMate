package com.project.speakmate.ui.call

import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.DictionaryLookupResult
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.PronunciationState
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.VocabularySaveResult
import com.project.speakmate.domain.repository.CallRepository
import com.project.speakmate.domain.repository.DictionaryRepository
import com.project.speakmate.domain.repository.SavedVocabularyRepository
import com.project.speakmate.ui.call.state.CallDictionaryStatus
import com.project.speakmate.ui.call.state.CallDictionaryUiState
import com.project.speakmate.ui.call.state.CallTranscriptMessageUiState
import com.project.speakmate.ui.call.state.CallVocabularySaveStatus
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CallDictionaryManager @Inject constructor(
    private val dictionaryRepository: DictionaryRepository,
    private val savedVocabularyRepository: SavedVocabularyRepository,
    private val repository: CallRepository
) {
    companion object {
        private val SENTENCE_DELIMITERS = charArrayOf('.', '!', '?')
    }

    private var dictionarySaveRequest = 0L
    private var dictionaryLookupRequest = 0L
    private var dictionaryLookupJob: Job? = null

    fun reset() {
        dictionaryLookupJob?.cancel()
        dictionaryLookupRequest++
        dictionarySaveRequest++
    }

    fun selectWord(
        scope: CoroutineScope,
        message: CallTranscriptMessageUiState,
        selectionStart: Int,
        word: String,
        updateDictionary: ((CallDictionaryUiState?) -> CallDictionaryUiState?) -> Unit
    ) {
        val messageId = message.id
        val request = ++dictionaryLookupRequest
        dictionaryLookupJob?.cancel()

        repository.stopPronunciation()

        val sentenceContext = getSentence(message.text, selectionStart)

        updateDictionary {
            CallDictionaryUiState(
                word = word,
                sentence = sentenceContext,
                messageId = messageId,
                selectionStart = selectionStart,
                status = CallDictionaryStatus.LOADING
            )
        }

        dictionaryLookupJob =
            scope.launch {
                val result =
                    try {
                        dictionaryRepository.lookUp(word, sentenceContext)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        DictionaryLookupResult.Error(failure)
                    }
                if (request != dictionaryLookupRequest) return@launch

                updateDictionary { state ->
                    val currentDict = state ?: return@updateDictionary state

                    if (currentDict.messageId != messageId || currentDict.selectionStart != selectionStart) {
                        return@updateDictionary state
                    }

                    val updatedDict =
                        when (result) {
                            is DictionaryLookupResult.Found ->
                                currentDict.copy(
                                    status = CallDictionaryStatus.SUCCESS,
                                    phonetic = result.item.phonetic,
                                    partOfSpeech = result.item.partOfSpeech,
                                    meaning = result.item.mean,
                                    exampleSentence = result.item.example.sentence,
                                    exampleTranslation = result.item.example.translate
                                )

                            is DictionaryLookupResult.NotFound ->
                                currentDict.copy(status = CallDictionaryStatus.NOT_FOUND)

                            is DictionaryLookupResult.Error -> currentDict.copy(status = CallDictionaryStatus.ERROR)
                        }

                    updatedDict
                }
            }
    }

    fun saveWord(
        scope: CoroutineScope,
        currentDictionary: () -> CallDictionaryUiState?,
        updateDictionary: ((CallDictionaryUiState?) -> CallDictionaryUiState?) -> Unit
    ) {
        val currentDict = currentDictionary() ?: return
        if (!currentDict.canSave || currentDict.saveStatus == CallVocabularySaveStatus.SAVING) return
        val saveRequest = ++dictionarySaveRequest

        updateDictionary { it?.copy(saveStatus = CallVocabularySaveStatus.SAVING) }

        scope.launch {
            val vocabItem =
                VocabularyItem(
                    word = currentDict.word,
                    mean = currentDict.meaning,
                    phonetic = currentDict.phonetic,
                    partOfSpeech = currentDict.partOfSpeech,
                    example =
                        LearningExample(
                            sentence = currentDict.exampleSentence,
                            translate = currentDict.exampleTranslation
                        )
                )

            val result =
                try {
                    savedVocabularyRepository.saveVocabulary(vocabItem)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Result.failure(failure)
                }

            updateDictionary { state ->
                val dict = state ?: return@updateDictionary state
                if (saveRequest != dictionarySaveRequest ||
                    dict.messageId != currentDict.messageId ||
                    dict.word != currentDict.word ||
                    dict.selectionStart != currentDict.selectionStart ||
                    dict.saveStatus != CallVocabularySaveStatus.SAVING) {
                    return@updateDictionary state
                }

                val updatedSaveStatus =
                    result.fold(
                        onSuccess = { saveStatusResult ->
                            when (saveStatusResult) {
                                VocabularySaveResult.SAVED -> CallVocabularySaveStatus.SAVED

                                VocabularySaveResult.MOVED_TO_TOP -> CallVocabularySaveStatus.MOVED_TO_TOP

                                VocabularySaveResult.SAVED_REPLACING_OLDEST ->
                                    CallVocabularySaveStatus.SAVED_REPLACING_OLDEST
                            }
                        },
                        onFailure = { CallVocabularySaveStatus.FAILED }
                    )

                dict.copy(saveStatus = updatedSaveStatus)
            }
        }
    }

    fun close(updateDictionary: ((CallDictionaryUiState?) -> CallDictionaryUiState?) -> Unit) {
        cancelLookup()
        updateDictionary { null }
    }

    fun retryLookup(
        currentDictionary: () -> CallDictionaryUiState?,
        updateDictionary: ((CallDictionaryUiState?) -> CallDictionaryUiState?) -> Unit
    ): CallDictionaryUiState? {
        updateDictionary { dictionary ->
            if (dictionary?.status != CallDictionaryStatus.ERROR) dictionary
            else dictionary.copy(status = CallDictionaryStatus.LOADING)
        }
        return currentDictionary()
    }

    fun playWord(
        currentDictionary: () -> CallDictionaryUiState?,
        updateDictionary: ((CallDictionaryUiState?) -> CallDictionaryUiState?) -> Unit
    ) {
        val dictionary = currentDictionary() ?: return
        if (dictionary.status != CallDictionaryStatus.SUCCESS || dictionary.isPronouncing) return
        val session = repository.sessionState.value
        if (session.isFinalizing || session.state != CallState.ACTIVE && session.state != CallState.PAUSED) return
        updateDictionary {
            it?.copy(
                isPronouncing = true,
                isPronunciationLoading = true,
                hasPronunciationError = false
            )
        }
        repository.playPronunciation(dictionary.word)
    }

    fun stopWord() {
        repository.stopPronunciation()
    }

    private fun getSentence(text: String, index: Int): String {
        val start =
            text.lastIndexOfAny(SENTENCE_DELIMITERS, (index - 1).coerceAtLeast(0)).let {
                if (it < 0 || it >= index) 0 else it + 1
            }

        val end = text.indexOfAny(SENTENCE_DELIMITERS, index).let { if (it < 0) text.length else it + 1 }

        return text.substring(start, end.coerceAtLeast(start)).trim()
    }

    fun cancelLookup() {
        dictionaryLookupRequest++
        dictionaryLookupJob?.cancel()
        repository.stopPronunciation()
    }
}
