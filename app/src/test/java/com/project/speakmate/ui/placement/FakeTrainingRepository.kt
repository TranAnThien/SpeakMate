package com.project.speakmate.ui.placement

import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.domain.rules.TrainingProgressTestFixtures

class FakeTrainingRepository : TrainingRepository {
    override suspend fun getTrainingPath(level: SpeakingLevel, requireServer: Boolean) =
        Result.success(TrainingProgressTestFixtures.getPath().copy(targetLevel = level))

    override suspend fun getTrainingPathStructure(level: SpeakingLevel) =
        getTrainingPath(level = level, requireServer = true)

    override suspend fun getScenario(scenarioId: String, requireServer: Boolean): Result<Scenario> =
        Result.failure(UnsupportedOperationException())

    override suspend fun getScenarioImageUrl(scenarioId: String) = Result.success("")
}
