package com.project.speakmate.data.dto

import com.project.speakmate.domain.model.SpeakingCriteriaResult

data class SpeakingCriteriaResultDto(
    val rangeMet: Boolean = false,
    val accuracyMet: Boolean = false,
    val fluencyMet: Boolean = false,
    val interactionMet: Boolean = false,
    val coherenceMet: Boolean = false
)

fun SpeakingCriteriaResult.toDto() = SpeakingCriteriaResultDto(rangeMet, accuracyMet, fluencyMet, interactionMet, coherenceMet)

fun SpeakingCriteriaResultDto.toDomain() = SpeakingCriteriaResult(rangeMet, accuracyMet, fluencyMet, interactionMet, coherenceMet)
