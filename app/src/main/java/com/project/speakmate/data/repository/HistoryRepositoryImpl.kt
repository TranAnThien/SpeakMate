package com.project.speakmate.data.repository

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

    override suspend fun savePracticeAttempt(attempt: PracticeAttempt): Result<Unit> = runCatching {
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
                    doc.toObject(PracticeAttemptDto::class.java)?.toDomain(emptyList())
                }
            }
    }

    override suspend fun getSessionDetail(attemptId: String): Result<PracticeAttempt> = runCatching {
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

            val attemptDto = attemptSnapshot.toObject(PracticeAttemptDto::class.java)!!
            val messages = messagesSnapshot.documents.mapNotNull {
                it.toObject(TranscriptMessageDto::class.java)?.toDomain()
            }

            attemptDto.toDomain(messages)
        }
    }
}
