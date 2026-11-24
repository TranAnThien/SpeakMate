package com.project.speakmate.ui.placement.state

import com.project.speakmate.domain.model.SpeakingLevel

data class PromotionIntroductionUiState(
    val currentLevel: SpeakingLevel = SpeakingLevel.A1,
    val nextLevel: SpeakingLevel = SpeakingLevel.A2
)
