package com.project.speakmate.domain.model

enum class PlacementStep(val level: SpeakingLevel, val role: PlacementRole) {
    INITIAL_A2(SpeakingLevel.A2, PlacementRole.INITIAL),
    B1_PROBE(SpeakingLevel.B1, PlacementRole.PROBE),
    B2_PROBE(SpeakingLevel.B2, PlacementRole.PROBE),
    B2_CONFIRMATION(SpeakingLevel.B2, PlacementRole.CONFIRMATION),
    B1_CONFIRMATION(SpeakingLevel.B1, PlacementRole.CONFIRMATION),
    A2_CONFIRMATION(SpeakingLevel.A2, PlacementRole.CONFIRMATION),
    A1_CONFIRMATION(SpeakingLevel.A1, PlacementRole.CONFIRMATION)
}
