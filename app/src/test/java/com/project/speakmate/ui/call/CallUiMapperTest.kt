package com.project.speakmate.ui.call

import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.PronunciationState
import com.project.speakmate.ui.call.state.CallDictionaryUiState
import com.project.speakmate.ui.call.state.CallDialog
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.call.state.CallUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallUiMapperTest {
    companion object {
        private const val WORD = "coffee"
        private const val OTHER_WORD = "bread"
    }

    @Test
    fun completionRequiresATerminalStateOrOutcomeAndNeverRunsWhileReadyOrCalling() {
        for (state in CallState.entries) {
            for (outcome in listOf(null) + CallOutcome.entries) {
                val session = CallSessionState(state = state, outcome = outcome)
                val expected = when (state) {
                    CallState.READY, CallState.CALLING -> false
                    CallState.ENDED, CallState.FAILED -> true
                    CallState.ACTIVE, CallState.PAUSED -> outcome != null
                }
                assertEquals(expected, CallUiMapper.shouldComplete(session))
                assertEquals(
                    state in listOf(CallState.CALLING, CallState.ACTIVE, CallState.PAUSED),
                    CallUiMapper.isCallOpen(session)
                )
            }
        }
    }

    @Test
    fun pronunciationTracksOnlyTheWordCurrentlySelectedInAnOpenCall() {
        val state = CallUiState(dictionary = CallDictionaryUiState(word = WORD))
        for (pronunciation in PronunciationState.entries) {
            val session = CallSessionState(
                state = CallState.ACTIVE,
                pronunciation = pronunciation,
                pronunciationText = WORD
            )
            val dictionary = requireNotNull(
                CallUiMapper.applySession(state = state, session = session, isCallOpen = true).dictionary
            )
            assertEquals(
                pronunciation == PronunciationState.LOADING || pronunciation == PronunciationState.PLAYING,
                dictionary.isPronouncing
            )
            assertEquals(pronunciation == PronunciationState.LOADING, dictionary.isPronunciationLoading)
            assertEquals(pronunciation == PronunciationState.FAILED, dictionary.hasPronunciationError)

            val other = session.copy(pronunciationText = OTHER_WORD)
            val otherDictionary = requireNotNull(
                CallUiMapper.applySession(state = state, session = other, isCallOpen = true).dictionary
            )
            assertFalse(otherDictionary.isPronouncing)
            assertFalse(otherDictionary.isPronunciationLoading)
            assertFalse(otherDictionary.hasPronunciationError)
            assertNull(CallUiMapper.applySession(state = state, session = session, isCallOpen = false).dictionary)
        }
    }

    @Test
    fun closingACallClearsTranscriptAndConfirmationAndWaitsForExitConsumption() {
        val state = CallUiState(
            isExiting = true,
            isTranscriptVisible = true,
            dictionary = CallDictionaryUiState(word = WORD),
            activeDialog = CallDialog.EndConfirmation
        )
        val closed = CallSessionState(state = CallState.ENDED, isClosed = true)
        val result = CallUiMapper.applySession(state = state, session = closed, isCallOpen = false)

        assertFalse(result.isTranscriptVisible)
        assertNull(result.dictionary)
        assertNull(result.activeDialog)
        assertTrue(result.isExitReady)
        assertFalse(
            CallUiMapper.applySession(
                state = state.copy(hasConsumedExit = true),
                session = closed,
                isCallOpen = false
            ).isExitReady
        )
    }

    @Test
    fun completionErrorOverridesTerminalPresentationAndPreservesConsumedReadiness() {
        val previous = CallSessionState(state = CallState.ENDED, outcome = CallOutcome.COMPLETED)
        val state = CallUiState(
            hasCompletionError = true,
            finishedSession = previous,
            isFinishedSessionReady = false,
            activeDialog = CallDialog.Title
        )
        val latest = previous.copy(isClosed = true)
        val result = CallUiMapper.applySession(state = state, session = latest, isCallOpen = false)

        assertEquals(CallPresentation.FAILED, result.presentation)
        assertFalse(result.isFinishedSessionReady)
        assertEquals(latest, result.finishedSession)
        assertEquals(CallDialog.Title, result.activeDialog)
    }
}
