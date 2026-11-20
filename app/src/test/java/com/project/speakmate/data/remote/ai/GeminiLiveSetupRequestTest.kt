package com.project.speakmate.data.remote.ai

import com.project.speakmate.domain.model.Scenario
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiLiveSetupRequestTest {
    private val scenario = Scenario("scenario", "Cafe", "Order a drink", "Act as the barista")
    private val request = GeminiLiveSetupRequest()

    @Test
    fun setupRequestsAudioTranscriptionAutomaticDetectionAndCompression() {
        val root = JSONObject(request.buildSetup(scenario, null))
        assertEquals(setOf("setup"), root.keySet())
        val setup = root.getJSONObject("setup")
        assertEquals("models/gemini-3.8-live", setup.getString("model"))
        assertEquals(listOf("AUDIO"), setup.getJSONObject("generationConfig").getJSONArray("responseModalities").toList())
        assertFalse(setup.getJSONObject("realtimeInputConfig").getJSONObject("automaticActivityDetection").getBoolean("disabled"))
        assertEquals(0, setup.getJSONObject("inputAudioTranscription").length())
        assertEquals(0, setup.getJSONObject("outputAudioTranscription").length())
        assertTrue(setup.getJSONObject("contextWindowCompression").has("slidingWindow"))
    }

    @Test
    fun systemInstructionPreservesScenarioOrderAndEscapesContent() {
        val input = scenario.copy(name = "Name \"quoted\"", description = "line one\nline two", promptTemplate = "\\prompt")
        val setup = JSONObject(request.buildSetup(input, null)).getJSONObject("setup")
        assertEquals("Name \"quoted\"\nline one\nline two\n\\prompt", setup.getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0).getString("text"))
    }

    @Test
    fun resumptionHandleIsPresentOnlyWhenProvided() {
        val initial = JSONObject(request.buildSetup(scenario, null)).getJSONObject("setup").getJSONObject("sessionResumption")
        val resumed = JSONObject(request.buildSetup(scenario, "opaque-handle")).getJSONObject("setup").getJSONObject("sessionResumption")
        assertEquals(0, initial.length())
        assertEquals("opaque-handle", resumed.getString("handle"))
    }
}
