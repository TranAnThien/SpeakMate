package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.ScenarioPreparationMetadata

interface ScenarioRepository {
    suspend fun getScenario(scenarioId: String, requireServer: Boolean = false): Scenario?
    suspend fun getScenarioPreparationMetadata(scenarioId: String, requireServer: Boolean = false): ScenarioPreparationMetadata
    suspend fun getScenarioImageUrl(scenarioId: String): String
}
