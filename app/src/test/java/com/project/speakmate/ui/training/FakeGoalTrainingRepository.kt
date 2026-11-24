package com.project.speakmate.ui.training

import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.repository.TrainingRepository
import kotlinx.coroutines.CompletableDeferred

class FakeGoalTrainingRepository : TrainingRepository {
    var path = GoalAssessmentTestFixtures.path
    var failure: Exception? = null
    var pathGate: CompletableDeferred<Unit>? = null
    val requestedScenarioIds = mutableListOf<String>()

    override suspend fun getTrainingPath(level: SpeakingLevel, requireServer: Boolean): Result<TrainingPath> {
        pathGate?.await()
        val exception = failure
        return if (exception != null) Result.failure(exception) else Result.success(path)
    }

    override suspend fun getTrainingPathStructure(level: SpeakingLevel) =
        getTrainingPath(level = level, requireServer = true)

    override suspend fun getScenario(scenarioId: String, requireServer: Boolean): Result<Scenario> {
        requestedScenarioIds += scenarioId
        return Result.success(GoalAssessmentTestFixtures.scenario.copy(scenarioId = scenarioId))
    }

    override suspend fun getScenarioImageUrl(scenarioId: String) = Result.success("")
}
