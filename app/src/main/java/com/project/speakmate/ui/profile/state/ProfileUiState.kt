package com.project.speakmate.ui.profile.state

import com.project.speakmate.domain.model.SpeakingLevel

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val levelLabel: String = "",
    val assessmentDescription: String = "",
    val skills: List<ProfileSkillUiState> = emptyList(),
    val isProfileLoadFailed: Boolean = false,
    val level: SpeakingLevel? = null,
    val isLoading: Boolean = true,
    val hasLoaded: Boolean = false,
    val lastAssessedAtMillis: Long? = null,
    val savedVocabularyCount: Int? = null
)
