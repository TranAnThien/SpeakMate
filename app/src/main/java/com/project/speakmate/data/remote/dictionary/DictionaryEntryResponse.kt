package com.project.speakmate.data.remote.dictionary

data class DictionaryEntryResponse(
    val phonetic: String,
    val partOfSpeech: String,
    val exampleSentence: String
)
