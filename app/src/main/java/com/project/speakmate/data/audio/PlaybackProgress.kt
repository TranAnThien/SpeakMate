package com.project.speakmate.data.audio

data class PlaybackProgress(
    val playedFrames: Long,
    val enqueuedFrames: Long
) {
    val playedRatio: Float
        get() = if (enqueuedFrames <= 0L) 0f else (playedFrames.toFloat() / enqueuedFrames).coerceIn(0f, 1f)
}
