package com.project.speakmate.ui.profile.state

import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.ui.components.assessment.AssessmentCriterionUiState

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val skills: List<AssessmentCriterionUiState> = emptyList(),
    val isProfileLoadFailed: Boolean = false,
    val level: SpeakingLevel? = null,
    val isLoading: Boolean = true,
    val hasLoaded: Boolean = false,
    val lastAssessedAtMillis: Long? = null,
    val savedVocabularyCount: Int? = null
)
