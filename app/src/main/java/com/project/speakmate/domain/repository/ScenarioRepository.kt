package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.ScenarioPreparationMetadata

interface ScenarioRepository {
    suspend fun getScenario(scenarioId: String): Scenario?
    suspend fun getScenarioPreparationMetadata(scenarioId: String): ScenarioPreparationMetadata
    suspend fun getScenarioImageUrl(scenarioId: String): String
}
