package com.project.speakmate.data.repository

import com.project.speakmate.data.dto.AssessmentTaskDto
import com.project.speakmate.domain.model.AssessmentTask
import com.project.speakmate.domain.model.SpeakingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AssessmentTaskDtoTest {
    private val task = AssessmentTaskDto("assessment-a2", "cafe", "Order coffee", "A2", "Order a drink")

    @Test
    fun mapsAllConfiguredFieldsWithoutChangingStoredIds() {
        assertEquals(AssessmentTask("assessment-a2", "cafe", "Order coffee", SpeakingLevel.A2, "Order a drink"), task.toDomain())
    }

    @Test
    fun acceptsEveryAssessmentLevel() {
        listOf(SpeakingLevel.A1, SpeakingLevel.A2, SpeakingLevel.B1, SpeakingLevel.B2).forEach {
            assertEquals(it, checkNotNull(task.copy(targetLevel = it.name).toDomain()).targetLevel)
        }
    }

    @Test
    fun unknownLevelIsDiscarded() {
        assertNull(task.copy(targetLevel = "C1").toDomain())
    }

    @Test
    fun notAssessedLevelIsDiscarded() {
        assertNull(task.copy(targetLevel = SpeakingLevel.NOT_ASSESSED.name).toDomain())
    }

    @Test
    fun missingLevelIsDiscarded() {
        assertNull(task.copy(targetLevel = "").toDomain())
    }

    @Test
    fun emptyTaskIdIsDiscarded() {
        assertNull(task.copy(taskId = "").toDomain())
    }

    @Test
    fun emptyScenarioIdIsDiscarded() {
        assertNull(task.copy(scenarioId = "").toDomain())
    }

    @Test
    fun blankIdsAreDiscarded() {
        assertNull(task.copy(taskId = "  ").toDomain())
        assertNull(task.copy(scenarioId = "\t").toDomain())
    }

    @Test
    fun levelIsNotGuessedOrNormalized() {
        listOf("a2", " A2 ", "unknown").forEach { assertNull(task.copy(targetLevel = it).toDomain()) }
    }

    @Test
    fun firestoreDefaultsNeverCreateValidAssessment() {
        assertNotNull(AssessmentTaskDto())
        assertNull(AssessmentTaskDto().toDomain())
    }

    @Test
    fun missingTaskIdFieldFallsBackToDocumentId() {
        assertEquals("doc-a2", checkNotNull(task.copy(taskId = "").withDocumentId("doc-a2").toDomain()).taskId)
    }

    @Test
    fun storedTaskIdFieldWinsOverDocumentId() {
        assertEquals("assessment-a2", checkNotNull(task.withDocumentId("doc-a2").toDomain()).taskId)
    }
}
