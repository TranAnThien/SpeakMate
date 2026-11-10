package com.project.speakmate.domain.model

data class PlacementCheckpoint(
    val userId: String,
    val step: PlacementStep,
    val currentTaskId: String?,
    val records: List<PlacementTaskRecord>,
    val candidateLevel: SpeakingLevel,
    val usedScenarioIds: List<String>,
    val technicalRetryCount: Int,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val pendingResult: PlacementResult?
)
