package com.project.speakmate.data.dto

import com.project.speakmate.domain.model.*
import java.util.Date

data class UserDto(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val level: String = SpeakingLevel.NOT_ASSESSED.name,
    val latestAssessmentCriteria: SpeakingCriteriaResultDto? = null,
    val lastAssessedAt: Date? = null,
    val trainingProgress: TrainingProgressDto? = null,
    val savedVocabularies: List<VocabularyItemDto> = emptyList()
)

data class SpeakingCriteriaResultDto(
    val rangeMet: Boolean = false,
    val accuracyMet: Boolean = false,
    val fluencyMet: Boolean = false,
    val interactionMet: Boolean = false,
    val coherenceMet: Boolean = false
)

data class TrainingProgressDto(
    val currentGoalOrder: Int = 0,
    val currentUnitOrder: Int = 0,
    val stage: String = TrainingStage.UNIT.name
)

data class VocabularyItemDto(
    val word: String = "",
    val mean: String = "",
    val phonetic: String = "",
    val partOfSpeech: String = "",
    val example: LearningExampleDto = LearningExampleDto()
)

data class LearningExampleDto(
    val sentence: String = "",
    val translate: String = ""
)

fun User.toDto() = UserDto(
    userId = userId,
    displayName = displayName,
    email = email,
    photoUrl = photoUrl,
    level = level.name,
    latestAssessmentCriteria = latestAssessmentCriteria?.let {
        SpeakingCriteriaResultDto(it.rangeMet, it.accuracyMet, it.fluencyMet, it.interactionMet, it.coherenceMet)
    },
    lastAssessedAt = lastAssessedAt,
    trainingProgress = trainingProgress?.let {
        TrainingProgressDto(it.currentGoalOrder, it.currentUnitOrder, it.stage.name)
    },
    savedVocabularies = savedVocabularies.map {
        VocabularyItemDto(it.word, it.mean, it.phonetic, it.partOfSpeech,
            LearningExampleDto(it.example.sentence, it.example.translate))
    }
)

fun UserDto.toDomain() = User(
    userId = userId,
    displayName = displayName,
    email = email,
    photoUrl = photoUrl,
    level = runCatching { SpeakingLevel.valueOf(level) }.getOrDefault(SpeakingLevel.NOT_ASSESSED),
    latestAssessmentCriteria = latestAssessmentCriteria?.let {
        SpeakingCriteriaResult(it.rangeMet, it.accuracyMet, it.fluencyMet, it.interactionMet, it.coherenceMet)
    },
    lastAssessedAt = lastAssessedAt,
    trainingProgress = trainingProgress?.let {
        TrainingProgress(it.currentGoalOrder, it.currentUnitOrder,
            runCatching { TrainingStage.valueOf(it.stage) }.getOrDefault(TrainingStage.UNIT))
    },
    savedVocabularies = savedVocabularies.map {
        VocabularyItem(it.word, it.mean, it.phonetic, it.partOfSpeech,
            LearningExample(it.example.sentence, it.example.translate))
    }
)