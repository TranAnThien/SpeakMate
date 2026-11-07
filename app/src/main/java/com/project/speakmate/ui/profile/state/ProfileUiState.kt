package com.project.speakmate.ui.profile.state

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
    val skills: List<ProfileSkillUiState> = emptyList()
)
