package com.project.speakmate.data.remote.ai

import com.project.speakmate.domain.rules.AssessmentTestFixtures
import org.json.JSONArray
import org.json.JSONObject

object SpeakingEvaluationTestFixtures {
    fun getEvaluationJson(): JSONObject {
        val json = JSONObject()
        listOf("range", "accuracy", "fluency", "interaction", "coherence").forEach { key ->
            val judgement = JSONObject().put("isMet", true).put("userQuote", AssessmentTestFixtures.USER_QUOTE)
                .put("feedback", "Short feedback").put("audioStartMillis", JSONObject.NULL).put("audioEndMillis", JSONObject.NULL)
            if (key == "fluency") judgement.put("audioStartMillis", 0L).put("audioEndMillis", AssessmentTestFixtures.AUDIO_DURATION_MILLIS)
            json.put(key, judgement)
        }
        return json.put("requiredActions", JSONObject())
    }

    fun getInteractionResponse(json: JSONObject = getEvaluationJson()): String = JSONObject().put("status", "completed")
        .put("steps", JSONArray().put(JSONObject().put("type", "model_output").put("content",
            JSONArray().put(JSONObject().put("type", "text").put("text", json.toString()))))).toString()
}
