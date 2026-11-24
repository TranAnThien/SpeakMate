package com.project.speakmate.data.session

import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.utils.AppConstant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CallPronunciationHoldTest {
    @Test
    fun startedAndFinishedCallbacksWaitForTheCommandQueueAndStartedCancelsTimeout() = runTest {
        val media = FakeCallSessionMedia().apply { shouldDelayPronunciationStart = true }
        val current = createSession(media)
        val commands = mutableListOf<() -> Unit>()
        var starts = 0
        var timeouts = 0
        val results = mutableListOf<Boolean>()
        CallPronunciationHold().play(
            current = current,
            text = "coffee",
            revision = current.pronunciationRevision,
            sendCommand = { commands += it },
            isCurrent = { true },
            isLoading = { true },
            onStarted = { starts++ },
            onFinished = { results += it },
            onTimeout = { timeouts++ }
        )
        runCurrent()
        media.startPronunciation()
        assertEquals(0, starts)
        commands.removeAt(0).invoke()
        assertEquals(1, starts)
        advanceTimeBy(AppConstant.PRONUNCIATION_START_TIMEOUT_MILLIS)
        runCurrent()
        assertTrue(commands.isEmpty())
        assertEquals(0, timeouts)
        media.finishPronunciation(isSuccess = true)
        assertTrue(results.isEmpty())
        commands.removeAt(0).invoke()
        assertEquals(listOf(true), results)
    }

    @Test
    fun replacedRevisionRejectsQueuedTimeoutAndLateMediaCallbacks() = runTest {
        val media = FakeCallSessionMedia().apply { shouldDelayPronunciationStart = true }
        val current = createSession(media)
        val commands = mutableListOf<() -> Unit>()
        var callbacks = 0
        CallPronunciationHold().play(
            current = current,
            text = "coffee",
            revision = current.pronunciationRevision,
            sendCommand = { commands += it },
            isCurrent = { true },
            isLoading = { true },
            onStarted = { callbacks++ },
            onFinished = { callbacks++ },
            onTimeout = { callbacks++ }
        )
        runCurrent()
        advanceTimeBy(AppConstant.PRONUNCIATION_START_TIMEOUT_MILLIS)
        runCurrent()
        current.pronunciationRevision++
        media.startPronunciation()
        media.finishPronunciation(isSuccess = true)
        assertEquals(3, commands.size)
        commands.forEach { it() }
        assertEquals(0, callbacks)
    }

    @Test
    fun timeoutRunsOnlyForTheCurrentLoadingSession() = runTest {
        val media = FakeCallSessionMedia().apply { shouldDelayPronunciationStart = true }
        val current = createSession(media)
        val commands = mutableListOf<() -> Unit>()
        var timeouts = 0
        CallPronunciationHold().play(
            current = current,
            text = "coffee",
            revision = current.pronunciationRevision,
            sendCommand = { commands += it },
            isCurrent = { true },
            isLoading = { true },
            onStarted = {},
            onFinished = {},
            onTimeout = { timeouts++ }
        )
        runCurrent()
        advanceTimeBy(AppConstant.PRONUNCIATION_START_TIMEOUT_MILLIS)
        runCurrent()
        assertEquals(0, timeouts)
        commands.single().invoke()
        assertEquals(1, timeouts)
    }

    private fun TestScope.createSession(media: FakeCallSessionMedia): CallSession = CallSession(
        media = media,
        events = Channel(),
        scope = backgroundScope,
        callingAtMillis = 0L,
        mediaRevision = 0L,
        scenario = Scenario("scenario", "Name", "Description", "Prompt"),
        activityType = CallActivityType.FREE_PRACTICE
    ).apply { isPronouncing = true }
}
