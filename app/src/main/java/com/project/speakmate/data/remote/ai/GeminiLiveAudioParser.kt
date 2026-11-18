package com.project.speakmate.data.remote.ai

import com.project.speakmate.utils.AppConstant
import java.util.Base64
import org.json.JSONObject

class GeminiLiveAudioParser {
    companion object {
        private const val OUTPUT_MIME_TYPE = "${GeminiWireField.PCM_MIME_TYPE};rate=${AppConstant.CALL_OUTPUT_SAMPLE_RATE}"
        private const val DURATION_SECONDS_SUFFIX = "s"
    }

    fun readServerEvents(frame: JSONObject, generation: Long?): List<GeminiLiveEvent> = buildList {
        if (frame.has(GeminiWireField.SETUP_COMPLETE)) add(GeminiLiveEvent.SetupComplete)
        readSpeechActivity(frame, generation)?.let(::add)
        readInterruption(frame, generation)?.let(::add)
        addAll(readTranscriptions(frame, generation))
        readContent(frame, generation)?.let(::add)
        readGenerationComplete(frame, generation)?.let(::add)
        readResumptionUpdate(frame)?.let(::add)
        readGoAway(frame)?.let(::add)
    }

    fun readTranscriptions(frame: JSONObject, generation: Long?): List<GeminiLiveEvent> {
        val content = frame.optJSONObject(GeminiWireField.SERVER_CONTENT) ?: return emptyList()
        return buildList {
            content.optJSONObject(GeminiWireField.INPUT_TRANSCRIPTION)?.let { transcription ->
                val text = transcription.optString(GeminiWireField.TEXT)
                val isFinished = transcription.optBoolean(GeminiWireField.FINISHED)
                if (text.isNotEmpty() || isFinished) add(GeminiLiveEvent.InputTranscription(text, isFinished, generation))
            }
            content.optJSONObject(GeminiWireField.OUTPUT_TRANSCRIPTION)?.let { transcription ->
                val text = transcription.optString(GeminiWireField.TEXT)
                val isFinished = transcription.optBoolean(GeminiWireField.FINISHED)
                if (text.isNotEmpty() || isFinished) add(GeminiLiveEvent.OutputTranscription(text, isFinished, generation))
            }
        }
    }

    fun readInterruption(frame: JSONObject, generation: Long?): GeminiLiveEvent.Interrupted? {
        val content = frame.optJSONObject(GeminiWireField.SERVER_CONTENT) ?: return null
        return if (content.optBoolean(GeminiWireField.INTERRUPTED)) GeminiLiveEvent.Interrupted(generation) else null
    }

    fun readGenerationComplete(frame: JSONObject, generation: Long?): GeminiLiveEvent.GenerationComplete? {
        val content = frame.optJSONObject(GeminiWireField.SERVER_CONTENT) ?: return null
        return if (content.optBoolean(GeminiWireField.GENERATION_COMPLETE)) GeminiLiveEvent.GenerationComplete(generation) else null
    }

    fun readResumptionUpdate(frame: JSONObject): GeminiLiveEvent.ResumptionUpdate? {
        val update = frame.optJSONObject(GeminiWireField.SESSION_RESUMPTION_UPDATE) ?: return null
        val handle = update.optString(GeminiWireField.NEW_HANDLE)
        return GeminiLiveEvent.ResumptionUpdate(handle, update.optBoolean(GeminiWireField.RESUMABLE) && handle.isNotEmpty())
    }

    fun readGoAway(frame: JSONObject): GeminiLiveEvent.GoAway? {
        val goAway = frame.optJSONObject(GeminiWireField.GO_AWAY) ?: return null
        val timeLeft = goAway.optString(GeminiWireField.TIME_LEFT)
        val timeLeftMillis = timeLeft.takeIf { it.endsWith(DURATION_SECONDS_SUFFIX) }
            ?.removeSuffix(DURATION_SECONDS_SUFFIX)?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it >= 0.0 }
            ?.let { (it * AppConstant.MILLIS_PER_SECOND).toLong() }
        return GeminiLiveEvent.GoAway(timeLeftMillis)
    }

    fun readSpeechActivity(frame: JSONObject, generation: Long?): GeminiLiveEvent.SpeechActivity? =
        when (frame.optJSONObject(GeminiWireField.VOICE_ACTIVITY)?.optString(GeminiWireField.TYPE)) {
            GeminiWireField.ACTIVITY_START -> GeminiLiveEvent.SpeechActivity(true, generation)
            GeminiWireField.ACTIVITY_END -> GeminiLiveEvent.SpeechActivity(false, generation)
            else -> null
        }

    fun readContent(frame: JSONObject, generation: Long?): GeminiLiveEvent.Content? {
        val content = frame.optJSONObject(GeminiWireField.SERVER_CONTENT) ?: return null
        val modelTurn = content.optJSONObject(GeminiWireField.MODEL_TURN)
        if (modelTurn == null && !content.has(GeminiWireField.TURN_COMPLETE) && !content.has(GeminiWireField.WAITING_FOR_INPUT)) return null
        val parts = modelTurn?.optJSONArray(GeminiWireField.PARTS)
        val audio = buildList {
            if (parts != null && generation != null) {
                for (index in 0 until parts.length()) {
                    val inline = parts.optJSONObject(index)?.optJSONObject(GeminiWireField.INLINE_DATA) ?: continue
                    val mimeType = inline.getString(GeminiWireField.MIME_TYPE).replace(" ", "").lowercase()
                    if (!mimeType.startsWith(GeminiWireField.PCM_MIME_TYPE)) continue
                    require(mimeType == GeminiWireField.PCM_MIME_TYPE || mimeType == OUTPUT_MIME_TYPE)
                    val pcm = Base64.getDecoder().decode(inline.getString(GeminiWireField.DATA))
                    require(pcm.size % AppConstant.PCM16_BYTES_PER_SAMPLE == 0)
                    if (pcm.isNotEmpty()) add(pcm)
                }
            }
        }
        val isTurnFinished = content.optBoolean(GeminiWireField.TURN_COMPLETE) && content.optString(GeminiWireField.INTERACTION_STATUS) != GeminiWireField.IN_PROGRESS
        return GeminiLiveEvent.Content(
            audio,
            generation,
            hasModelTurn = modelTurn != null,
            isTurnFinished = isTurnFinished,
            isWaitingForInput = content.optBoolean(GeminiWireField.WAITING_FOR_INPUT)
        )
    }
}
