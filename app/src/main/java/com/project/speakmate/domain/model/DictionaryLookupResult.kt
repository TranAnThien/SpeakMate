package com.project.speakmate.domain.model

sealed interface DictionaryLookupResult {
    data class Found(val item: VocabularyItem) : DictionaryLookupResult
    data object NotFound : DictionaryLookupResult
}
