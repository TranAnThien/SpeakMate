package com.project.speakmate.ui.home.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class HomeUiState(
    val presentation: HomePresentation = HomePresentation.LOADING,
    val level: String = "",
    val nextLevel: String = "",
    val goalTitle: String = "",
    val unitTitle: String = "",
    val unitDescription: String = "",
    val displayName: String = "",
    val email: String = ""
) {
    val hasProfileSummary: Boolean get() = displayName.isNotBlank() || email.isNotBlank()
    val hasContinueAction: Boolean get() = presentation == HomePresentation.ACTIVE
}
