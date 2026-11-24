package com.project.speakmate.data.repository

import com.project.speakmate.data.dto.PlacementCheckpointDto
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PlacementCheckpoint
import com.project.speakmate.domain.model.PlacementFeedback
import com.project.speakmate.domain.model.PlacementResult
import com.project.speakmate.domain.model.PlacementStep
import com.project.speakmate.domain.model.PlacementTaskRecord
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.rules.PlacementRules
import com.project.speakmate.domain.rules.PlacementTestFixtures
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacementCheckpointDtoTest {
    companion object {
        private const val EXPIRES_AT_MILLIS = "expiresAtMillis"
    }

    private val checkpoint = PlacementTestFixtures.getCheckpoint()

    @Test
    fun newCheckpointPreservesItsSavedExpiry() {
        val deadline = checkpoint.createdAtMillis + 1
        val restored = requireNotNull(roundTrip(checkpoint.copy(expiresAtMillis = deadline)))
        assertEquals(deadline, restored.expiresAtMillis)
        assertFalse(PlacementRules.isExpired(restored, nowMillis = deadline - 1))
        assertTrue(PlacementRules.isExpired(restored, nowMillis = deadline))
    }

    @Test
    fun legacyCheckpointWithoutExpiryUsesItsCreatedAtTtl() {
        val json = getJson()
        json.remove(EXPIRES_AT_MILLIS)
        val restored = requireNotNull(parse(json))
        val deadline = restored.createdAtMillis + PlacementRules.CHECKPOINT_TTL_MILLIS
        assertEquals(0L, restored.expiresAtMillis)
        assertFalse(PlacementRules.isExpired(restored, nowMillis = deadline - 1))
        assertTrue(PlacementRules.isExpired(restored, nowMillis = deadline))
    }

    @Test
    fun storedExpiryBeyondTtlIsPreservedButCannotExtendValidity() {
        val restored = requireNotNull(parse(getJson().put(EXPIRES_AT_MILLIS, Long.MAX_VALUE)))
        val deadline = restored.createdAtMillis + PlacementRules.CHECKPOINT_TTL_MILLIS
        assertEquals(Long.MAX_VALUE, restored.expiresAtMillis)
        assertTrue(PlacementRules.isExpired(restored, nowMillis = deadline))
    }

    @Test
    fun restoredCheckpointExpiresWhenTheClockMovesBackward() {
        val restored = requireNotNull(roundTrip(checkpoint))
        assertTrue(PlacementRules.isExpired(restored, nowMillis = restored.createdAtMillis - 1))
    }

    @Test
    fun oldJournalFlowFieldIsIgnoredWithoutChangingCheckpointProgress() {
        val original = getCompletedJson()
        val expected = requireNotNull(parse(original))
        original.put("operationFlowId", "legacy-flow")
        assertEquals(expected, parse(original))
        val restored = requireNotNull(parse(original))
        assertEquals(restored, roundTrip(restored))
        assertFalse(JSONObject(PlacementCheckpointDto.fromCheckpoint(restored).toJson()).has("operationFlowId"))
    }

    @Test
    fun roundTripPreservesEveryField() {
        val criteria = PlacementTestFixtures.getOutcome(AssessmentResult.PASS).criteria
        val original =
            checkpoint.copy(
                step = PlacementStep.B1_PROBE,
                currentTaskId = "task_B1_1",
                records =
                    listOf(
                        PlacementTaskRecord(
                            step = PlacementStep.INITIAL_A2,
                            taskId = "task_A2_1",
                            scenarioId = "scenario_A2_1",
                            result = AssessmentResult.PASS,
                            criteria = criteria
                        )
                    ),
                candidateLevel = SpeakingLevel.A2,
                usedScenarioIds = listOf("scenario_A2_1"),
                technicalRetryCount = 2
            )
        assertEquals(original, roundTrip(original))
    }

    @Test
    fun pendingResultIsPreserved() {
        val criteria = PlacementTestFixtures.getOutcome(AssessmentResult.FAIL).criteria
        val original =
            checkpoint.copy(
                currentTaskId = null,
                pendingResult =
                    PlacementResult(level = SpeakingLevel.A1, speakingProfile = criteria, assessedAtMillis = 2_000L)
            )
        assertEquals(original, roundTrip(original))
    }

    @Test
    fun pendingResultPreservesTheTaskOutcomeAndAllFeedback() {
        val criteria = PlacementTestFixtures.getOutcome(AssessmentResult.PARTIAL).criteria
        val result =
            PlacementResult(
                level = SpeakingLevel.A1,
                speakingProfile = criteria,
                assessedAtMillis = 2_000L,
                taskResult = AssessmentResult.PARTIAL,
                feedback =
                    PlacementFeedback(
                        range = "range",
                        accuracy = "accuracy",
                        fluency = "fluency",
                        interaction = "interaction",
                        coherence = "coherence"
                    )
            )
        val original = checkpoint.copy(currentTaskId = null, pendingResult = result)

        assertEquals(original, roundTrip(original))
    }

    @Test
    fun legacyPendingResultUsesTheLastConfirmationOutcome() {
        val json = getCompletedJson()
        val pending = json.getJSONObject("pendingResult")
        pending.remove("result")
        pending.remove("feedback")
        val record = json.getJSONArray("records").getJSONObject(0)

        val restored = requireNotNull(parse(json)).pendingResult

        assertEquals(AssessmentResult.valueOf(record.getString("result")), restored?.taskResult)
        assertEquals(PlacementFeedback(), restored?.feedback)
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
    fun everyMissingRequiredCheckpointFieldReturnsNull() {
        val original = getJson()
        original
            .keys()
            .asSequence()
            .filter { it != EXPIRES_AT_MILLIS }
            .toList()
            .forEach { key ->
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
    fun checkpointContainsNoTranscriptAudioOrUserQuotes() {
        val json = getCompletedJson()
        val allowedKeys =
            setOf(
                "version",
                "userId",
                "step",
                "currentTaskId",
                "records",
                "candidateLevel",
                "usedScenarioIds",
                "technicalRetryCount",
                "createdAtMillis",
                "expiresAtMillis",
                "pendingResult"
            )
        assertEquals(allowedKeys, json.keys().asSequence().toSet())
        val serialized = json.toString()
        listOf("transcript", "audio", "evaluation", "userQuote").forEach { assertFalse(serialized.contains(it)) }
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
        val record =
            PlacementTaskRecord(
                step = PlacementStep.A2_CONFIRMATION,
                taskId = "task_A2_2",
                scenarioId = "scenario_A2_2",
                result = AssessmentResult.PASS,
                criteria = criteria
            )
        val completed =
            checkpoint.copy(
                currentTaskId = null,
                records = listOf(record),
                pendingResult =
                    PlacementResult(level = SpeakingLevel.A2, speakingProfile = criteria, assessedAtMillis = 2_000L)
            )
        return JSONObject(PlacementCheckpointDto.fromCheckpoint(completed).toJson())
    }

    private fun parse(json: JSONObject): PlacementCheckpoint? =
        PlacementCheckpointDto(json.toString()).toPlacementCheckpoint()
}
