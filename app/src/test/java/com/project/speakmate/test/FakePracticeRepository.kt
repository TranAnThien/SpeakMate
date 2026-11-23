package com.project.speakmate.test

import com.project.speakmate.domain.model.FreePractice
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.ScenarioPreparationMetadata
import com.project.speakmate.domain.repository.PracticeRepository

class FakePracticeRepository(
    private val catalogResult: Result<List<FreePractice>> = Result.success(
        listOf(
            FreePractice("practice-1", "scenario-1", "Cafe"),
            FreePractice("practice-2", "scenario-2", "Travel")
        )
    ),
    private val scenarioResult: Result<Scenario> = Result.success(
        Scenario("scenario-1", "Order coffee", "Order a drink", "Prompt")
    ),
    private val metadataResult: Result<ScenarioPreparationMetadata> = Result.success(
        ScenarioPreparationMetadata(
            imageUrl = "https://example.com/cafe.jpg",
            uiContext = "At a coffee shop",
            aiRole = "Barista",
            preCallHint = "Order a drink naturally"
        )
    )
) : PracticeRepository {
    override suspend fun getScenarioCatalog(requireServer: Boolean): Result<List<FreePractice>> = catalogResult

    override suspend fun getScenario(scenarioId: String, requireServer: Boolean): Result<Scenario> =
        scenarioResult.map { it.copy(scenarioId = scenarioId) }

    override suspend fun getScenarioPreparationMetadata(scenarioId: String, requireServer: Boolean): Result<ScenarioPreparationMetadata> = metadataResult
}
