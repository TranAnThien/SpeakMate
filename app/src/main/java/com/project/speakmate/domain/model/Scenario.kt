package com.project.speakmate.domain.model

data class Scenario(
    val scenarioId: String,
    val name: String,
    val description: String,
    val promptTemplate: String
)
