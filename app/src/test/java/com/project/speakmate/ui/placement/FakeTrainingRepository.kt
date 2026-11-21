package com.project.speakmate.ui.placement

import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingGoal
import com.project.speakmate.domain.model.TrainingUnit
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.domain.rules.TrainingProgressTestFixtures

class FakeTrainingRepository : TrainingRepository {
    override suspend fun getTrainingPath(level: SpeakingLevel) =
        Result.success(TrainingProgressTestFixtures.getPath().copy(targetLevel = level))

    override suspend fun getTrainingGoal(goalId: String): Result<TrainingGoal> = Result.failure(UnsupportedOperationException())

    override suspend fun getTrainingUnit(unitId: String): Result<TrainingUnit> = Result.failure(UnsupportedOperationException())

    override suspend fun getScenario(scenarioId: String): Result<Scenario> = Result.failure(UnsupportedOperationException())

    override suspend fun getScenarioImageUrl(scenarioId: String) = Result.success("")
}
