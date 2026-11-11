package com.project.speakmate.data.repository

import com.project.speakmate.data.audio.AssessmentAudioFileProvider
import com.project.speakmate.data.remote.ai.FakeAssessmentInterceptor
import com.project.speakmate.data.remote.ai.GeminiAssessmentClient
import com.project.speakmate.data.remote.ai.SpeakingEvaluationTestFixtures
import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.RequiredAction
import com.project.speakmate.domain.model.PlacementTransition
import com.project.speakmate.domain.rules.AssessmentTestFixtures
import com.project.speakmate.domain.rules.PlacementRules
import com.project.speakmate.domain.rules.PlacementTestFixtures
import kotlin.random.Random
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.After

class SpeakingAssessmentRepositoryImplTest {
    companion object {
        private const val AUDIO_BYTES = 320_000
        private const val TEST_KEY = "synthetic-key"
        private const val WAIT_SECONDS = 5L
    }

    @get:Rule
    val folder = TemporaryFolder()

    private val httpClients = mutableListOf<OkHttpClient>()

    @After
    fun closeClients() {
        httpClients.forEach { it.dispatcher.executorService.shutdownNow(); it.connectionPool.evictAll() }
    }

    @Test
    fun validEvidenceUsesRulesAndDeletesWav() = runBlocking {
        val (files, evidence) = createEvidence()
        val result = createRepository(files, FakeAssessmentInterceptor()).evaluate(evidence)
        assertEquals(AssessmentResult.PASS, (result as AssessmentOutcome.Evaluated).result)
        assertNull(files.getAudioFile(checkNotNull(evidence.assessmentAudioId)))
    }

