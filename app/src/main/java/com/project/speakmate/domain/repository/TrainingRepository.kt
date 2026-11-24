package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingPath

interface TrainingRepository {
    suspend fun getTrainingPath(level: SpeakingLevel, requireServer: Boolean = false): Result<TrainingPath>

    suspend fun getTrainingPathStructure(level: SpeakingLevel): Result<TrainingPath>

    suspend fun getScenario(scenarioId: String, requireServer: Boolean = false): Result<Scenario>

    suspend fun getScenarioImageUrl(scenarioId: String): Result<String>
}
