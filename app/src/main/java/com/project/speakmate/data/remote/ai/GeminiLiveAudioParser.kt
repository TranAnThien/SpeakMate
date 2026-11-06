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
