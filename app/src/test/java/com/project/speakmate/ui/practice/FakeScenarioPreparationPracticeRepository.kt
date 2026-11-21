package com.project.speakmate.ui.practice

import com.project.speakmate.domain.model.FreePractice
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.ScenarioPreparationMetadata
import com.project.speakmate.domain.repository.PracticeRepository

class FakeScenarioPreparationPracticeRepository : PracticeRepository {
    private val scenario = Scenario("FP-SC", "Coffee chat", "Order a drink", "Prompt")
    override suspend fun getScenarioCatalog(): Result<List<FreePractice>> =
        Result.success(listOf(FreePractice("fp-1", scenario.scenarioId, "Daily")))
    override suspend fun getScenario(scenarioId: String): Result<Scenario> = Result.success(scenario)
    override suspend fun getScenarioPreparationMetadata(scenarioId: String): Result<ScenarioPreparationMetadata> = Result.success(
        ScenarioPreparationMetadata(
            imageUrl = "https://example.com/coffee.jpg",
            uiContext = "You are in a coffee shop",
            aiRole = "Barista",
            preCallHint = "Ask about the menu"
        )
    )
}
