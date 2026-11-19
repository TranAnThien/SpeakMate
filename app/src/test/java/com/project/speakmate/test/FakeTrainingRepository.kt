package com.project.speakmate.test

import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingGoal
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.model.TrainingUnit
import com.project.speakmate.domain.repository.TrainingRepository

class FakeTrainingRepository(
    private val pathResult: Result<TrainingPath> = Result.success(TrainingTestFixtures.getPath()),
    private val scenarioResult: Result<Scenario> = Result.success(
        Scenario("scenario-unit-1", "Scenario", "Bối cảnh: Scenario context", "Prompt")
    ),
    private val imageResult: Result<String> = Result.success("https://example.com/image.jpg")
) : TrainingRepository {
    override suspend fun getTrainingPath(level: SpeakingLevel): Result<TrainingPath> = pathResult

    override suspend fun getTrainingGoal(goalId: String): Result<TrainingGoal> =
        pathResult.mapCatching { path -> path.goals.first { it.goalId == goalId } }

    override suspend fun getTrainingUnit(unitId: String): Result<TrainingUnit> =
        pathResult.mapCatching { path -> path.goals.flatMap { it.units }.first { it.unitId == unitId } }

    override suspend fun getScenario(scenarioId: String): Result<Scenario> = scenarioResult

    override suspend fun getScenarioImageUrl(scenarioId: String): Result<String> = imageResult
}
