package com.project.speakmate.data.repository

import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.domain.model.ScenarioPreparationMetadata
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.utils.extension.defaultEmpty

object ScenarioPreparationMapper {
    private const val MISSING_AI_ROLE = "Scenario AI role is missing or invalid."
    fun toMetadata(fields: Map<String, Any?>): ScenarioPreparationMetadata {
        val level = (fields[FirestoreField.SUGGESTED_LEVEL] as? String)?.let { name -> SpeakingLevel.entries.firstOrNull { it.name == name && it != SpeakingLevel.NOT_ASSESSED } }
        val minutes = (fields[FirestoreField.ESTIMATED_DURATION_MINUTES] as? Number)?.toDouble()?.takeIf {
            it.isFinite() && it > 0 && it <= Int.MAX_VALUE && it % 1 == 0.0
        }?.toInt()
        return ScenarioPreparationMetadata(
            imageUrl = (fields[FirestoreField.IMAGE_URL] as? String).defaultEmpty().trim(),
            uiContext = (fields[FirestoreField.UI_CONTEXT] as? String).defaultEmpty().trim(),
            aiRole = (fields[FirestoreField.AI_ROLE] as? String)?.trim()?.takeIf(String::isNotBlank) ?: error(MISSING_AI_ROLE),
            preCallHint = (fields[FirestoreField.PRE_CALL_HINT] as? String).defaultEmpty().trim(),
            suggestedLevel = level,
            estimatedDurationMinutes = minutes
        )
    }
}
