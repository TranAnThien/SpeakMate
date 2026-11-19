package com.project.speakmate.ui.profile.state

import com.project.speakmate.domain.model.SpeakingLevel
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
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
