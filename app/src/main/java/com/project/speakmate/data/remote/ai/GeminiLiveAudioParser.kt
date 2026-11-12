package com.project.speakmate.data.remote.ai

import com.project.speakmate.utils.AppConstant
import java.util.Base64
import org.json.JSONObject

class GeminiLiveAudioParser {
    companion object {
        private const val VOICE_ACTIVITY = "voiceActivity"
        private const val ACTIVITY_TYPE = "type"
        private const val ACTIVITY_START = "ACTIVITY_START"
        private const val ACTIVITY_END = "ACTIVITY_END"
        private const val SERVER_CONTENT = "serverContent"
        private const val MODEL_TURN = "modelTurn"
        private const val PARTS = "parts"
        private const val INLINE_DATA = "inlineData"
        private const val MIME_TYPE = "mimeType"
        private const val DATA = "data"
        private const val PCM_MIME_TYPE = "audio/pcm"
        private const val OUTPUT_MIME_TYPE = "$PCM_MIME_TYPE;rate=${AppConstant.CALL_OUTPUT_SAMPLE_RATE}"
        private const val TURN_COMPLETE = "turnComplete"
        private const val INTERACTION_STATUS = "interactionStatus"
        private const val IN_PROGRESS = "IN_PROGRESS"
        private const val WAITING_FOR_INPUT = "waitingForInput"
        private const val SETUP_COMPLETE = "setupComplete"
        private const val INPUT_TRANSCRIPTION = "inputTranscription"
        private const val OUTPUT_TRANSCRIPTION = "outputTranscription"
        private const val TRANSCRIPTION_TEXT = "text"
        private const val TRANSCRIPTION_FINISHED = "finished"
        private const val INTERRUPTED = "interrupted"
        private const val GENERATION_COMPLETE = "generationComplete"
        private const val SESSION_RESUMPTION_UPDATE = "sessionResumptionUpdate"
        private const val NEW_HANDLE = "newHandle"
        private const val RESUMABLE = "resumable"
        private const val GO_AWAY = "goAway"
        private const val TIME_LEFT = "timeLeft"
        private const val DURATION_SECONDS_SUFFIX = "s"
        private const val MILLIS_PER_SECOND = 1_000.0
    }

    fun readServerEvents(frame: JSONObject, generation: Long?): List<GeminiLiveEvent> = buildList {
        if (frame.has(SETUP_COMPLETE)) add(GeminiLiveEvent.SetupComplete)
        readSpeechActivity(frame, generation)?.let(::add)
        readInterruption(frame, generation)?.let(::add)
        addAll(readTranscriptions(frame, generation))
        readContent(frame, generation)?.let(::add)
        readGenerationComplete(frame, generation)?.let(::add)
        readResumptionUpdate(frame)?.let(::add)
        readGoAway(frame)?.let(::add)
    }

    fun readTranscriptions(frame: JSONObject, generation: Long?): List<GeminiLiveEvent> {
        val content = frame.optJSONObject(SERVER_CONTENT) ?: return emptyList()
        return buildList {
            content.optJSONObject(INPUT_TRANSCRIPTION)?.let { transcription ->
                val text = transcription.optString(TRANSCRIPTION_TEXT)
                val isFinished = transcription.optBoolean(TRANSCRIPTION_FINISHED)
                if (text.isNotEmpty() || isFinished) add(GeminiLiveEvent.InputTranscription(text, isFinished, generation))
            }
            content.optJSONObject(OUTPUT_TRANSCRIPTION)?.let { transcription ->
                val text = transcription.optString(TRANSCRIPTION_TEXT)
                val isFinished = transcription.optBoolean(TRANSCRIPTION_FINISHED)
                if (text.isNotEmpty() || isFinished) add(GeminiLiveEvent.OutputTranscription(text, isFinished, generation))
            }
        }
    }

    fun readInterruption(frame: JSONObject, generation: Long?): GeminiLiveEvent.Interrupted? {
        val content = frame.optJSONObject(SERVER_CONTENT) ?: return null
        return if (content.optBoolean(INTERRUPTED)) GeminiLiveEvent.Interrupted(generation) else null
    }

    fun readGenerationComplete(frame: JSONObject, generation: Long?): GeminiLiveEvent.GenerationComplete? {
        val content = frame.optJSONObject(SERVER_CONTENT) ?: return null
        return if (content.optBoolean(GENERATION_COMPLETE)) GeminiLiveEvent.GenerationComplete(generation) else null
    }

    fun readResumptionUpdate(frame: JSONObject): GeminiLiveEvent.ResumptionUpdate? {
        val update = frame.optJSONObject(SESSION_RESUMPTION_UPDATE) ?: return null
        val handle = update.optString(NEW_HANDLE)
        return GeminiLiveEvent.ResumptionUpdate(handle, update.optBoolean(RESUMABLE) && handle.isNotEmpty())
    }

    fun readGoAway(frame: JSONObject): GeminiLiveEvent.GoAway? {
        val goAway = frame.optJSONObject(GO_AWAY) ?: return null
        val timeLeft = goAway.optString(TIME_LEFT)
        val timeLeftMillis = timeLeft.takeIf { it.endsWith(DURATION_SECONDS_SUFFIX) }
            ?.removeSuffix(DURATION_SECONDS_SUFFIX)?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it >= 0.0 }
            ?.let { (it * MILLIS_PER_SECOND).toLong() }
        return GeminiLiveEvent.GoAway(timeLeftMillis)
    }

    fun readSpeechActivity(frame: JSONObject, generation: Long?): GeminiLiveEvent.SpeechActivity? =
        when (frame.optJSONObject(VOICE_ACTIVITY)?.optString(ACTIVITY_TYPE)) {
            ACTIVITY_START -> GeminiLiveEvent.SpeechActivity(true, generation)
            ACTIVITY_END -> GeminiLiveEvent.SpeechActivity(false, generation)
            else -> null
        }

    fun readContent(frame: JSONObject, generation: Long?): GeminiLiveEvent.Content? {
        val content = frame.optJSONObject(SERVER_CONTENT) ?: return null
        val modelTurn = content.optJSONObject(MODEL_TURN)
        if (modelTurn == null && !content.has(TURN_COMPLETE) && !content.has(WAITING_FOR_INPUT)) return null
        val parts = modelTurn?.optJSONArray(PARTS)
        val audio = buildList {
            if (parts != null && generation != null) {
                for (index in 0 until parts.length()) {
                    val inline = parts.optJSONObject(index)?.optJSONObject(INLINE_DATA) ?: continue
                    val mimeType = inline.getString(MIME_TYPE).replace(" ", "").lowercase()
                    if (!mimeType.startsWith(PCM_MIME_TYPE)) continue
                    require(mimeType == PCM_MIME_TYPE || mimeType == OUTPUT_MIME_TYPE)
                    val pcm = Base64.getDecoder().decode(inline.getString(DATA))
                    require(pcm.size % AppConstant.PCM16_BYTES_PER_SAMPLE == 0)
                    if (pcm.isNotEmpty()) add(pcm)
                }
            }
        }
        val isTurnFinished = content.optBoolean(TURN_COMPLETE) && content.optString(INTERACTION_STATUS) != IN_PROGRESS
        return GeminiLiveEvent.Content(
            audio,
            generation,
            hasModelTurn = modelTurn != null,
            isTurnFinished = isTurnFinished,
            isWaitingForInput = content.optBoolean(WAITING_FOR_INPUT)
        )
    }
}
