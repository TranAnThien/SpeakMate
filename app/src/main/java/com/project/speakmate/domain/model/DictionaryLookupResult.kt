package com.project.speakmate.domain.model

sealed interface DictionaryLookupResult {
    data class Found(val item: VocabularyItem) : DictionaryLookupResult
    data object NotFound : DictionaryLookupResult
    data class Error(val throwable: Throwable) : DictionaryLookupResult
}