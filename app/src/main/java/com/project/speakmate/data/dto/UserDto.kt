package com.project.speakmate.data.dto

import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.domain.model.InvalidUserProfileException
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.model.VocabularyItem
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

fun User.toDto() = UserDto(
    userId = userId,
    displayName = displayName,
    email = email,
    photoUrl = photoUrl,
    level = level.name,
    latestAssessmentCriteria = latestAssessmentCriteria?.toDto(),
    lastAssessedAt = lastAssessedAt,
    trainingProgress = trainingProgress?.toDto(),
    savedVocabularies = savedVocabularies.map(VocabularyItem::toDto)
)

fun UserDto.toDomain() = User(
    userId = userId,
    displayName = displayName,
    email = email,
    photoUrl = photoUrl,
    level = SpeakingLevel.entries.firstOrNull { it.name == level }
        ?: throw InvalidUserProfileException(FirestoreField.LEVEL),
    latestAssessmentCriteria = latestAssessmentCriteria?.toDomain(),
    lastAssessedAt = lastAssessedAt,
    trainingProgress = trainingProgress?.let { progress ->
        val stage = TrainingStage.entries.firstOrNull { it.name == progress.stage }
            ?: throw InvalidUserProfileException(FirestoreField.STAGE)
        TrainingProgress(progress.currentGoalOrder, progress.currentUnitOrder, stage)
    },
    savedVocabularies = savedVocabularies.map {
        VocabularyItem(it.word, it.mean, it.phonetic, it.partOfSpeech,
            LearningExample(it.example.sentence, it.example.translate))
    }
)
