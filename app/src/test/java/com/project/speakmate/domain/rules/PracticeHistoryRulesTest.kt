package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.RequiredAction
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeHistoryRulesTest {
    private val attempt = PracticeAttempt(attemptId = "attempt", scenarioId = "scenario", practiceType = PracticeType.FREE_PRACTICE,
        startedAt = Date(0), endedAt = Date(0), result = null, criteriaResult = null,
        requiredActionResults = emptyMap(), transcripts = emptyList(), feedbackSummary = null)

    @Test
    fun newHeadKeepsDisplacedLoadedRowsAndReplacesDuplicateMetadata() {
        val previous = listOf(attempt.copy(attemptId = "old-head"), attempt.copy(attemptId = "boundary"), attempt.copy(attemptId = "tail"))
        val updated = attempt.copy(attemptId = "old-head", feedbackSummary = "Confirmed feedback")
        val head = listOf(attempt.copy(attemptId = "new"), updated)
        val merged = PracticeHistoryRules.mergeAttempts(head, previous)
        assertEquals(listOf("new", "old-head", "boundary", "tail"), merged.map { it.attemptId })
        assertEquals(updated, merged[1])
        assertEquals(3, previous.size)
    }

    @Test
    fun olderPagePreservesServerOrderWhenDisplayedTimestampsMatch() {
        val current = listOf(attempt.copy(attemptId = "z"), attempt.copy(attemptId = "m"))
        val older = listOf(attempt.copy(attemptId = "m"), attempt.copy(attemptId = "k"), attempt.copy(attemptId = "a"))
        assertEquals(listOf("z", "m", "k", "a"), PracticeHistoryRules.mergeAttempts(current, older).map { it.attemptId })
    }

    @Test
    fun emptyPageKeepsTheLoadedSnapshot() {
        val current = listOf(attempt)
        assertEquals(current, PracticeHistoryRules.mergeAttempts(current, emptyList()))
        assertEquals(current, PracticeHistoryRules.mergeAttempts(emptyList(), current))
        assertTrue(PracticeHistoryRules.mergeAttempts(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun unitAndGoalDescriptionsResolveByScenarioAndActivityType() {
        val path = TrainingProgressTestFixtures.getPath(listOf(1))
        val goal = path.goals.single()
        val unit = goal.units.single().copy(scenarioId = "shared", requiredActions = listOf(RequiredAction("confirm", "Confirm the order")))
        val assessment = goal.goalAssessment.copy(scenarioId = "shared", requiredActions = listOf(RequiredAction("confirm", "Confirm payment")))
        val configured = path.copy(goals = listOf(goal.copy(units = listOf(unit), goalAssessment = assessment)))
        assertEquals(mapOf("confirm" to "Confirm the order"), PracticeHistoryRules.getActionDescriptions("shared", PracticeType.TRAINING_UNIT, listOf(configured)))
        assertEquals(mapOf("confirm" to "Confirm payment"), PracticeHistoryRules.getActionDescriptions("shared", PracticeType.GOAL_ASSESSMENT, listOf(configured)))
    }

    @Test
    fun missingOrAmbiguousContentDoesNotInventAnActionDescription() {
        val path = TrainingProgressTestFixtures.getPath(listOf(1))
        val goal = path.goals.single()
        val unit = goal.units.single().copy(requiredActions = listOf(RequiredAction("action", "First")))
        val first = path.copy(goals = listOf(goal.copy(units = listOf(unit))))
        val second = path.copy(goals = listOf(goal.copy(units = listOf(unit.copy(requiredActions = listOf(RequiredAction("action", "Different")))))))
        assertTrue(PracticeHistoryRules.getActionDescriptions(unit.scenarioId, PracticeType.TRAINING_UNIT, listOf(first, second)).isEmpty())
        assertTrue(PracticeHistoryRules.getActionDescriptions("missing", PracticeType.TRAINING_UNIT, listOf(first)).isEmpty())
        assertTrue(PracticeHistoryRules.getActionDescriptions(unit.scenarioId, PracticeType.FREE_PRACTICE, listOf(first)).isEmpty())
    }
}
