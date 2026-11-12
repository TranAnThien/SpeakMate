package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.DictionaryLookupResult

interface DictionaryRepository {
    suspend fun lookUp(word: String, sentence: String): DictionaryLookupResult
}
