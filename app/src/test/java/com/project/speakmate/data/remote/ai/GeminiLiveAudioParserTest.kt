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

        assertEquals(
            GeminiLiveEvent.SpeechActivity(isSpeaking = true, playbackGeneration = GENERATION),
            parser
                .readServerEvents(start, GENERATION)
                .filterIsInstance<GeminiLiveEvent.SpeechActivity>()
                .singleOrNull()
        )
        assertEquals(
            GeminiLiveEvent.SpeechActivity(isSpeaking = false, playbackGeneration = GENERATION),
            parser.readServerEvents(end, GENERATION).filterIsInstance<GeminiLiveEvent.SpeechActivity>().singleOrNull()
        )
    }

    @Test
    fun acceptsModelAudioWhenVoiceActivityIsAbsent() {
        val frame =
            JSONObject(
                """{"serverContent":{"modelTurn":{"parts":[""" +
                    """{"inlineData":{"mimeType":"audio/pcm;rate=24000","data":"AAA="}}]}}}"""
            )
        val content =
            checkNotNull(
                parser.readServerEvents(frame, GENERATION).filterIsInstance<GeminiLiveEvent.Content>().singleOrNull()
            )

        assertNull(
            parser
                .readServerEvents(frame, GENERATION)
                .filterIsInstance<GeminiLiveEvent.SpeechActivity>()
                .singleOrNull()
        )
        assertTrue(content.hasModelTurn)
        assertArrayEquals(byteArrayOf(0, 0), content.audio.single())
    }

    @Test
    fun readsInputAndOutputTranscriptionInWireOrder() {
        val frame =
            JSONObject(
                """{"serverContent":{"outputTranscription":{"text":" Hello"},""" +
                    """"inputTranscription":{"text":"Hi","finished":true}}}"""
            )

        assertEquals(
            listOf(
                GeminiLiveEvent.InputTranscription(text = "Hi", isFinished = true),
                GeminiLiveEvent.OutputTranscription(
                    text = " Hello",
                    isFinished = false,
                    playbackGeneration = GENERATION
                )
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

        assertNull(
            parser.readServerEvents(frame, GENERATION).filterIsInstance<GeminiLiveEvent.Content>().singleOrNull()
        )
    }

    @Test
    fun readsInterruptionBeforeTurnCompletion() {
        val frame = JSONObject("""{"serverContent":{"interrupted":true,"turnComplete":true}}""")
        val events = parser.readServerEvents(frame, GENERATION)

        assertEquals(GeminiLiveEvent.Interrupted(GENERATION), events.first())
        assertTrue((events.last() as GeminiLiveEvent.Content).isTurnFinished)
    }

    @Test
    fun readsResumptionHandleOnlyWhenResumable() {
        val resumable = JSONObject("""{"sessionResumptionUpdate":{"newHandle":"handle-1","resumable":true}}""")
        val pending = JSONObject("""{"sessionResumptionUpdate":{"resumable":false}}""")

        assertEquals(
            GeminiLiveEvent.ResumptionUpdate(handle = "handle-1", isResumable = true),
            parser
                .readServerEvents(resumable, GENERATION)
                .filterIsInstance<GeminiLiveEvent.ResumptionUpdate>()
                .single()
        )
        assertEquals(
            GeminiLiveEvent.ResumptionUpdate(handle = "", isResumable = false),
            parser.readServerEvents(pending, GENERATION).filterIsInstance<GeminiLiveEvent.ResumptionUpdate>().single()
        )
    }

    @Test
    fun readsGoAwayWithoutDependingOnItsUnusedTimeLeft() {
        val frame = JSONObject("""{"goAway":{"timeLeft":"9.5s"}}""")
        assertEquals(listOf(GeminiLiveEvent.GoAway), parser.readServerEvents(frame, GENERATION))
    }

    @Test
    fun readsSetupCompleteAsFirstEvent() {
        val frame = JSONObject("""{"setupComplete":{}}""")

        assertEquals(listOf(GeminiLiveEvent.SetupComplete), parser.readServerEvents(frame = frame, generation = null))
    }

    @Test
    fun rejectsMisalignedPcm() {
        val frame =
            JSONObject(
                """{"serverContent":{"modelTurn":{"parts":[""" +
                    """{"inlineData":{"mimeType":"audio/pcm;rate=24000","data":"AA=="}}]}}}"""
            )

        assertThrows(IllegalArgumentException::class.java) {
            parser.readServerEvents(frame, GENERATION).filterIsInstance<GeminiLiveEvent.Content>().singleOrNull()
        }
    }
}
