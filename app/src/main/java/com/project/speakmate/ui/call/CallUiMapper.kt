package com.project.speakmate.ui.call

import com.project.speakmate.domain.model.CallInterruption
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.PronunciationState
import com.project.speakmate.ui.call.state.CallDictionaryUiState
import com.project.speakmate.ui.call.state.CallDialog
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.call.state.CallUiState

object CallUiMapper {
    fun isCallOpen(session: CallSessionState): Boolean =
        session.state == CallState.CALLING || session.state == CallState.ACTIVE || session.state == CallState.PAUSED

    fun shouldComplete(session: CallSessionState): Boolean =
        (session.outcome != null || session.state == CallState.ENDED || session.state == CallState.FAILED) &&
            session.state != CallState.READY && session.state != CallState.CALLING

    fun applySession(state: CallUiState, session: CallSessionState, isCallOpen: Boolean): CallUiState =
        state.copy(
            isTranscriptVisible = state.isTranscriptVisible && isCallOpen,
            dictionary = applyPronunciation(
                currentDictionary = state.dictionary,
                session = session,
                isCallOpen = isCallOpen
            ),
            presentation = if (state.hasCompletionError) CallPresentation.FAILED else getPresentation(session),
            hasUserSpeech = session.hasUserSpeech,
            isMuted = session.isMuted,
            isResuming = session.isResuming,
            failure = session.failure,
            activeDialog =
                state.activeDialog.takeUnless { dialog ->
                    dialog == CallDialog.EndConfirmation &&
                        !(session.state == CallState.ACTIVE && !session.isFinalizing ||
                            session.state == CallState.PAUSED &&
                                (state.presentation == CallPresentation.PAUSED ||
                                    state.presentation == CallPresentation.RECONNECTING))
                },
            isExitReady = state.isExiting && session.isClosed && !state.hasConsumedExit,
            isFinishedSessionReady =
                state.isFinishedSessionReady || (session.outcome != null && state.finishedSession == null),
            finishedSession = if (session.outcome != null) session else state.finishedSession
        )

    fun getPresentation(session: CallSessionState): CallPresentation =
        when (session.state) {
            CallState.READY -> CallPresentation.READY
            CallState.ENDED ->
                if (session.outcome == CallOutcome.COMPLETED) CallPresentation.FINALIZING else CallPresentation.READY
            CallState.CALLING -> CallPresentation.CONNECTING
            CallState.PAUSED -> {
                if (session.interruption == CallInterruption.NETWORK) {
                    CallPresentation.RECONNECTING
                } else {
                    CallPresentation.PAUSED
                }
            }

            CallState.FAILED -> CallPresentation.FAILED
            CallState.ACTIVE ->
                when {
                    session.isFinalizing -> CallPresentation.FINALIZING
                    session.interruption == CallInterruption.MICROPHONE -> CallPresentation.MICROPHONE_BLOCKED

                    session.isPlaying -> CallPresentation.SPEAKING
                    session.isUserSpeaking && !session.isMuted -> CallPresentation.USER_SPEAKING
                    session.isProcessing -> CallPresentation.PROCESSING
                    session.isAiInterrupted -> CallPresentation.BARGE_IN
                    else -> CallPresentation.LISTENING
                }
        }

    private fun applyPronunciation(
        currentDictionary: CallDictionaryUiState?,
        session: CallSessionState,
        isCallOpen: Boolean
    ): CallDictionaryUiState? =
        currentDictionary.takeIf { isCallOpen }?.let { dictionary ->
            val isCurrent = dictionary.word == session.pronunciationText
            dictionary.copy(
                isPronouncing =
                    isCurrent &&
                        (session.pronunciation == PronunciationState.PLAYING ||
                            session.pronunciation == PronunciationState.LOADING),
                isPronunciationLoading = isCurrent && session.pronunciation == PronunciationState.LOADING,
                hasPronunciationError = isCurrent && session.pronunciation == PronunciationState.FAILED
            )
        }
}
