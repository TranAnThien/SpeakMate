package com.project.speakmate.ui.call.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.rules.SavedVocabularyRules

@Immutable
data class CallDictionaryUiState(
    val word: String = "",
    val sentence: String = "",
    val status: CallDictionaryStatus = CallDictionaryStatus.LOADING,
    val phonetic: String = "",
    val partOfSpeech: String = "",
    val meaning: String = "",
    val exampleSentence: String = "",
    val exampleTranslation: String = "",
    val messageId: String = "",
    val selectionStart: Int = 0,
    val saveStatus: CallVocabularySaveStatus = CallVocabularySaveStatus.IDLE,
    val isPronouncing: Boolean = false,
    val hasPronunciationError: Boolean = false,
    val isPronunciationLoading: Boolean = false
) {
    val canSave: Boolean
        get() = status == CallDictionaryStatus.SUCCESS && SavedVocabularyRules.hasValidFields(
            VocabularyItem(word, meaning, phonetic, partOfSpeech, LearningExample(exampleSentence, exampleTranslation))
        )
}
