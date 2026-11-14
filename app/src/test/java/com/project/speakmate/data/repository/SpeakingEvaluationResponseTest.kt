package com.project.speakmate.data.repository

import com.project.speakmate.data.dto.SpeakingEvaluationResponse
import com.project.speakmate.data.remote.ai.SpeakingEvaluationTestFixtures
import com.project.speakmate.domain.rules.AssessmentTestFixtures
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SpeakingEvaluationResponseTest {
    @Test
    fun completeJsonMapsToEvaluation() {
        val evaluation = SpeakingEvaluationResponse(SpeakingEvaluationTestFixtures.getEvaluationJson().toString()).toDomain()
        assertEquals(AssessmentTestFixtures.getEvaluation().range.isMet, evaluation.range.isMet)
        assertEquals(AssessmentTestFixtures.USER_QUOTE, evaluation.range.userQuote)
        assertEquals(AssessmentTestFixtures.AUDIO_DURATION_MILLIS, evaluation.fluency.audioEndMillis)
        assertEquals(emptyMap<String, Any>(), evaluation.requiredActions)
    }

    @Test
    fun everyMissingCriterionOrActionsFieldIsRejected() {
        val keys = SpeakingEvaluationTestFixtures.getEvaluationJson().keys().asSequence().toList()
        keys.forEach { key ->
            val json = SpeakingEvaluationTestFixtures.getEvaluationJson()
            json.remove(key)
            assertRejected(json)
        }
    }

    @Test
    fun everyMissingJudgementFieldIsRejected() {
        val keys = SpeakingEvaluationTestFixtures.getEvaluationJson().getJSONObject("range").keys().asSequence().toList()
        keys.forEach { key ->
            val json = SpeakingEvaluationTestFixtures.getEvaluationJson()
            json.getJSONObject("range").remove(key)
            assertRejected(json)
        }
    }

    @Test
    fun stringAndNullBooleanAreRejected() {
        listOf("false", 0, JSONObject.NULL).forEach {
            val json = SpeakingEvaluationTestFixtures.getEvaluationJson()
            json.getJSONObject("range").put("isMet", it)
            assertRejected(json)
        }
    }

    @Test
    fun numericQuoteAndNullFeedbackAreRejected() {
        val quote = SpeakingEvaluationTestFixtures.getEvaluationJson()
        quote.getJSONObject("range").put("userQuote", 1)
        assertRejected(quote)
        val feedback = SpeakingEvaluationTestFixtures.getEvaluationJson()
        feedback.getJSONObject("range").put("feedback", JSONObject.NULL)
        assertRejected(feedback)
    }

    @Test
    fun fractionalAndStringAudioIntervalsAreRejected() {
        listOf(1.5, "1").forEach {
            val json = SpeakingEvaluationTestFixtures.getEvaluationJson()
            json.getJSONObject("fluency").put("audioStartMillis", it)
            assertRejected(json)
        }
    }

    @Test
    fun requiredActionJudgementIsParsedStrictly() {
        val json = SpeakingEvaluationTestFixtures.getEvaluationJson()
        val action = JSONObject().put("isCompleted", true).put("userQuote", AssessmentTestFixtures.USER_QUOTE)
        json.getJSONObject("requiredActions").put(AssessmentTestFixtures.REQUIRED_ACTION_ID, action)
        val parsed = SpeakingEvaluationResponse(json.toString()).toDomain()
        assertEquals(true, parsed.requiredActions[AssessmentTestFixtures.REQUIRED_ACTION_ID]?.isCompleted)
        action.put("isCompleted", "true")
        assertRejected(json)
        action.put("isCompleted", false).remove("userQuote")
        assertRejected(json)
    }

    @Test
    fun unexpectedRootAndCriterionFieldsAreRejected() {
        assertRejected(SpeakingEvaluationTestFixtures.getEvaluationJson().put("result", "PASS"))
        val json = SpeakingEvaluationTestFixtures.getEvaluationJson()
        json.getJSONObject("range").put("extra", true)
        assertRejected(json)
    }

    @Test
    fun corruptResponseIsRejected() {
        listOf("", "{", "[]").forEach {
            assertThrows(JSONException::class.java) { SpeakingEvaluationResponse(it).toDomain() }
        }
    }

    @Test
    fun schemaRequiresAllFieldsAndOnlyConfiguredActions() {
        val schema = SpeakingEvaluationResponse.createSchema(listOf(AssessmentTestFixtures.REQUIRED_ACTION_ID))
        assertEquals(false, schema.getBoolean("additionalProperties"))
        assertEquals(6, schema.getJSONArray("required").length())
        val range = schema.getJSONObject("properties").getJSONObject("range")
        assertEquals(5, range.getJSONArray("required").length())
        val actions = schema.getJSONObject("properties").getJSONObject("requiredActions")
        assertEquals(listOf(AssessmentTestFixtures.REQUIRED_ACTION_ID), actions.getJSONObject("properties").keys().asSequence().toList())
    }

    private fun assertRejected(json: JSONObject) {
        assertThrows(JSONException::class.java) { SpeakingEvaluationResponse(json.toString()).toDomain() }
    }
}
