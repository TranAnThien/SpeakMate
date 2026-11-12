package com.project.speakmate.test

import com.project.speakmate.domain.model.FreePractice
import com.project.speakmate.domain.model.GoalAssessment
import com.project.speakmate.domain.model.RequiredAction
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingGoal
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.model.TrainingUnit
import com.project.speakmate.domain.model.UnitPreparation
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.domain.repository.TrainingRepository

internal fun q2Path(): TrainingPath = TrainingPath(
    pathId = "path-a1",
    targetLevel = SpeakingLevel.A1,
    title = "A1 Path",
    goals = listOf(
        TrainingGoal(
            goalId = "goal-1",
            title = "Goal 1",
            canDoDescription = "Can do one",
            goalAssessment = GoalAssessment("assessment-1", "Goal 1 assessment", emptyList()),
            units = listOf(q2Unit("unit-1"), q2Unit("unit-2"))
        ),
        TrainingGoal(
            goalId = "goal-2",
            title = "Goal 2",
            canDoDescription = "Can do two",
            goalAssessment = GoalAssessment("assessment-2", "Goal 2 assessment", emptyList()),
            units = listOf(q2Unit("unit-3"))
        )
    )
)

internal fun q2Unit(id: String): TrainingUnit = TrainingUnit(
    unitId = id,
    scenarioId = "scenario-$id",
    title = "Title $id",
    communicativeFocus = "Focus $id",
    preparation = UnitPreparation(emptyList(), emptyList(), emptyList()),
    requiredActions = listOf(RequiredAction("action-$id", "Action $id"))
)

internal class FakeTrainingRepository(
    private val pathResult: Result<TrainingPath> = Result.success(q2Path()),
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

internal class FakePracticeRepository(
    private val catalogResult: Result<List<FreePractice>> = Result.success(
        listOf(
            FreePractice("practice-1", "scenario-1", "Cafe"),
            FreePractice("practice-2", "scenario-2", "Travel")
        )
    ),
    private val scenarioResult: Result<Scenario> = Result.success(
        Scenario("scenario-1", "Order coffee", "Order a drink", "Prompt")
    ),
    private val metadataResult: Result<Map<String, String>> = Result.success(
        mapOf(
            "imageUrl" to "https://example.com/cafe.jpg",
            "uiContext" to "At a coffee shop",
            "aiRole" to "Barista",
            "preCallHint" to "Order a drink naturally"
        )
    )
) : PracticeRepository {
    override suspend fun getScenarioCatalog(): Result<List<FreePractice>> = catalogResult

    override suspend fun getScenario(scenarioId: String): Result<Scenario> =
        scenarioResult.map { it.copy(scenarioId = scenarioId) }

    override suspend fun getScenarioPreparationMetadata(scenarioId: String): Result<Map<String, String>> = metadataResult
}
