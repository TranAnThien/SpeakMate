package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingGoal
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.model.TrainingUnit

interface TrainingRepository {
    suspend fun getTrainingPath(level: SpeakingLevel, requireServer: Boolean = false): Result<TrainingPath>
    suspend fun getTrainingPathStructure(level: SpeakingLevel): Result<TrainingPath>
    suspend fun getTrainingGoal(goalId: String): Result<TrainingGoal>
    suspend fun getTrainingUnit(unitId: String): Result<TrainingUnit>
    suspend fun getScenario(scenarioId: String, requireServer: Boolean = false): Result<Scenario>
    suspend fun getScenarioImageUrl(scenarioId: String): Result<String>
}
