package com.project.speakmate.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SnapshotMetadata
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.util.CustomClassMapper
import com.project.speakmate.data.dto.SpeakingCriteriaResultDto
import com.project.speakmate.data.dto.UserDto
import com.project.speakmate.data.dto.toDomain
import com.project.speakmate.data.dto.toDto
import com.project.speakmate.data.remote.firebase.FirebaseAuthDataSource
import com.project.speakmate.data.remote.firebase.FirestoreCollection
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeCompletion
import com.project.speakmate.domain.model.PracticeSaveException
import com.project.speakmate.domain.model.PracticeSaveFailure
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingActivity
import com.project.speakmate.domain.model.TrainingActivityScenario
import com.project.speakmate.domain.model.TranscriptMessage
import com.project.speakmate.domain.rules.PracticeSaveRetryRules
import com.project.speakmate.test.FakeTrainingRepository
import io.mockk.every
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryRepositoryImplTest {

    private val sampleAttempt = PracticeAttempt(
        attemptId = "attempt_100",
        scenarioId = "cafe_scenario",
        practiceType = PracticeType.TRAINING_UNIT,
        startedAt = Date(1700000000000L),
        endedAt = Date(1700000060000L),
        result = AssessmentResult.PASS,
        criteriaResult = SpeakingCriteriaResult(
            rangeMet = true,
            accuracyMet = true,
            fluencyMet = true,
            interactionMet = true,
            coherenceMet = true
        ),
        requiredActionResults = mapOf("order_coffee" to true),
        transcripts = listOf(
            TranscriptMessage("m1", SenderType.USER, "One latte please", 0, isFinal = true, isInterrupted = false),
            TranscriptMessage("m2", SenderType.AI, "Hot or cold?", 1, isFinal = true, isInterrupted = false)
        ),
        feedbackSummary = "Great pronunciation!"
    )

    @Test
    fun practiceAttemptToDtoMapsAllFieldsCorrectly() {
        val dto = sampleAttempt.toDto()

        assertEquals("attempt_100", dto.attemptId)
        assertEquals("cafe_scenario", dto.scenarioId)
        assertEquals(PracticeType.TRAINING_UNIT.name, dto.practiceType)
        assertEquals(Timestamp(sampleAttempt.startedAt), dto.startedAt)
        assertEquals(Timestamp(sampleAttempt.endedAt), dto.endedAt)
        assertEquals(AssessmentResult.PASS.name, dto.result)
        assertNotNull(dto.criteriaResult)
        assertEquals(true, dto.criteriaResult?.rangeMet)
        assertEquals(mapOf("order_coffee" to true), dto.requiredActionResults)
        assertEquals("Great pronunciation!", dto.feedbackSummary)
    }

    @Test
    fun practiceAttemptDtoToDomainRestoresObjectCorrectly() {
        val dto = sampleAttempt.toDto()
        val domain = requireNotNull(dto.toDomain(sampleAttempt.transcripts))

        assertEquals(sampleAttempt.attemptId, domain.attemptId)
        assertEquals(sampleAttempt.scenarioId, domain.scenarioId)
        assertEquals(sampleAttempt.practiceType, domain.practiceType)
        assertEquals(sampleAttempt.startedAt, domain.startedAt)
        assertEquals(sampleAttempt.endedAt, domain.endedAt)
        assertEquals(sampleAttempt.result, domain.result)
        assertEquals(sampleAttempt.criteriaResult, domain.criteriaResult)
        assertEquals(sampleAttempt.requiredActionResults, domain.requiredActionResults)
        assertEquals(2, domain.transcripts.size)
        assertEquals("Great pronunciation!", domain.feedbackSummary)
    }

    @Test
    fun nullResultAttemptToDtoAndToDomainPreservesNullValues() {
        val nullAttempt = sampleAttempt.copy(
            result = null,
            criteriaResult = null,
            feedbackSummary = "Thiếu bằng chứng"
        )
        val dto = nullAttempt.toDto()
        assertNull(dto.result)
        assertNull(dto.criteriaResult)

        val domain = requireNotNull(dto.toDomain(emptyList()))
        assertNull(domain.result)
        assertNull(domain.criteriaResult)
        assertEquals("Thiếu bằng chứng", domain.feedbackSummary)
    }

    @Test
    fun transcriptMessageDtoMappingPreservesAllFields() {
        val msg = TranscriptMessage("msg_1", SenderType.AI, "Welcome!", 0, isFinal = true, isInterrupted = true)
        val dto = msg.toDto()

        assertEquals("msg_1", dto.messageId)
        assertEquals(SenderType.AI.name, dto.senderType)
        assertEquals("Welcome!", dto.content)
        assertEquals(0, dto.orderIndex)
        assertEquals(true, dto.isFinal)
        assertEquals(true, dto.isInterrupted)

        val backToDomain = dto.toDomain()
        assertEquals(msg, backToDomain)
    }

    @Test
    fun attemptMappingRejectsUnknownPracticeType() {
        assertNull(sampleAttempt.toDto().copy(practiceType = "UNKNOWN_TYPE").toDomain(emptyList()))
    }

    @Test
    fun attemptMappingRejectsUnknownAssessmentResult() {
        assertNull(sampleAttempt.toDto().copy(result = "UNKNOWN_RESULT").toDomain(emptyList()))
    }

    @Test
    fun transcriptMappingRejectsUnknownSenderType() {
        val dto = sampleAttempt.transcripts.first().toDto().copy(senderType = "UNKNOWN_SENDER")
        assertNull(dto.toDomain())
    }

    @Test
    fun transcriptMappingUsesBothLegacyFlagsWhenCanonicalFlagsAreMissing() {
        val dto = sampleAttempt.transcripts.first().toDto().copy(isFinal = null, isInterrupted = null)

        val message = requireNotNull(dto.toDomain(legacyFinal = false, legacyInterrupted = true))

        assertEquals(false, message.isFinal)
        assertEquals(true, message.isInterrupted)
    }

    @Test
    fun transcriptMappingKeepsCanonicalFalseFlagsWhenLegacyFlagsAreTrue() {
        val dto = sampleAttempt.transcripts.first().toDto().copy(isFinal = false, isInterrupted = false)

        val message = requireNotNull(dto.toDomain(legacyFinal = true, legacyInterrupted = true))

        assertEquals(false, message.isFinal)
        assertEquals(false, message.isInterrupted)
    }

    @Test
    fun firestoreMapperWritesAndReadsCanonicalTranscriptFlagsOnly() {
        val message = sampleAttempt.transcripts.first().copy(isFinal = false, isInterrupted = true)
        val persisted = CustomClassMapper.convertToPlainJavaTypes(message.toDto()) as Map<*, *>

        assertEquals(false, persisted["isFinal"])
        assertEquals(true, persisted["isInterrupted"])
        assertTrue("final" !in persisted)
        assertTrue("interrupted" !in persisted)

        val dto = CustomClassMapper.convertToCustomClass(persisted, message.toDto()::class.java, null)
        assertEquals(message, dto.toDomain())
    }

    @Test
    fun firestoreMapperPreservesTheNestedCriteriaFieldNames() {
        val persisted = CustomClassMapper.convertToPlainJavaTypes(sampleAttempt.toDto()) as Map<*, *>
        val criteria = persisted["criteriaResult"] as Map<*, *>

        assertEquals(setOf("rangeMet", "accuracyMet", "fluencyMet", "interactionMet", "coherenceMet"), criteria.keys)
    }

    @Test
    fun attemptMappingRejectsCriteriaWithMissingField() {
        val incompleteCriteria =
            SpeakingCriteriaResultDto(
                rangeMet = true,
                accuracyMet = true,
                fluencyMet = null,
                interactionMet = true,
                coherenceMet = true
            )

        assertNull(sampleAttempt.toDto().copy(criteriaResult = incompleteCriteria).toDomain(emptyList()))
    }

    @Test
    fun saveCompletedPracticeMapsMissingUserProfileInTransactionToInvalidCompletion() = runTest {
        val failure = saveTrainingCompletionWithUser(userDto = null)

        assertEquals(PracticeSaveFailure.INVALID_COMPLETION, failure.reason)
        assertFalse(PracticeSaveRetryRules.canRetry(failure.reason))
    }

    @Test
    fun saveCompletedPracticeMapsMissingTrainingProgressInTransactionToInvalidCompletion() = runTest {
        val failure =
            saveTrainingCompletionWithUser(
                userDto = UserDto(userId = "user-1", level = SpeakingLevel.A1.name, trainingProgress = null)
            )

        assertEquals(PracticeSaveFailure.INVALID_COMPLETION, failure.reason)
        assertFalse(PracticeSaveRetryRules.canRetry(failure.reason))
    }

    private suspend fun saveTrainingCompletionWithUser(userDto: UserDto?): PracticeSaveException {
        val firestore = mockk<FirebaseFirestore>()
        val usersCollection = mockk<CollectionReference>()
        val userRef = mockk<DocumentReference>()
        val attemptsCollection = mockk<CollectionReference>()
        val attemptRef = mockk<DocumentReference>()
        val attemptSnapshot = mockk<DocumentSnapshot>()
        val userSnapshot = mockk<DocumentSnapshot>()
        val serverMetadata = mockk<SnapshotMetadata>()
        val transaction = mockk<Transaction>(relaxed = true)
        val authDataSource = mockk<FirebaseAuthDataSource>()

        every { authDataSource.requireCurrentUserId() } returns "user-1"
        every { firestore.collection(FirestoreCollection.USERS) } returns usersCollection
        every { usersCollection.document("user-1") } returns userRef
        every { userRef.collection(FirestoreCollection.PRACTICE_ATTEMPTS) } returns attemptsCollection
        every { attemptsCollection.document(sampleAttempt.attemptId) } returns attemptRef
        every { serverMetadata.isFromCache } returns false
        every { serverMetadata.hasPendingWrites() } returns false
        every { attemptSnapshot.metadata } returns serverMetadata
        every { attemptSnapshot.exists() } returns false
        every { attemptRef.get(Source.SERVER) } returns Tasks.forResult(attemptSnapshot)
        every { transaction.get(attemptRef) } returns attemptSnapshot
        every { transaction.get(userRef) } returns userSnapshot
        every { userSnapshot.toObject(UserDto::class.java) } returns userDto
        every { firestore.runTransaction(any<Transaction.Function<Unit>>()) } answers {
            val block = firstArg<Transaction.Function<Unit>>()
            try {
                Tasks.forResult(block.apply(transaction))
            } catch (error: Exception) {
                Tasks.forException(error)
            }
        }

        val repository = HistoryRepositoryImpl(firestore, authDataSource, FakeTrainingRepository())
        val training =
            TrainingActivityScenario(
                scenario = Scenario("cafe_scenario", "Cafe", "Order a drink", "Act as a barista"),
                activity = TrainingActivity.UnitActivity(goalIndex = 0, unitIndex = 0),
                targetLevel = SpeakingLevel.A1,
                communicativeGoal = "Order a drink",
                requiredActions = emptyList(),
                difficultyCode = SpeakingLevel.A1.name,
                passConditionCode = AssessmentResult.PASS.name
            )
        val completion = PracticeCompletion(attempt = sampleAttempt, training = training, outcome = null)

        val result = repository.saveCompletedPractice(userId = "user-1", completion = completion)
        return result.exceptionOrNull() as PracticeSaveException
    }
}
