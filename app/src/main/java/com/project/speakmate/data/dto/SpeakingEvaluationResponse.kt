package com.project.speakmate.data.dto

import com.project.speakmate.utils.extension.requireBoolean
import com.project.speakmate.utils.extension.requireLong
import com.project.speakmate.utils.extension.requireString
import com.project.speakmate.data.remote.ai.GeminiWireField
import com.project.speakmate.domain.model.CriterionJudgement
import com.project.speakmate.domain.model.RequiredActionJudgement
import com.project.speakmate.domain.model.SpeakingEvaluation
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class SpeakingEvaluationResponse(private val evaluationJson: String) {
    companion object {

        fun createSchema(requiredActionIds: List<String>): JSONObject {
            val criteriaKeys = listOf(GeminiWireField.RANGE, GeminiWireField.ACCURACY, GeminiWireField.FLUENCY, GeminiWireField.INTERACTION, GeminiWireField.COHERENCE)
            val properties = JSONObject()
            criteriaKeys.forEach { properties.put(it, createCriterionSchema()) }
            val actionProperties = JSONObject()
            requiredActionIds.forEach { id ->
                actionProperties.put(id, objectSchema(JSONObject()
                    .put(GeminiWireField.IS_COMPLETED, JSONObject().put(GeminiWireField.TYPE, GeminiWireField.BOOLEAN))
                    .put(GeminiWireField.USER_QUOTE, JSONObject().put(GeminiWireField.TYPE, GeminiWireField.STRING)), listOf(GeminiWireField.IS_COMPLETED, GeminiWireField.USER_QUOTE)))
            }
            properties.put(GeminiWireField.REQUIRED_ACTIONS, objectSchema(actionProperties, requiredActionIds))
            return objectSchema(properties, criteriaKeys + GeminiWireField.REQUIRED_ACTIONS)
        }

        private fun createCriterionSchema(): JSONObject = objectSchema(JSONObject()
            .put(GeminiWireField.IS_MET, JSONObject().put(GeminiWireField.TYPE, GeminiWireField.BOOLEAN))
            .put(GeminiWireField.USER_QUOTE, JSONObject().put(GeminiWireField.TYPE, GeminiWireField.STRING))
            .put(GeminiWireField.FEEDBACK, JSONObject().put(GeminiWireField.TYPE, GeminiWireField.STRING))
            .put(GeminiWireField.AUDIO_START_MILLIS, JSONObject().put(GeminiWireField.TYPE, JSONArray(listOf(GeminiWireField.INTEGER, GeminiWireField.NULL))))
            .put(GeminiWireField.AUDIO_END_MILLIS, JSONObject().put(GeminiWireField.TYPE, JSONArray(listOf(GeminiWireField.INTEGER, GeminiWireField.NULL)))),
            listOf(GeminiWireField.IS_MET, GeminiWireField.USER_QUOTE, GeminiWireField.FEEDBACK, GeminiWireField.AUDIO_START_MILLIS, GeminiWireField.AUDIO_END_MILLIS))

        private fun objectSchema(properties: JSONObject, required: List<String>): JSONObject = JSONObject()
            .put(GeminiWireField.TYPE, GeminiWireField.OBJECT).put(GeminiWireField.PROPERTIES, properties).put(GeminiWireField.REQUIRED, JSONArray(required)).put(GeminiWireField.ADDITIONAL_PROPERTIES, false)
    }

    fun toDomain(): SpeakingEvaluation {
        val json = JSONObject(evaluationJson)
        requireKeys(json, setOf(GeminiWireField.RANGE, GeminiWireField.ACCURACY, GeminiWireField.FLUENCY, GeminiWireField.INTERACTION, GeminiWireField.COHERENCE, GeminiWireField.REQUIRED_ACTIONS))
        val actions = json.getJSONObject(GeminiWireField.REQUIRED_ACTIONS)
        return SpeakingEvaluation(
            range = readCriterion(json.getJSONObject(GeminiWireField.RANGE)),
            accuracy = readCriterion(json.getJSONObject(GeminiWireField.ACCURACY)),
            fluency = readCriterion(json.getJSONObject(GeminiWireField.FLUENCY)),
            interaction = readCriterion(json.getJSONObject(GeminiWireField.INTERACTION)),
            coherence = readCriterion(json.getJSONObject(GeminiWireField.COHERENCE)),
            requiredActions = actions.keys().asSequence().associateWith { id ->
                val action = actions.getJSONObject(id)
                requireKeys(action, setOf(GeminiWireField.IS_COMPLETED, GeminiWireField.USER_QUOTE))
                RequiredActionJudgement(action.requireBoolean(GeminiWireField.IS_COMPLETED), action.requireString(GeminiWireField.USER_QUOTE))
            }
        )
    }

    private fun readCriterion(json: JSONObject): CriterionJudgement {
        requireKeys(json, setOf(GeminiWireField.IS_MET, GeminiWireField.USER_QUOTE, GeminiWireField.FEEDBACK, GeminiWireField.AUDIO_START_MILLIS, GeminiWireField.AUDIO_END_MILLIS))
        return CriterionJudgement(
            isMet = json.requireBoolean(GeminiWireField.IS_MET),
            userQuote = json.requireString(GeminiWireField.USER_QUOTE),
            feedback = json.requireString(GeminiWireField.FEEDBACK),
            audioStartMillis = readNullableLong(json, GeminiWireField.AUDIO_START_MILLIS),
            audioEndMillis = readNullableLong(json, GeminiWireField.AUDIO_END_MILLIS)
        )
    }

    private fun requireKeys(json: JSONObject, expected: Set<String>) {
        if (json.keys().asSequence().toSet() != expected) throw JSONException(GeminiWireField.PROPERTIES)
    }

    private fun readNullableLong(json: JSONObject, key: String): Long? =
        if (json.get(key) == JSONObject.NULL) null else json.requireLong(key)
}