    @Test
    fun evidenceGateRunsBeforeFileLookupAndNetwork() = runBlocking {
        val (files, evidence) = createEvidence()
        val fake = FakeAssessmentInterceptor()
        val result = createRepository(files, fake).evaluate(evidence.copy(callOutcome = CallOutcome.FAILED))
        assertEquals(AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.CALL_FAILED), result)
        assertEquals(0, fake.calls.get())
        assertNull(files.getAudioFile(checkNotNull(evidence.assessmentAudioId)))
    }

    @Test
    fun missingAndUnknownAudioAreUnavailableWithoutNetwork() = runBlocking {
        val (files, evidence) = createEvidence()
        val fake = FakeAssessmentInterceptor()
        files.deleteAudio(checkNotNull(evidence.assessmentAudioId))
        val repository = createRepository(files, fake)
        assertEquals(AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE), repository.evaluate(evidence))
        assertEquals(AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE),
            repository.evaluate(evidence.copy(assessmentAudioId = "../outside.wav")))
        assertEquals(0, fake.calls.get())
    }

    @Test
    fun missingAudioConsumesOneTechnicalRetryThroughPlacementRules() = runBlocking {
        val (files, evidence) = createEvidence()
        files.deleteAudio(checkNotNull(evidence.assessmentAudioId))
        val outcome = createRepository(files, FakeAssessmentInterceptor()).evaluate(evidence)
        val checkpoint = PlacementTestFixtures.getCheckpoint()
        val tasks = PlacementTestFixtures.getTasks()
        val task = tasks.first { it.taskId == checkpoint.currentTaskId }
        val transition = PlacementRules.applyOutcome(checkpoint, task, outcome, tasks,
            Random(PlacementTestFixtures.RANDOM_SEED), PlacementTestFixtures.CREATED_AT_MILLIS)
        assertEquals(1, (transition as PlacementTransition.RetryTask).checkpoint.technicalRetryCount)
    }

    @Test
    fun requiredActionGateRemainsOwnedByRules() = runBlocking {
        val (files, evidence) = createEvidence()
        val json = SpeakingEvaluationTestFixtures.getEvaluationJson()
        json.getJSONObject("requiredActions").put(AssessmentTestFixtures.REQUIRED_ACTION_ID,
            JSONObject().put("isCompleted", false).put("userQuote", ""))
        val fake = FakeAssessmentInterceptor().also { it.responseBody = SpeakingEvaluationTestFixtures.getInteractionResponse(json) }
        val input = evidence.copy(requiredActions = listOf(RequiredAction(AssessmentTestFixtures.REQUIRED_ACTION_ID, "Order a drink")))
        val result = createRepository(files, fake).evaluate(input)
        assertEquals(AssessmentResult.FAIL, (result as AssessmentOutcome.Evaluated).result)
    }

    @Test
    fun httpFailureBecomesEvaluationFailedAndDeletesAudio() = runBlocking {
        val (files, evidence) = createEvidence()
        val fake = FakeAssessmentInterceptor().also { it.statusCodes = listOf(403) }
        assertEquals(AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_FAILED),
            createRepository(files, fake).evaluate(evidence))
        assertEquals(1, fake.calls.get())
        assertNull(files.getAudioFile(checkNotNull(evidence.assessmentAudioId)))
    }

    @Test
    fun parseFailureBecomesEvaluationFailedAndDeletesAudio() = runBlocking {
        val (files, evidence) = createEvidence()
        val fake = FakeAssessmentInterceptor().also { it.responseBody = "{" }
        assertEquals(AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_FAILED),
            createRepository(files, fake).evaluate(evidence))
        assertEquals(1, fake.calls.get())
        assertNull(files.getAudioFile(checkNotNull(evidence.assessmentAudioId)))
    }

    @Test
    fun invalidQuoteIsRejectedByRulesAndAudioIsDeleted() = runBlocking {
        val (files, evidence) = createEvidence()
        val json = SpeakingEvaluationTestFixtures.getEvaluationJson()
        json.getJSONObject("range").put("userQuote", "words never spoken")
        val fake = FakeAssessmentInterceptor().also { it.responseBody = SpeakingEvaluationTestFixtures.getInteractionResponse(json) }
        assertEquals(AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_INVALID),
            createRepository(files, fake).evaluate(evidence))
        assertNull(files.getAudioFile(checkNotNull(evidence.assessmentAudioId)))
    }

    @Test
    fun clearAudioEvidenceIsRepeatSafe() = runBlocking {
        val (files, evidence) = createEvidence()
        val repository = createRepository(files, FakeAssessmentInterceptor())
        repository.clearAudioEvidence(checkNotNull(evidence.assessmentAudioId))
        repository.clearAudioEvidence(evidence.assessmentAudioId)
        repository.clearAudioEvidence("../outside.wav")
        assertNull(files.getAudioFile(evidence.assessmentAudioId))
    }

    @Test
    fun cancellationPropagatesAndDeletesAudio() = runBlocking {
        val (files, evidence) = createEvidence()
        val fake = FakeAssessmentInterceptor().also { it.shouldWaitForCancellation = true }
        val repository = createRepository(files, fake)
        val job = launch(Dispatchers.Default) { repository.evaluate(evidence) }
        assertTrue(fake.started.await(WAIT_SECONDS, TimeUnit.SECONDS))
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertTrue(fake.cancelled.await(WAIT_SECONDS, TimeUnit.SECONDS))
        assertNull(files.getAudioFile(checkNotNull(evidence.assessmentAudioId)))
    }

    private fun createEvidence(): Pair<AssessmentAudioFileProvider, AssessmentEvidence> {
        val files = AssessmentAudioFileProvider(folder.root)
        val audioId = files.createAudioId()
        files.openRecorder(audioId).also { it.append(ByteArray(AUDIO_BYTES)); it.close() }
        return files to AssessmentTestFixtures.getEvidence().copy(assessmentAudioId = audioId)
    }

    private fun createRepository(files: AssessmentAudioFileProvider, fake: FakeAssessmentInterceptor): SpeakingAssessmentRepositoryImpl =
        SpeakingAssessmentRepositoryImpl(files, GeminiAssessmentClient(
            OkHttpClient.Builder().addInterceptor(fake).build().also { httpClients += it }, TEST_KEY))
}
