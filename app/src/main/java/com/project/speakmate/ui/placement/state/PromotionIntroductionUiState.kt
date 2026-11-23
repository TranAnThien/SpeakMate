package com.project.speakmate.ui.placement.state


data class PromotionIntroductionUiState(
    val title: String = "",
    val description: String = "",
    val steps: List<PromotionStepUiState> = emptyList(),
    val conditionTitle: String = "",
    val conditionDescription: String = "",
    val note: String = "",
    val currentLevelLabel: String = ""
)
