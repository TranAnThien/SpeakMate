package com.project.speakmate.data.remote.ai

import com.project.speakmate.domain.model.Scenario
import org.json.JSONArray
import org.json.JSONObject

class GeminiLiveSetupRequest {
    companion object {
        private const val MODEL = "models/gemini-3.8-live"
    }

    fun buildSetup(scenario: Scenario, resumptionHandle: String?): String {
        val instruction = "${scenario.name}\n" +
            "${scenario.description}\n${scenario.promptTemplate}"
        val generationConfig = JSONObject().put(GeminiWireField.RESPONSE_MODALITIES, JSONArray().put(GeminiWireField.AUDIO_MODALITY))
        val systemInstruction = JSONObject().put(GeminiWireField.PARTS, JSONArray().put(JSONObject().put(GeminiWireField.TEXT, instruction)))
        val realtimeConfig = JSONObject().put(GeminiWireField.AUTOMATIC_ACTIVITY_DETECTION, JSONObject().put(GeminiWireField.DISABLED, false))
        val setup = JSONObject().put(GeminiWireField.MODEL, MODEL)
            .put(GeminiWireField.GENERATION_CONFIG, generationConfig)
            .put(GeminiWireField.SYSTEM_INSTRUCTION, systemInstruction)
            .put(GeminiWireField.REALTIME_INPUT_CONFIG, realtimeConfig)
            .put(GeminiWireField.INPUT_AUDIO_TRANSCRIPTION, JSONObject())
            .put(GeminiWireField.OUTPUT_AUDIO_TRANSCRIPTION, JSONObject())
            .put(GeminiWireField.SESSION_RESUMPTION, JSONObject().apply { resumptionHandle?.let { put(GeminiWireField.RESUMPTION_HANDLE, it) } })
            .put(GeminiWireField.CONTEXT_WINDOW_COMPRESSION, JSONObject().put(GeminiWireField.SLIDING_WINDOW, JSONObject()))
        return JSONObject().put(GeminiWireField.SETUP, setup).toString()
    }
}
