package com.project.speakmate.data.dto

import com.project.speakmate.domain.model.CriterionJudgement
import com.project.speakmate.domain.model.RequiredActionJudgement
import com.project.speakmate.domain.model.SpeakingEvaluation
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class SpeakingEvaluationResponse(private val evaluationJson: String) {
    companion object {
        private const val RANGE = "range"
        private const val ACCURACY = "accuracy"
        private const val FLUENCY = "fluency"
        private const val INTERACTION = "interaction"
        private const val COHERENCE = "coherence"
        private const val REQUIRED_ACTIONS = "requiredActions"
        private const val IS_MET = "isMet"
        private const val USER_QUOTE = "userQuote"
        private const val FEEDBACK = "feedback"
        private const val AUDIO_START_MILLIS = "audioStartMillis"
        private const val AUDIO_END_MILLIS = "audioEndMillis"
        private const val IS_COMPLETED = "isCompleted"
        private const val TYPE = "type"
        private const val OBJECT = "object"
        private const val PROPERTIES = "properties"
        private const val REQUIRED = "required"
        private const val ADDITIONAL_PROPERTIES = "additionalProperties"
        private const val BOOLEAN = "boolean"
        private const val STRING = "string"
        private const val INTEGER = "integer"
        private const val NULL = "null"

        fun createSchema(requiredActionIds: List<String>): JSONObject {
            val criteriaKeys = listOf(RANGE, ACCURACY, FLUENCY, INTERACTION, COHERENCE)
            val properties = JSONObject()
            criteriaKeys.forEach { properties.put(it, createCriterionSchema()) }
            val actionProperties = JSONObject()
            requiredActionIds.forEach { id ->
                actionProperties.put(id, objectSchema(JSONObject()
                    .put(IS_COMPLETED, JSONObject().put(TYPE, BOOLEAN))
                    .put(USER_QUOTE, JSONObject().put(TYPE, STRING)), listOf(IS_COMPLETED, USER_QUOTE)))
            }
            properties.put(REQUIRED_ACTIONS, objectSchema(actionProperties, requiredActionIds))
            return objectSchema(properties, criteriaKeys + REQUIRED_ACTIONS)
        }

        private fun createCriterionSchema(): JSONObject = objectSchema(JSONObject()
            .put(IS_MET, JSONObject().put(TYPE, BOOLEAN))
            .put(USER_QUOTE, JSONObject().put(TYPE, STRING))
            .put(FEEDBACK, JSONObject().put(TYPE, STRING))
            .put(AUDIO_START_MILLIS, JSONObject().put(TYPE, JSONArray(listOf(INTEGER, NULL))))
            .put(AUDIO_END_MILLIS, JSONObject().put(TYPE, JSONArray(listOf(INTEGER, NULL)))),
            listOf(IS_MET, USER_QUOTE, FEEDBACK, AUDIO_START_MILLIS, AUDIO_END_MILLIS))

        private fun objectSchema(properties: JSONObject, required: List<String>): JSONObject = JSONObject()
            .put(TYPE, OBJECT).put(PROPERTIES, properties).put(REQUIRED, JSONArray(required)).put(ADDITIONAL_PROPERTIES, false)
    }

    fun toDomain(): SpeakingEvaluation {
        val json = JSONObject(evaluationJson)
        requireKeys(json, setOf(RANGE, ACCURACY, FLUENCY, INTERACTION, COHERENCE, REQUIRED_ACTIONS))
        val actions = json.getJSONObject(REQUIRED_ACTIONS)
        return SpeakingEvaluation(
            range = readCriterion(json.getJSONObject(RANGE)),
            accuracy = readCriterion(json.getJSONObject(ACCURACY)),
            fluency = readCriterion(json.getJSONObject(FLUENCY)),
            interaction = readCriterion(json.getJSONObject(INTERACTION)),
            coherence = readCriterion(json.getJSONObject(COHERENCE)),
            requiredActions = actions.keys().asSequence().associateWith { id ->
                val action = actions.getJSONObject(id)
                requireKeys(action, setOf(IS_COMPLETED, USER_QUOTE))
                RequiredActionJudgement(readBoolean(action, IS_COMPLETED), readString(action, USER_QUOTE))
            }
        )
    }

    private fun readCriterion(json: JSONObject): CriterionJudgement {
        requireKeys(json, setOf(IS_MET, USER_QUOTE, FEEDBACK, AUDIO_START_MILLIS, AUDIO_END_MILLIS))
        return CriterionJudgement(
            isMet = readBoolean(json, IS_MET),
            userQuote = readString(json, USER_QUOTE),
            feedback = readString(json, FEEDBACK),
            audioStartMillis = readNullableLong(json, AUDIO_START_MILLIS),
            audioEndMillis = readNullableLong(json, AUDIO_END_MILLIS)
        )
    }

    private fun requireKeys(json: JSONObject, expected: Set<String>) {
        if (json.keys().asSequence().toSet() != expected) throw JSONException(PROPERTIES)
    }

    private fun readBoolean(json: JSONObject, key: String): Boolean = json.get(key) as? Boolean ?: throw JSONException(key)

    private fun readString(json: JSONObject, key: String): String = json.get(key) as? String ?: throw JSONException(key)

    private fun readNullableLong(json: JSONObject, key: String): Long? = when (val value = json.get(key)) {
        JSONObject.NULL -> null
        is Long -> value
        is Int -> value.toLong()
        else -> throw JSONException(key)
    }
}
