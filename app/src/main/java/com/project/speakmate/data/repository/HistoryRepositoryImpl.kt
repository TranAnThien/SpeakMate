package com.project.speakmate.data.repository

import com.project.speakmate.data.remote.firebase.FirestoreCollection
import com.project.speakmate.data.remote.firebase.FirestoreField
import android.util.Log
import com.project.speakmate.utils.extension.suspendRunCatching

import com.project.speakmate.data.remote.firebase.FirebaseAuthDataSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.project.speakmate.data.dto.PracticeAttemptDto
import com.project.speakmate.data.dto.TranscriptMessageDto
import com.project.speakmate.data.dto.toDomain
import com.project.speakmate.data.dto.toDto
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.repository.HistoryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authDataSource: FirebaseAuthDataSource
) : HistoryRepository {

    companion object {
        private const val TAG = "HistoryRepository"
    }

    override suspend fun savePracticeAttempt(attempt: PracticeAttempt): Result<Unit> = suspendRunCatching {
        val uid = authDataSource.requireCurrentUserId()
        val attemptRef = firestore.collection(FirestoreCollection.USERS).document(uid)
            .collection(FirestoreCollection.PRACTICE_ATTEMPTS).document(attempt.attemptId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(attemptRef)
            if (snapshot.exists()) {
                return@runTransaction
            }

            val attemptDto = attempt.toDto()
            transaction.set(attemptRef, attemptDto)

            attempt.transcripts.forEach { msg ->
                val msgRef = attemptRef.collection(FirestoreCollection.MESSAGES).document(msg.messageId)
                transaction.set(msgRef, msg.toDto())
            }
        }.await()
    }

    override fun observePracticeHistory(): Flow<List<PracticeAttempt>> {
        val uid = try {
            authDataSource.requireCurrentUserId()
        } catch (_: IllegalStateException) {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }

        return firestore.collection(FirestoreCollection.USERS).document(uid)
            .collection(FirestoreCollection.PRACTICE_ATTEMPTS)
            .orderBy(FirestoreField.STARTED_AT, Query.Direction.DESCENDING)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    val attempt = try {
                        doc.toObject(PracticeAttemptDto::class.java)?.toDomain(emptyList())
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: RuntimeException) {
                        null
                    }
                    if (attempt == null) Log.w(TAG, "Skipping invalid practice attempt")
                    attempt
                }
            }
    }

    override suspend fun getSessionDetail(attemptId: String): Result<PracticeAttempt> = suspendRunCatching {
        val uid = authDataSource.requireCurrentUserId()
        val attemptRef = firestore.collection(FirestoreCollection.USERS).document(uid)
            .collection(FirestoreCollection.PRACTICE_ATTEMPTS).document(attemptId)

        coroutineScope {
            val attemptSnapshotDeferred = async { attemptRef.get().await() }
            val messagesSnapshotDeferred = async {
                attemptRef.collection(FirestoreCollection.MESSAGES)
                    .orderBy(FirestoreField.ORDER_INDEX)
                    .get().await()
            }

            val attemptSnapshot = attemptSnapshotDeferred.await()
            val messagesSnapshot = messagesSnapshotDeferred.await()

            if (!attemptSnapshot.exists()) {
                throw IllegalArgumentException("Attempt not found")
            }

            val attemptDto = attemptSnapshot.toObject(PracticeAttemptDto::class.java)
                ?: throw IllegalArgumentException("Invalid practice attempt")
            val messages = messagesSnapshot.documents.mapNotNull { document ->
                val message = document.toObject(TranscriptMessageDto::class.java)?.let { dto ->
                    dto.toDomain(
                        legacyFinal = document.get(FirestoreField.FINAL) as? Boolean,
                        legacyInterrupted = document.get(FirestoreField.INTERRUPTED) as? Boolean
                    )
                }
                if (message == null) Log.w(TAG, "Skipping invalid transcript message")
                message
            }

            attemptDto.toDomain(messages) ?: throw IllegalArgumentException("Invalid practice attempt")
        }
    }
}
