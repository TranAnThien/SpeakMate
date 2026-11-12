package com.project.speakmate.domain.model

data class TrainingUnit(
    val unitId: String,
    val scenarioId: String,
    val title: String,
    val communicativeFocus: String,
    val preparation: UnitPreparation,
    val requiredActions: List<RequiredAction>
)