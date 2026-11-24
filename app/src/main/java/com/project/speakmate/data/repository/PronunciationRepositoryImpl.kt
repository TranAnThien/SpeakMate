package com.project.speakmate.data.repository

import com.project.speakmate.data.audio.PronunciationPlayer
import com.project.speakmate.domain.model.PronunciationPlaybackState
import com.project.speakmate.domain.model.PronunciationState
import com.project.speakmate.domain.repository.PronunciationRepository
import com.project.speakmate.utils.AppConstant
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PronunciationRepositoryImpl @Inject constructor(private val player: PronunciationPlayer) :
    PronunciationRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val revision = AtomicLong()
    private val state = MutableStateFlow(PronunciationPlaybackState())
    override val playback = state.asStateFlow()
    private var timeoutJob: Job? = null

    override fun playText(text: String) {
        if (text.isBlank()) return
        val current = state.value
        if (current.text == text &&
            (current.status == PronunciationState.LOADING || current.status == PronunciationState.PLAYING)) {
            stop()
            return
        }
        val request = revision.incrementAndGet()
        timeoutJob?.cancel()
        player.stop()
        state.value = PronunciationPlaybackState(text, PronunciationState.LOADING)
        timeoutJob =
            scope.launch {
                delay(AppConstant.PRONUNCIATION_START_TIMEOUT_MILLIS)
                if (revision.compareAndSet(request, request + 1)) {
                    player.stop()
                    state.value = PronunciationPlaybackState(text, PronunciationState.FAILED)
                }
            }
        try {
            player.speak(
                text,
                onStarted = {
                    if (revision.get() == request) {
                        timeoutJob?.cancel()
                        state.value = PronunciationPlaybackState(text, PronunciationState.PLAYING)
                    }
                }
            ) { isSuccess ->
                if (revision.compareAndSet(request, request + 1)) {
                    timeoutJob?.cancel()
                    state.value =
                        PronunciationPlaybackState(
                            text,
                            if (isSuccess) PronunciationState.IDLE else PronunciationState.FAILED
                        )
                }
            }
        } catch (_: RuntimeException) {
            if (revision.compareAndSet(request, request + 1)) {
                timeoutJob?.cancel()
                state.value = PronunciationPlaybackState(text, PronunciationState.FAILED)
            }
        }
    }

    override fun stop() {
        revision.incrementAndGet()
        timeoutJob?.cancel()
        player.stop()
        state.value = PronunciationPlaybackState()
    }

    override fun release() {
        stop()
        player.release()
        scope.cancel()
    }
}
