package com.project.speakmate.data.repository

import android.util.Log
import com.project.speakmate.utils.extension.suspendRunCatching

import com.google.firebase.auth.FirebaseAuth
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
    private val auth: FirebaseAuth
) : HistoryRepository {

    companion object {
        private const val TAG = "HistoryRepository"
    }

    override suspend fun savePracticeAttempt(attempt: PracticeAttempt): Result<Unit> = suspendRunCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val attemptRef = firestore.collection("users").document(uid)
            .collection("practiceAttempts").document(attempt.attemptId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(attemptRef)
            if (snapshot.exists()) {
                return@runTransaction
            }

            val attemptDto = attempt.toDto()
            transaction.set(attemptRef, attemptDto)

            attempt.transcripts.forEach { msg ->
                val msgRef = attemptRef.collection("messages").document(msg.messageId)
                transaction.set(msgRef, msg.toDto())
            }
        }.await()
    }

    override fun getPracticeHistoryList(): Flow<List<PracticeAttempt>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())

        return firestore.collection("users").document(uid)
            .collection("practiceAttempts")
            .orderBy("startedAt", Query.Direction.DESCENDING)
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
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val attemptRef = firestore.collection("users").document(uid)
            .collection("practiceAttempts").document(attemptId)

        coroutineScope {
            val attemptSnapshotDeferred = async { attemptRef.get().await() }
            val messagesSnapshotDeferred = async {
                attemptRef.collection("messages")
                    .orderBy("orderIndex")
                    .get().await()
            }

            val attemptSnapshot = attemptSnapshotDeferred.await()
            val messagesSnapshot = messagesSnapshotDeferred.await()

            if (!attemptSnapshot.exists()) {
                throw IllegalArgumentException("Attempt not found")
            }

            val attemptDto = attemptSnapshot.toObject(PracticeAttemptDto::class.java)
                ?: throw IllegalArgumentException("Invalid practice attempt")
            val messages = messagesSnapshot.documents.mapNotNull {
                val message = it.toObject(TranscriptMessageDto::class.java)?.toDomain()
                if (message == null) Log.w(TAG, "Skipping invalid transcript message")
                message
            }

            attemptDto.toDomain(messages) ?: throw IllegalArgumentException("Invalid practice attempt")
        }
    }
}
