package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.FreePractice
import com.project.speakmate.domain.model.Scenario

interface PracticeRepository {
    suspend fun getScenarioCatalog(): Result<List<FreePractice>>
    suspend fun getScenario(scenarioId: String): Result<Scenario>
    suspend fun getScenarioPreparationMetadata(scenarioId: String): Result<Map<String, String>>
}
