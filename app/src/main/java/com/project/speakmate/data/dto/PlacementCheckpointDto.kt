package com.project.speakmate.data.dto

import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PlacementCheckpoint
import com.project.speakmate.domain.model.PlacementResult
import com.project.speakmate.domain.model.PlacementStep
import com.project.speakmate.domain.model.PlacementTaskRecord
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.utils.extension.default
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class PlacementCheckpointDto(private val checkpointJson: String) {
    companion object {
        private const val VERSION = 1
        private const val VERSION_KEY = "version"
        private const val USER_ID = "userId"
        private const val STEP = "step"
        private const val CURRENT_TASK_ID = "currentTaskId"
        private const val RECORDS = "records"
        private const val CANDIDATE_LEVEL = "candidateLevel"
        private const val USED_SCENARIO_IDS = "usedScenarioIds"
        private const val TECHNICAL_RETRY_COUNT = "technicalRetryCount"
        private const val CREATED_AT_MILLIS = "createdAtMillis"
        private const val EXPIRES_AT_MILLIS = "expiresAtMillis"
        private const val PENDING_RESULT = "pendingResult"
        private const val TASK_ID = "taskId"
        private const val SCENARIO_ID = "scenarioId"
        private const val RESULT = "result"
        private const val CRITERIA = "criteria"
        private const val LEVEL = "level"
        private const val SPEAKING_PROFILE = "speakingProfile"
        private const val ASSESSED_AT_MILLIS = "assessedAtMillis"
        private const val RANGE_MET = "rangeMet"
        private const val ACCURACY_MET = "accuracyMet"
        private const val FLUENCY_MET = "fluencyMet"
        private const val INTERACTION_MET = "interactionMet"
        private const val COHERENCE_MET = "coherenceMet"

        fun fromCheckpoint(checkpoint: PlacementCheckpoint): PlacementCheckpointDto {
            val json = JSONObject()
                .put(VERSION_KEY, VERSION)
                .put(USER_ID, checkpoint.userId)
                .put(STEP, checkpoint.step.name)
                .put(CURRENT_TASK_ID, checkpoint.currentTaskId.default<Any>(JSONObject.NULL))
                .put(RECORDS, JSONArray().apply { checkpoint.records.forEach { put(toRecordJson(it)) } })
                .put(CANDIDATE_LEVEL, checkpoint.candidateLevel.name)
                .put(USED_SCENARIO_IDS, JSONArray(checkpoint.usedScenarioIds))
                .put(TECHNICAL_RETRY_COUNT, checkpoint.technicalRetryCount)
                .put(CREATED_AT_MILLIS, checkpoint.createdAtMillis)
                .put(EXPIRES_AT_MILLIS, checkpoint.expiresAtMillis)
                .put(PENDING_RESULT, checkpoint.pendingResult?.let(::toResultJson).default<Any>(JSONObject.NULL))
            return PlacementCheckpointDto(json.toString())
        }

        private fun toRecordJson(record: PlacementTaskRecord): JSONObject = JSONObject()
            .put(STEP, record.step.name)
            .put(TASK_ID, record.taskId)
            .put(SCENARIO_ID, record.scenarioId)
            .put(RESULT, record.result.name)
            .put(CRITERIA, toCriteriaJson(record.criteria))

        private fun toResultJson(result: PlacementResult): JSONObject = JSONObject()
            .put(LEVEL, result.level.name)
            .put(SPEAKING_PROFILE, toCriteriaJson(result.speakingProfile))
            .put(ASSESSED_AT_MILLIS, result.assessedAtMillis)

        private fun toCriteriaJson(criteria: SpeakingCriteriaResult): JSONObject = JSONObject()
            .put(RANGE_MET, criteria.rangeMet)
            .put(ACCURACY_MET, criteria.accuracyMet)
            .put(FLUENCY_MET, criteria.fluencyMet)
            .put(INTERACTION_MET, criteria.interactionMet)
            .put(COHERENCE_MET, criteria.coherenceMet)
    }

    fun toJson(): String = checkpointJson

    fun toPlacementCheckpoint(): PlacementCheckpoint? = try {
        val json = JSONObject(checkpointJson)
        require(readLong(json, VERSION_KEY) == VERSION.toLong())
        val records = json.getJSONArray(RECORDS)
        val scenarioIds = json.getJSONArray(USED_SCENARIO_IDS)
        val retryCount = readLong(json, TECHNICAL_RETRY_COUNT)
        require(retryCount in 0..Int.MAX_VALUE.toLong())
        PlacementCheckpoint(
            userId = readString(json, USER_ID),
            step = PlacementStep.valueOf(readString(json, STEP)),
            currentTaskId = readNullableString(json, CURRENT_TASK_ID),
            records = List(records.length()) { readRecord(records.getJSONObject(it)) },
            candidateLevel = SpeakingLevel.valueOf(readString(json, CANDIDATE_LEVEL)),
            usedScenarioIds = List(scenarioIds.length()) { scenarioIds.get(it) as? String ?: throw JSONException(USED_SCENARIO_IDS) },
            technicalRetryCount = retryCount.toInt(),
            createdAtMillis = readLong(json, CREATED_AT_MILLIS),
            expiresAtMillis = readLong(json, EXPIRES_AT_MILLIS),
            pendingResult = if (json.get(PENDING_RESULT) == JSONObject.NULL) null else readResult(json.getJSONObject(PENDING_RESULT))
        )
    } catch (_: JSONException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun readRecord(json: JSONObject): PlacementTaskRecord = PlacementTaskRecord(
        step = PlacementStep.valueOf(readString(json, STEP)),
        taskId = readString(json, TASK_ID),
        scenarioId = readString(json, SCENARIO_ID),
        result = AssessmentResult.valueOf(readString(json, RESULT)),
        criteria = readCriteria(json.getJSONObject(CRITERIA))
    )

    private fun readResult(json: JSONObject): PlacementResult {
        val level = SpeakingLevel.valueOf(readString(json, LEVEL))
        require(level != SpeakingLevel.NOT_ASSESSED)
        return PlacementResult(level, readCriteria(json.getJSONObject(SPEAKING_PROFILE)), readLong(json, ASSESSED_AT_MILLIS))
    }

    private fun readCriteria(json: JSONObject): SpeakingCriteriaResult = SpeakingCriteriaResult(
        rangeMet = readBoolean(json, RANGE_MET),
        accuracyMet = readBoolean(json, ACCURACY_MET),
        fluencyMet = readBoolean(json, FLUENCY_MET),
        interactionMet = readBoolean(json, INTERACTION_MET),
        coherenceMet = readBoolean(json, COHERENCE_MET)
    )

    private fun readString(json: JSONObject, key: String): String = json.get(key) as? String ?: throw JSONException(key)

    private fun readNullableString(json: JSONObject, key: String): String? =
        if (json.get(key) == JSONObject.NULL) null else readString(json, key)

    private fun readBoolean(json: JSONObject, key: String): Boolean = json.get(key) as? Boolean ?: throw JSONException(key)

    private fun readLong(json: JSONObject, key: String): Long = when (val value = json.get(key)) {
        is Long -> value
        is Int -> value.toLong()
        else -> throw JSONException(key)
    }
}
