package com.project.speakmate.data.remote.ai

import com.project.speakmate.domain.model.RequiredAction
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.rules.AssessmentTestFixtures
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AssessmentPromptTest {
    @Test
    fun onlyFinalTranscriptIsSentAndInterruptedAiIsMarked() {
        val evidence = AssessmentTestFixtures.getEvidence()
        val transcript = evidence.transcript + listOf(
            AssessmentTestFixtures.getMessage("unfinished secret", isFinal = false, orderIndex = 5),
            AssessmentTestFixtures.getMessage("Interrupted answer", SenderType.AI, orderIndex = 6).copy(isInterrupted = true)
        )
        val prompt = AssessmentPrompt.create(evidence.copy(transcript = transcript))
        assertFalse(prompt.contains("unfinished secret"))
        assertFalse(prompt.contains(AssessmentTestFixtures.AUDIO_ID))
        val json = JSONObject(prompt.substring(prompt.indexOf('{')))
        val messages = json.getJSONArray("transcript")
        assertEquals(transcript.count { it.isFinal }, messages.length())
        assertEquals(true, messages.getJSONObject(messages.length() - 1).getBoolean("isInterrupted"))
    }

    @Test
    fun goalAndRequiredActionsAreStructuredAsEvidence() {
        val evidence = AssessmentTestFixtures.getEvidence().copy(requiredActions = listOf(
            RequiredAction(AssessmentTestFixtures.REQUIRED_ACTION_ID, "Order a drink")
        ))
        val prompt = AssessmentPrompt.create(evidence)
        val json = JSONObject(prompt.substring(prompt.indexOf('{')))
        assertEquals(evidence.communicativeGoal, json.getString("communicativeGoal"))
        assertEquals(AssessmentTestFixtures.REQUIRED_ACTION_ID,
            json.getJSONArray("requiredActions").getJSONObject(0).getString("requiredActionId"))
    }
}
