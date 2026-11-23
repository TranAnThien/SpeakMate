package com.project.speakmate.ui.placement

import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.ScenarioPreparationMetadata
import com.project.speakmate.domain.repository.ScenarioRepository

class FakeScenarioRepository : ScenarioRepository {
    var failure: Exception? = null

    override suspend fun getScenario(scenarioId: String, requireServer: Boolean): Scenario {
        failure?.let { throw it }
        return Scenario(scenarioId, "Assessment", "Speak naturally", "Maintain the scenario role")
    }

    override suspend fun getScenarioPreparationMetadata(scenarioId: String, requireServer: Boolean) = ScenarioPreparationMetadata("", "", "", "")

    override suspend fun getScenarioImageUrl(scenarioId: String) = ""
}
