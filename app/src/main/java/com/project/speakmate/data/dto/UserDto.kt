package com.project.speakmate.data.dto

import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.domain.model.InvalidUserProfileException
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User
import java.util.Date

data class UserDto(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val level: String = SpeakingLevel.NOT_ASSESSED.name,
    val latestAssessmentCriteria: SpeakingCriteriaResultDto? = null,
    val lastAssessedAt: Date? = null,
    val trainingProgress: TrainingProgressDto? = null
)

fun User.toDto() =
    UserDto(
        userId = userId,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl,
        level = level.name,
        latestAssessmentCriteria = latestAssessmentCriteria?.toDto(),
        lastAssessedAt = lastAssessedAt,
        trainingProgress = trainingProgress?.toDto()
    )

fun UserDto.toDomain() =
    User(
        userId = userId,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl,
        level =
            SpeakingLevel.entries.firstOrNull { it.name == level }
                ?: throw InvalidUserProfileException(FirestoreField.LEVEL),
        latestAssessmentCriteria =
            latestAssessmentCriteria?.let { criteria ->
                criteria.toDomain() ?: throw InvalidUserProfileException(FirestoreField.LATEST_ASSESSMENT_CRITERIA)
            },
        lastAssessedAt = lastAssessedAt,
        trainingProgress =
            trainingProgress?.let { progress ->
                val currentGoalOrder =
                    progress.currentGoalOrder ?: throw InvalidUserProfileException(FirestoreField.TRAINING_PROGRESS)
                val currentUnitOrder =
                    progress.currentUnitOrder ?: throw InvalidUserProfileException(FirestoreField.TRAINING_PROGRESS)
                val stage = when (progress.stage) {
                    "IN_PROGRESS" -> TrainingStage.UNIT
                    else -> TrainingStage.entries.firstOrNull { it.name == progress.stage }
                        ?: throw InvalidUserProfileException(FirestoreField.STAGE)
                }
                TrainingProgress(
                    currentGoalOrder = currentGoalOrder,
                    currentUnitOrder = currentUnitOrder,
                    stage = stage
                )
            }
    )
