package com.project.speakmate.data.dto

import com.project.speakmate.domain.model.VocabularyItem

data class VocabularyItemDto(
    val word: String = "",
    val mean: String = "",
    val phonetic: String = "",
    val partOfSpeech: String = "",
    val example: LearningExampleDto = LearningExampleDto()
)

fun VocabularyItem.toDto() = VocabularyItemDto(word, mean, phonetic, partOfSpeech, LearningExampleDto(example.sentence, example.translate))
