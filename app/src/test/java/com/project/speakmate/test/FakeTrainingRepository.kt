package com.project.speakmate.test

import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.repository.TrainingRepository

class FakeTrainingRepository(
    private val pathResult: Result<TrainingPath> = Result.success(TrainingTestFixtures.getPath()),
    private val scenarioResult: Result<Scenario> =
        Result.success(
            Scenario(
                scenarioId = "scenario-unit-1",
                name = "Scenario",
                description = "Bối cảnh: Scenario context",
                promptTemplate = "Prompt"
            )
        ),
    private val imageResult: Result<String> = Result.success("https://example.com/image.jpg")
) : TrainingRepository {
    override suspend fun getTrainingPath(level: SpeakingLevel, requireServer: Boolean): Result<TrainingPath> =
        pathResult

    override suspend fun getTrainingPathStructure(level: SpeakingLevel) =
        getTrainingPath(level = level, requireServer = true)

    override suspend fun getScenario(scenarioId: String, requireServer: Boolean): Result<Scenario> = scenarioResult

    override suspend fun getScenarioImageUrl(scenarioId: String): Result<String> = imageResult
}
