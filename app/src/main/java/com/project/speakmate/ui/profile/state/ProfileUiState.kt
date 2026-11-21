package com.project.speakmate.ui.profile.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.SpeakingLevel

@Immutable
data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val levelLabel: String = "",
    val assessmentDescription: String = "",
    val skills: List<ProfileSkillUiState> = emptyList(),
    val isProfileLoadFailed: Boolean = false,
    val level: SpeakingLevel? = null
)
