package com.project.speakmate.domain.model

data class VocabularyItem(
    val word: String,
    val mean: String,
    val phonetic: String,
    val partOfSpeech: String,
    val example: LearningExample
)
