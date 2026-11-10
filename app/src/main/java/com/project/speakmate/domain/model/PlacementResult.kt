package com.project.speakmate.domain.model

data class PlacementResult(
    val level: SpeakingLevel,
    val speakingProfile: SpeakingCriteriaResult,
    val assessedAtMillis: Long
)
