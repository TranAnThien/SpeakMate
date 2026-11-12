package com.project.speakmate.data.repository

import com.project.speakmate.data.dto.PlacementCheckpointDto
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PlacementCheckpoint
import com.project.speakmate.domain.model.PlacementResult
import com.project.speakmate.domain.model.PlacementStep
import com.project.speakmate.domain.model.PlacementTaskRecord
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.rules.PlacementTestFixtures
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PlacementCheckpointDtoTest {
    private val checkpoint = PlacementTestFixtures.getCheckpoint()

    @Test
    fun roundTripPreservesEveryField() {
        val criteria = PlacementTestFixtures.getOutcome(AssessmentResult.PASS).criteria
        val original = checkpoint.copy(
            step = PlacementStep.B1_PROBE,
            currentTaskId = "task_B1_1",
            records = listOf(PlacementTaskRecord(PlacementStep.INITIAL_A2, "task_A2_1", "scenario_A2_1", AssessmentResult.PASS, criteria)),
            candidateLevel = SpeakingLevel.A2,
            usedScenarioIds = listOf("scenario_A2_1"),
            technicalRetryCount = 2
        )
        assertEquals(original, roundTrip(original))
    }

    @Test
    fun pendingResultIsPreserved() {
        val criteria = PlacementTestFixtures.getOutcome(AssessmentResult.FAIL).criteria
        val original = checkpoint.copy(currentTaskId = null, pendingResult = PlacementResult(SpeakingLevel.A1, criteria, 2_000L))
        assertEquals(original, roundTrip(original))
    }

    @Test
    fun nullCurrentTaskAndPendingResultArePreserved() {
        assertEquals(checkpoint.copy(currentTaskId = null), roundTrip(checkpoint.copy(currentTaskId = null)))
    }

    @Test
    fun corruptJsonReturnsNull() {
        listOf("", "{", "[]", "null").forEach { assertNull(PlacementCheckpointDto(it).toPlacementCheckpoint()) }
    }

    @Test
    fun unknownStepReturnsNull() {
        assertNull(parse(getJson().put("step", "UNKNOWN")))
    }

    @Test
    fun unknownCandidateLevelReturnsNull() {
        assertNull(parse(getJson().put("candidateLevel", "C1")))
    }

    @Test
    fun unknownVersionReturnsNull() {
        assertNull(parse(getJson().put("version", 2)))
    }

    @Test
    fun missingVersionReturnsNull() {
        val json = getJson()
        json.remove("version")
        assertNull(parse(json))
    }

    @Test
    fun everyMissingCheckpointFieldReturnsNull() {
        val original = getJson()
        original.keys().asSequence().toList().forEach { key ->
            val json = getJson()
            json.remove(key)
            assertNull(key, parse(json))
        }
    }

    @Test
    fun unknownRecordResultReturnsNull() {
        val json = getCompletedJson()
        json.getJSONArray("records").getJSONObject(0).put("result", "INSUFFICIENT_EVIDENCE")
        assertNull(parse(json))
    }

    @Test
    fun unknownRecordStepReturnsNull() {
        val json = getCompletedJson()
        json.getJSONArray("records").getJSONObject(0).put("step", "UNKNOWN")
        assertNull(parse(json))
    }

    @Test
    fun missingCriterionNeverBecomesFalse() {
        val json = getCompletedJson()
        json.getJSONArray("records").getJSONObject(0).getJSONObject("criteria").remove("rangeMet")
        assertNull(parse(json))
    }

    @Test
    fun stringCriterionIsRejected() {
        val json = getCompletedJson()
        json.getJSONArray("records").getJSONObject(0).getJSONObject("criteria").put("rangeMet", "false")
        assertNull(parse(json))
    }

    @Test
    fun unknownPendingLevelReturnsNull() {
        val json = getCompletedJson()
        json.getJSONObject("pendingResult").put("level", "C1")
        assertNull(parse(json))
    }

    @Test
    fun notAssessedCannotBePendingResultLevel() {
        val json = getCompletedJson()
        json.getJSONObject("pendingResult").put("level", SpeakingLevel.NOT_ASSESSED.name)
        assertNull(parse(json))
    }

    @Test
    fun numericStringsAndFractionalTimesAreRejected() {
        assertNull(parse(getJson().put("createdAtMillis", "1000")))
        assertNull(parse(getJson().put("expiresAtMillis", 1.5)))
        assertNull(parse(getJson().put("version", "1")))
    }

    @Test
    fun invalidRetryCountIsRejected() {
        assertNull(parse(getJson().put("technicalRetryCount", -1)))
        assertNull(parse(getJson().put("technicalRetryCount", Long.MAX_VALUE)))
    }

    @Test
    fun checkpointContainsNoTranscriptAudioOrFeedback() {
        val json = getCompletedJson()
        val allowedKeys = setOf("version", "userId", "step", "currentTaskId", "records", "candidateLevel",
            "usedScenarioIds", "technicalRetryCount", "createdAtMillis", "expiresAtMillis", "pendingResult")
        assertEquals(allowedKeys, json.keys().asSequence().toSet())
        val serialized = json.toString()
        listOf("transcript", "audio", "feedback", "evaluation", "userQuote").forEach { assertFalse(serialized.contains(it)) }
    }

    @Test
    fun versionAndEnumNamesUseStoredContract() {
        val json = getJson()
        assertEquals(1, json.getInt("version"))
        assertEquals(PlacementStep.INITIAL_A2.name, json.getString("step"))
        assertEquals(SpeakingLevel.NOT_ASSESSED.name, json.getString("candidateLevel"))
    }

    private fun roundTrip(original: PlacementCheckpoint): PlacementCheckpoint? =
        PlacementCheckpointDto(PlacementCheckpointDto.fromCheckpoint(original).toJson()).toPlacementCheckpoint()

    private fun getJson(): JSONObject = JSONObject(PlacementCheckpointDto.fromCheckpoint(checkpoint).toJson())

    private fun getCompletedJson(): JSONObject {
        val criteria = PlacementTestFixtures.getOutcome(AssessmentResult.PASS).criteria
        val record = PlacementTaskRecord(PlacementStep.A2_CONFIRMATION, "task_A2_2", "scenario_A2_2", AssessmentResult.PASS, criteria)
        val completed = checkpoint.copy(currentTaskId = null, records = listOf(record),
            pendingResult = PlacementResult(SpeakingLevel.A2, criteria, 2_000L))
        return JSONObject(PlacementCheckpointDto.fromCheckpoint(completed).toJson())
    }

    private fun parse(json: JSONObject): PlacementCheckpoint? = PlacementCheckpointDto(json.toString()).toPlacementCheckpoint()
}
