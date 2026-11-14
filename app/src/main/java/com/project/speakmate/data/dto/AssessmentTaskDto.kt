package com.project.speakmate.data.dto

import com.project.speakmate.domain.model.AssessmentTask
import com.project.speakmate.domain.model.SpeakingLevel

data class AssessmentTaskDto(
    val taskId: String = "",
    val scenarioId: String = "",
    val title: String = "",
    val targetLevel: String = "",
    val communicativeGoal: String = ""
) {
    fun withDocumentId(documentId: String): AssessmentTaskDto = copy(taskId = taskId.ifBlank { documentId })

    fun toDomain(): AssessmentTask? {
        if (taskId.isBlank() || scenarioId.isBlank()) return null
        val level = SpeakingLevel.entries.firstOrNull { it.name == targetLevel && it != SpeakingLevel.NOT_ASSESSED }
            ?: return null
        return AssessmentTask(taskId, scenarioId, title, level, communicativeGoal)
    }
}
