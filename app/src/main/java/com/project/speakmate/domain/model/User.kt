package com.project.speakmate.domain.model

import java.util.Date

data class User(
    val userId: String,
    val displayName: String,
    val email: String,
    val photoUrl: String,
    val level: SpeakingLevel = SpeakingLevel.NOT_ASSESSED,
    val latestAssessmentCriteria: SpeakingCriteriaResult? = null,
    val lastAssessedAt: Date? = null,
    val trainingProgress: TrainingProgress? = null,
    val savedVocabularies: List<VocabularyItem> = emptyList()
) {
    fun canStartReassessment(): Boolean {
        return level != SpeakingLevel.NOT_ASSESSED &&
                level != SpeakingLevel.B2 &&
                trainingProgress?.stage == TrainingStage.PATH_COMPLETED
    }
}