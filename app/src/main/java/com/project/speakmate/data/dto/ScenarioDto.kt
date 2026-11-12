package com.project.speakmate.data.dto

import com.project.speakmate.domain.model.Scenario

data class ScenarioDto(
    val scenarioId: String = "",
    val name: String = "",
    val description: String = "",
    val promptTemplate: String = ""
) {
    fun withDocumentId(documentId: String): ScenarioDto = copy(scenarioId = scenarioId.ifBlank { documentId })

    fun toDomain(): Scenario? {
        if (scenarioId.isBlank() || name.isBlank() || description.isBlank() || promptTemplate.isBlank()) return null
        return Scenario(scenarioId, name, description, promptTemplate)
    }
}
