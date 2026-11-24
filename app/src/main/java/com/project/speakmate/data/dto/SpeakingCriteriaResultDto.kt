package com.project.speakmate.data.dto

import com.project.speakmate.domain.model.SpeakingCriteriaResult

data class SpeakingCriteriaResultDto(
    val rangeMet: Boolean? = null,
    val accuracyMet: Boolean? = null,
    val fluencyMet: Boolean? = null,
    val interactionMet: Boolean? = null,
    val coherenceMet: Boolean? = null
)

fun SpeakingCriteriaResult.toDto() =
    SpeakingCriteriaResultDto(
        rangeMet = rangeMet,
        accuracyMet = accuracyMet,
        fluencyMet = fluencyMet,
        interactionMet = interactionMet,
        coherenceMet = coherenceMet
    )

fun SpeakingCriteriaResultDto.toDomain(): SpeakingCriteriaResult? {
    val range = rangeMet ?: return null
    val accuracy = accuracyMet ?: return null
    val fluency = fluencyMet ?: return null
    val interaction = interactionMet ?: return null
    val coherence = coherenceMet ?: return null
    return SpeakingCriteriaResult(
        rangeMet = range,
        accuracyMet = accuracy,
        fluencyMet = fluency,
        interactionMet = interaction,
        coherenceMet = coherence
    )
}
