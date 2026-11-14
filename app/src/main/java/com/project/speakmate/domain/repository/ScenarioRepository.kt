package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.Scenario

interface ScenarioRepository {
    suspend fun getScenario(scenarioId: String): Scenario?
}
