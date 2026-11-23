package com.project.speakmate.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.project.speakmate.data.dto.PracticeAttemptDto
import com.project.speakmate.data.dto.TranscriptMessageDto
import com.project.speakmate.data.dto.UserDto
import com.project.speakmate.data.dto.toDomain
import com.project.speakmate.data.dto.toDto
import com.project.speakmate.data.remote.firebase.FirebaseAuthDataSource
import com.project.speakmate.data.remote.firebase.FirestoreCollection
import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.domain.model.HistoryReadException
import com.project.speakmate.domain.model.HistoryReadFailure
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeCompletion
import com.project.speakmate.domain.model.PracticeHistoryCursor
import com.project.speakmate.domain.model.PracticeHistoryPage
import com.project.speakmate.domain.model.PracticeSaveException
import com.project.speakmate.domain.model.PracticeSaveFailure
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.TrainingProgressDecision
import com.project.speakmate.domain.model.TranscriptMessage
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.domain.rules.PracticeCommitRules
import com.project.speakmate.domain.rules.TrainingProgressRules
import com.project.speakmate.utils.extension.suspendRunCatching
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authDataSource: FirebaseAuthDataSource,
    private val trainingRepository: TrainingRepository
) : HistoryRepository {

    companion object {
        private const val TAG = "HistoryRepository"
        private const val INVALID_ATTEMPT_MESSAGE = "Skipping invalid practice attempt"
        private const val MAX_FAILURE_DEPTH = 8
        private const val HISTORY_PAGE_SIZE = 20
        private const val NANOS_PER_SECOND = 1_000_000_000
    }

    override suspend fun savePracticeAttempt(attempt: PracticeAttempt): Result<Unit> =
        suspendRunCatching {
            saveCompletedPractice(authDataSource.requireCurrentUserId(), PracticeCompletion(attempt)).getOrThrow()
        }

    override suspend fun saveCompletedPractice(userId: String, completion: PracticeCompletion): Result<Unit> = try {
        withContext(Dispatchers.IO) { saveCompletion(userId, completion) }
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(PracticeSaveException(getSaveFailure(failure), failure))
    }

    private fun getSaveFailure(failure: Throwable): PracticeSaveFailure {
        var current: Throwable? = failure
        var firestoreFailure: FirebaseFirestoreException? = null
        repeat(MAX_FAILURE_DEPTH) {
            val cause = current ?: return@repeat
            if (cause is PracticeSaveException) return cause.reason
            if (cause is FirebaseFirestoreException) firestoreFailure = cause
            current = cause.cause
        }
        return when (firestoreFailure?.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED, FirebaseFirestoreException.Code.UNAUTHENTICATED -> PracticeSaveFailure.ACCESS_DENIED
            FirebaseFirestoreException.Code.UNAVAILABLE, FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.ABORTED, FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> PracticeSaveFailure.NETWORK
            FirebaseFirestoreException.Code.INVALID_ARGUMENT, FirebaseFirestoreException.Code.FAILED_PRECONDITION -> PracticeSaveFailure.INVALID_COMPLETION
            else -> PracticeSaveFailure.UNKNOWN
        }
    }

    private suspend fun saveCompletion(userId: String, completion: PracticeCompletion) {
        check(authDataSource.requireCurrentUserId() == userId)
        val attempt = completion.attempt
        if (attempt.attemptId.isBlank() || attempt.attemptId.contains('/') || !PracticeCommitRules.hasValidTranscripts(attempt)) {
            throw PracticeSaveException(PracticeSaveFailure.INVALID_COMPLETION)
        }
        val training = completion.training
        val userRef = firestore.collection(FirestoreCollection.USERS).document(userId)
        val attemptRef = userRef
            .collection(FirestoreCollection.PRACTICE_ATTEMPTS).document(attempt.attemptId)
        if (isServerCommitted(userId, attemptRef, attempt)) return
        val path = training?.let { trainingRepository.getTrainingPathStructure(it.targetLevel).getOrThrow() }
        check(authDataSource.requireCurrentUserId() == userId)

        firestore.runTransaction { transaction ->
            check(authDataSource.requireCurrentUserId() == userId)
            val snapshot = transaction.get(attemptRef)
            if (snapshot.exists()) {
                val stored = decodeAttempt(snapshot, attempt.transcripts)
                if (!PracticeCommitRules.matches(attempt, stored)) throw PracticeSaveException(PracticeSaveFailure.CONFLICT)
                attempt.transcripts.forEach { message ->
                    val document = transaction.get(attemptRef.collection(FirestoreCollection.MESSAGES).document(message.messageId))
                    if (!document.exists() || decodeMessage(document) != message) throw PracticeSaveException(PracticeSaveFailure.CONFLICT)
                }
                return@runTransaction
            }

            val user = if (training != null) {
                checkNotNull(transaction.get(userRef).toObject(UserDto::class.java)).toDomain()
            } else null
            val progress = user?.trainingProgress
            val decision = if (user != null && training != null && path != null && user.level == training.targetLevel) {
                TrainingProgressRules.applyOutcome(path, checkNotNull(progress), training.activity, checkNotNull(completion.outcome))
            } else null
            val updatedProgress = when (decision) {
                is TrainingProgressDecision.Advanced -> decision.progress
                is TrainingProgressDecision.PathCompleted -> decision.progress
                else -> null
            }

            val attemptDto = attempt.toDto()
            transaction.set(attemptRef, attemptDto)

            attempt.transcripts.forEach { msg ->
                val msgRef = attemptRef.collection(FirestoreCollection.MESSAGES).document(msg.messageId)
                transaction.set(msgRef, msg.toDto())
            }
            if (updatedProgress != null) {
                transaction.update(userRef, FirestoreField.TRAINING_PROGRESS, updatedProgress.toDto())
            }
            Unit
        }.await()
        check(authDataSource.requireCurrentUserId() == userId)
        if (!isServerCommitted(userId, attemptRef, attempt)) throw PracticeSaveException(PracticeSaveFailure.NETWORK)
    }

    private suspend fun isServerCommitted(userId: String, reference: DocumentReference, expected: PracticeAttempt): Boolean {
        check(authDataSource.requireCurrentUserId() == userId)
        val snapshot = reference.get(Source.SERVER).await()
        check(authDataSource.requireCurrentUserId() == userId)
        if (snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites()) throw PracticeSaveException(PracticeSaveFailure.NETWORK)
        if (!snapshot.exists()) return false
        val messages = reference.collection(FirestoreCollection.MESSAGES).get(Source.SERVER).await()
        check(authDataSource.requireCurrentUserId() == userId)
        if (messages.metadata.isFromCache || messages.metadata.hasPendingWrites() || messages.documents.any { it.metadata.hasPendingWrites() }) {
            throw PracticeSaveException(PracticeSaveFailure.NETWORK)
        }
        val actual = decodeAttempt(snapshot, messages.documents.map(::decodeMessage))
        if (!PracticeCommitRules.matches(expected, actual)) throw PracticeSaveException(PracticeSaveFailure.CONFLICT)
        return true
    }

    private fun decodeAttempt(snapshot: DocumentSnapshot, messages: List<TranscriptMessage>): PracticeAttempt = try {
        val dto = snapshot.toObject(PracticeAttemptDto::class.java) ?: throw PracticeSaveException(PracticeSaveFailure.CONFLICT)
        if (dto.startedAt == null || dto.endedAt == null || dto.attemptId != snapshot.id) throw PracticeSaveException(PracticeSaveFailure.CONFLICT)
        dto.toDomain(messages) ?: throw PracticeSaveException(PracticeSaveFailure.CONFLICT)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: RuntimeException) {
        throw PracticeSaveException(PracticeSaveFailure.CONFLICT, failure)
    }

    private fun decodeMessage(snapshot: DocumentSnapshot): TranscriptMessage = try {
        val dto = snapshot.toObject(TranscriptMessageDto::class.java) ?: throw PracticeSaveException(PracticeSaveFailure.CONFLICT)
        if (dto.messageId != snapshot.id) throw PracticeSaveException(PracticeSaveFailure.CONFLICT)
        dto.toDomain(legacyFinal = snapshot.get(FirestoreField.FINAL) as? Boolean,
            legacyInterrupted = snapshot.get(FirestoreField.INTERRUPTED) as? Boolean) ?: throw PracticeSaveException(PracticeSaveFailure.CONFLICT)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: RuntimeException) {
        throw PracticeSaveException(PracticeSaveFailure.CONFLICT, failure)
    }

    override suspend fun getPracticeHistoryPage(practiceType: PracticeType?, cursor: PracticeHistoryCursor?): Result<PracticeHistoryPage> = withContext(Dispatchers.IO) {
        try {
            val uid = authDataSource.requireCurrentUserId()
            if (cursor != null) {
                require(cursor.userId == uid && cursor.practiceType == practiceType)
                require(cursor.attemptId.isNotBlank() && !cursor.attemptId.contains('/'))
                require(cursor.startedAtNanoseconds in 0 until NANOS_PER_SECOND)
            }
            val query = getHistoryQuery(uid, practiceType)
            val paged = if (cursor == null) query else query.startAfter(Timestamp(cursor.startedAtSeconds, cursor.startedAtNanoseconds), cursor.attemptId)
            val snapshot = paged.limit(HISTORY_PAGE_SIZE.toLong()).get(Source.SERVER).await()
            check(authDataSource.requireCurrentUserId() == uid)
            if (snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites()) throw HistoryReadException(HistoryReadFailure.NETWORK)
            Result.success(toHistoryPage(snapshot, uid, practiceType))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Result.failure(toReadException(failure))
        }
    }

    private fun getHistoryQuery(userId: String, practiceType: PracticeType?): Query {
        val collection = firestore.collection(FirestoreCollection.USERS).document(userId).collection(FirestoreCollection.PRACTICE_ATTEMPTS)
        val query = if (practiceType == null) collection else collection.whereEqualTo(FirestoreField.PRACTICE_TYPE, practiceType.name)
        return query.orderBy(FirestoreField.STARTED_AT, Query.Direction.DESCENDING).orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
    }

    private fun toHistoryPage(snapshot: QuerySnapshot, userId: String, practiceType: PracticeType?): PracticeHistoryPage {
        val entries = snapshot.documents.map { document ->
            val startedAt = try { document.getTimestamp(FirestoreField.STARTED_AT) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: RuntimeException) { null }
            val pending = document.metadata.hasPendingWrites()
            val attempt = if (pending) null else try {
                    decodeAttempt(document, emptyList())
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    Log.w(TAG, INVALID_ATTEMPT_MESSAGE)
                    null
                }
            HistoryPageEntry(document.id, startedAt?.seconds, startedAt?.nanoseconds, attempt, pending)
        }
        return HistoryPageMapper.toPage(entries, userId, practiceType, HISTORY_PAGE_SIZE)
    }

    override suspend fun getPracticeAttempt(attemptId: String, requireServer: Boolean): Result<PracticeAttempt> = withContext(Dispatchers.IO) {
        try {
            require(attemptId.isNotBlank() && !attemptId.contains('/'))
            val uid = authDataSource.requireCurrentUserId()
            val snapshot = firestore.collection(FirestoreCollection.USERS).document(uid)
                .collection(FirestoreCollection.PRACTICE_ATTEMPTS).document(attemptId)
                .get(if (requireServer) Source.SERVER else Source.DEFAULT).await()
            check(authDataSource.requireCurrentUserId() == uid)
            if (requireServer && (snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites())) throw HistoryReadException(HistoryReadFailure.NETWORK)
            Result.success(HistoryAttemptMapper.toAttempt(attemptId, snapshot.id.takeIf { snapshot.exists() }, snapshot.toObject(PracticeAttemptDto::class.java)))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Result.failure(toReadException(failure))
        }
    }

    override suspend fun getSessionMessages(attemptId: String, requireServer: Boolean): Result<List<TranscriptMessage>> = withContext(Dispatchers.IO) {
        try {
            require(attemptId.isNotBlank() && !attemptId.contains('/'))
            val uid = authDataSource.requireCurrentUserId()
            val snapshot = firestore.collection(FirestoreCollection.USERS).document(uid)
                .collection(FirestoreCollection.PRACTICE_ATTEMPTS).document(attemptId)
                .collection(FirestoreCollection.MESSAGES).orderBy(FirestoreField.ORDER_INDEX)
                .get(if (requireServer) Source.SERVER else Source.DEFAULT).await()
            check(authDataSource.requireCurrentUserId() == uid)
            if (requireServer && (snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites())) throw HistoryReadException(HistoryReadFailure.NETWORK)
            Result.success(HistoryTranscriptMapper.toMessages(snapshot.documents.map(::decodeMessage)))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Result.failure(toReadException(failure))
        }
    }

    override suspend fun getSessionDetail(attemptId: String, requireServer: Boolean): Result<PracticeAttempt> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            val uid = authDataSource.requireCurrentUserId()
            val attempt = getPracticeAttempt(attemptId, requireServer).getOrThrow()
            val messages = getSessionMessages(attemptId, requireServer).getOrThrow()
            check(authDataSource.requireCurrentUserId() == uid)
            attempt.copy(transcripts = messages)
        }
    }

    private fun toReadException(failure: Exception): HistoryReadException {
        if (failure is HistoryReadException) return failure
        val reason = when (failure) {
            is FirebaseFirestoreException -> when (failure.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED, FirebaseFirestoreException.Code.UNAUTHENTICATED -> HistoryReadFailure.ACCESS_DENIED
                FirebaseFirestoreException.Code.UNAVAILABLE, FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> HistoryReadFailure.NETWORK
                else -> HistoryReadFailure.UNKNOWN
            }
            is PracticeSaveException, is IllegalArgumentException -> HistoryReadFailure.INVALID_DATA
            else -> HistoryReadFailure.UNKNOWN
        }
        return HistoryReadException(reason, failure)
    }
}
