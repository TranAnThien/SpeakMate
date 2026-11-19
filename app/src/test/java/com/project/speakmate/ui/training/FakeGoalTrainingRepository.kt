package com.project.speakmate.ui.training

import kotlinx.coroutines.CompletableDeferred
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.repository.TrainingRepository

class FakeGoalTrainingRepository : TrainingRepository {
    var path = GoalAssessmentTestFixtures.path
    var failure: Exception? = null
    var pathGate: CompletableDeferred<Unit>? = null
    val requestedScenarioIds = mutableListOf<String>()

    override suspend fun getTrainingPath(level: SpeakingLevel): Result<TrainingPath> {
        pathGate?.await()
        val exception = failure
        return if (exception != null) Result.failure(exception) else Result.success(path)
    }

    override suspend fun getTrainingGoal(goalId: String) =
        Result.success(path.goals.first { it.goalId == goalId })

    override suspend fun getTrainingUnit(unitId: String) =
        Result.success(path.goals.flatMap { it.units }.first { it.unitId == unitId })

    override suspend fun getScenario(scenarioId: String): Result<Scenario> {
        requestedScenarioIds += scenarioId
        return Result.success(GoalAssessmentTestFixtures.scenario.copy(scenarioId = scenarioId))
    }

    override suspend fun getScenarioImageUrl(scenarioId: String) = Result.success("")
}
