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
    fun readsInputAndOutputTranscriptionInWireOrder() {
        val frame = JSONObject(
            """{"serverContent":{"outputTranscription":{"text":" Hello"},"inputTranscription":{"text":"Hi","finished":true}}}"""
        )

        assertEquals(
            listOf(
                GeminiLiveEvent.InputTranscription("Hi", true, GENERATION),
                GeminiLiveEvent.OutputTranscription(" Hello", false, GENERATION)
            ),
            parser.readServerEvents(frame, GENERATION)
        )
    }

    @Test
    fun ignoresEmptyTranscriptionChunks() {
        val frame = JSONObject("""{"serverContent":{"inputTranscription":{"text":""},"outputTranscription":{}}}""")

        assertTrue(parser.readServerEvents(frame, GENERATION).isEmpty())
    }

    @Test
    fun transcriptionOnlyFrameDoesNotEmitContent() {
        val frame = JSONObject("""{"serverContent":{"outputTranscription":{"text":"Sure"}}}""")

        assertNull(parser.readContent(frame, GENERATION))
    }

    @Test
    fun readsInterruptionBeforeTurnCompletion() {
        val frame = JSONObject("""{"serverContent":{"interrupted":true,"turnComplete":true}}""")
        val events = parser.readServerEvents(frame, GENERATION)

        assertEquals(GeminiLiveEvent.Interrupted(GENERATION), events.first())
        assertTrue((events.last() as GeminiLiveEvent.Content).isTurnFinished)
    }

    @Test
    fun readsGenerationCompleteAfterModelAudio() {
        val frame = JSONObject(
            """{"serverContent":{"modelTurn":{"parts":[{"inlineData":{"mimeType":"audio/pcm","data":"AAA="}}]},"generationComplete":true}}"""
        )
        val events = parser.readServerEvents(frame, GENERATION)

        assertTrue(events.first() is GeminiLiveEvent.Content)
        assertEquals(GeminiLiveEvent.GenerationComplete(GENERATION), events.last())
    }

    @Test
    fun readsResumptionHandleOnlyWhenResumable() {
        val resumable = JSONObject("""{"sessionResumptionUpdate":{"newHandle":"handle-1","resumable":true}}""")
        val pending = JSONObject("""{"sessionResumptionUpdate":{"resumable":false}}""")

        assertEquals(GeminiLiveEvent.ResumptionUpdate("handle-1", true), parser.readResumptionUpdate(resumable))
        assertEquals(GeminiLiveEvent.ResumptionUpdate("", false), parser.readResumptionUpdate(pending))
    }

    @Test
    fun readsGoAwayTimeLeftAsMillis() {
        assertEquals(GeminiLiveEvent.GoAway(9_500L), parser.readGoAway(JSONObject("""{"goAway":{"timeLeft":"9.5s"}}""")))
        assertEquals(GeminiLiveEvent.GoAway(null), parser.readGoAway(JSONObject("""{"goAway":{}}""")))
        assertEquals(GeminiLiveEvent.GoAway(null), parser.readGoAway(JSONObject("""{"goAway":{"timeLeft":"-1s"}}""")))
    }

    @Test
    fun readsSetupCompleteAsFirstEvent() {
        val frame = JSONObject("""{"setupComplete":{}}""")

        assertEquals(listOf(GeminiLiveEvent.SetupComplete), parser.readServerEvents(frame, null))
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
