package com.project.speakmate.data.dto

import com.project.speakmate.utils.extension.requireBoolean
import com.project.speakmate.utils.extension.requireLong
import com.project.speakmate.utils.extension.requireString
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
        require(json.requireLong(VERSION_KEY) == VERSION.toLong())
        val records = json.getJSONArray(RECORDS)
        val scenarioIds = json.getJSONArray(USED_SCENARIO_IDS)
        val retryCount = json.requireLong(TECHNICAL_RETRY_COUNT)
        require(retryCount in 0..Int.MAX_VALUE.toLong())
        PlacementCheckpoint(
            userId = json.requireString(USER_ID),
            step = PlacementStep.valueOf(json.requireString(STEP)),
            currentTaskId = readNullableString(json, CURRENT_TASK_ID),
            records = List(records.length()) { readRecord(records.getJSONObject(it)) },
            candidateLevel = SpeakingLevel.valueOf(json.requireString(CANDIDATE_LEVEL)),
            usedScenarioIds = List(scenarioIds.length()) { scenarioIds.get(it) as? String ?: throw JSONException(USED_SCENARIO_IDS) },
            technicalRetryCount = retryCount.toInt(),
            createdAtMillis = json.requireLong(CREATED_AT_MILLIS),
            expiresAtMillis = json.requireLong(EXPIRES_AT_MILLIS),
            pendingResult = if (json.get(PENDING_RESULT) == JSONObject.NULL) null else readResult(json.getJSONObject(PENDING_RESULT))
        )
    } catch (_: JSONException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun readRecord(json: JSONObject): PlacementTaskRecord = PlacementTaskRecord(
        step = PlacementStep.valueOf(json.requireString(STEP)),
        taskId = json.requireString(TASK_ID),
        scenarioId = json.requireString(SCENARIO_ID),
        result = AssessmentResult.valueOf(json.requireString(RESULT)),
        criteria = readCriteria(json.getJSONObject(CRITERIA))
    )

    private fun readResult(json: JSONObject): PlacementResult {
        val level = SpeakingLevel.valueOf(json.requireString(LEVEL))
        require(level != SpeakingLevel.NOT_ASSESSED)
        return PlacementResult(level, readCriteria(json.getJSONObject(SPEAKING_PROFILE)), json.requireLong(ASSESSED_AT_MILLIS))
    }

    private fun readCriteria(json: JSONObject): SpeakingCriteriaResult = SpeakingCriteriaResult(
        rangeMet = json.requireBoolean(RANGE_MET),
        accuracyMet = json.requireBoolean(ACCURACY_MET),
        fluencyMet = json.requireBoolean(FLUENCY_MET),
        interactionMet = json.requireBoolean(INTERACTION_MET),
        coherenceMet = json.requireBoolean(COHERENCE_MET)
    )

    private fun readNullableString(json: JSONObject, key: String): String? =
        if (json.get(key) == JSONObject.NULL) null else json.requireString(key)
}
