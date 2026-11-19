package com.project.speakmate.ui.home.state

import androidx.compose.runtime.Immutable

@Immutable
data class HomeUiState(
    val presentation: HomePresentation = HomePresentation.LOADING,
    val level: String = "",
    val nextLevel: String = "",
    val goalTitle: String = "",
    val unitTitle: String = "",
    val unitDescription: String = "",
    val isGoalAssessment: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String? = null
) {
    val hasProfileSummary: Boolean get() = displayName.isNotBlank() || email.isNotBlank()
    val hasContinueAction: Boolean get() = presentation == HomePresentation.ACTIVE
}
