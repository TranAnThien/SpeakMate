package com.project.speakmate.data.remote.ai

import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiLiveAudioParserTest {
    companion object {
        private const val GENERATION = 3L
    }

    private val parser = GeminiLiveAudioParser()

    @Test
    fun readsSpeechStartAndEndFromWireType() {
        val start = JSONObject("""{"voiceActivity":{"type":"ACTIVITY_START","audioOffset":"0.160s"}}""")
        val end = JSONObject("""{"voiceActivity":{"type":"ACTIVITY_END"}}""")

        assertEquals(GeminiLiveEvent.SpeechActivity(true, GENERATION), parser.readSpeechActivity(start, GENERATION))
        assertEquals(GeminiLiveEvent.SpeechActivity(false, GENERATION), parser.readSpeechActivity(end, GENERATION))
    }

    @Test
    fun acceptsModelAudioWhenVoiceActivityIsAbsent() {
        val frame = JSONObject(
            """{"serverContent":{"modelTurn":{"parts":[{"inlineData":{"mimeType":"audio/pcm;rate=24000","data":"AAA="}}]}}}"""
        )
        val content = checkNotNull(parser.readContent(frame, GENERATION))

        assertNull(parser.readSpeechActivity(frame, GENERATION))
        assertTrue(content.hasModelTurn)
        assertArrayEquals(byteArrayOf(0, 0), content.audio.single())
    }

    @Test
    fun rejectsMisalignedPcm() {
        val frame = JSONObject(
            """{"serverContent":{"modelTurn":{"parts":[{"inlineData":{"mimeType":"audio/pcm;rate=24000","data":"AA=="}}]}}}"""
        )

        assertThrows(IllegalArgumentException::class.java) {
            parser.readContent(frame, GENERATION)
        }
    }
}
