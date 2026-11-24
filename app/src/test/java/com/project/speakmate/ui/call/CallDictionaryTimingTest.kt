package com.project.speakmate.ui.call

import androidx.lifecycle.ViewModelStore
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.DictionaryLookupResult
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.TranscriptMessage
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.repository.CallRepository
import com.project.speakmate.domain.repository.DictionaryRepository
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.domain.repository.SavedVocabularyRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.call.state.CallDictionaryStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CallDictionaryTimingTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()
    private val sessions = MutableStateFlow(CallSessionState())
    private val calls = mockk<CallRepository>(relaxed = true)
    private val dictionary = mockk<DictionaryRepository>()
    private val vocabulary = mockk<SavedVocabularyRepository>(relaxed = true)
    private val completion = mockk<PracticeCompletionRepository>(relaxed = true)
    private lateinit var viewModel: CallViewModel
    private val found = DictionaryLookupResult.Found(
        VocabularyItem(
            word = "coffee",
            mean = "meaning",
            phonetic = "",
            partOfSpeech = "noun",
            example = LearningExample("", "")
        )
    )

    @Before
    fun setUp() {
        every { calls.sessionState } returns sessions
        every { calls.audioLevel } returns MutableStateFlow(0f)
        viewModel = CallViewModel(
            repository = calls,
            dictionaryManager = CallDictionaryManager(
                dictionaryRepository = dictionary,
                savedVocabularyRepository = vocabulary,
                repository = calls
            ),
            completionRepository = completion
        )
        store.put("call", viewModel)
        viewModel.startCall(CallActivityContext.FreePractice(Scenario(
            scenarioId = "scenario",
            name = "Name",
            description = "Description",
            promptTemplate = "Prompt"
        )))
        sessions.value = CallSessionState(
            state = CallState.ACTIVE,
            transcript = listOf(TranscriptMessage(
                messageId = "message",
                senderType = SenderType.USER,
                content = "coffee",
                orderIndex = 0,
                isFinal = true,
                isInterrupted = false
            ))
        )
    }

    @After
    fun clear() {
        store.clear()
    }

    @Test
    fun lookupSeesLoadingStateBeforeTheRepositoryRuns() = runTest {
        runCurrent()
        coEvery { dictionary.lookUp("coffee", "coffee") } coAnswers {
            assertEquals(CallDictionaryStatus.LOADING, viewModel.uiState.value.dictionary?.status)
            found
        }
        viewModel.selectWord("message", 0, "coffee")
        runCurrent()
        assertEquals(CallDictionaryStatus.SUCCESS, viewModel.uiState.value.dictionary?.status)
    }

    @Test
    fun pronunciationLoadingIsVisibleInsideTheMainActionBeforeItReturns() = runTest {
        runCurrent()
        coEvery { dictionary.lookUp(any(), any()) } returns found
        viewModel.selectWord("message", 0, "coffee")
        runCurrent()
        withContext(Dispatchers.Main.immediate) {
            viewModel.playDictionaryWord()
            assertTrue(viewModel.uiState.value.dictionary?.isPronunciationLoading == true)
        }
    }

    @Test
    fun lookupIgnoringCancellationCannotReopenAClosedPopup() = runTest {
        runCurrent()
        val gate = CompletableDeferred<DictionaryLookupResult>()
        coEvery { dictionary.lookUp(any(), any()) } coAnswers {
            withContext(NonCancellable) { gate.await() }
        }
        viewModel.selectWord("message", 0, "coffee")
        runCurrent()
        viewModel.closeDictionary()
        gate.complete(found)
        runCurrent()
        assertNull(viewModel.uiState.value.dictionary)
    }
}
