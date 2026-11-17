package com.project.speakmate.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.data.dto.toDomain
import com.project.speakmate.data.dto.toDto
import com.project.speakmate.data.remote.firebase.FirestoreUserDataSource
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestoreUserDataSource: FirestoreUserDataSource,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : UserRepository {

    override suspend fun getOrCreateUser(user: User): Result<User> {
        return try {
            val savedDto = firestoreUserDataSource.getOrCreateUser(user.toDto())
            Result.success(savedDto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUser(userId: String): Result<User?> {
        return try {
            val userDto = firestoreUserDataSource.getUser(userId)
            Result.success(userDto?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserLevelAndProgress(
        newLevel: SpeakingLevel,
        criteriaResult: SpeakingCriteriaResult,
        newProgress: TrainingProgress
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val userRef = firestore.collection("users").document(uid)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            if (!snapshot.exists()) {
                throw IllegalStateException("User document not found")
            }

            val updates = mapOf(
                "level" to newLevel.name,
                "latestAssessmentCriteria" to criteriaResult.toDto(),
                "lastAssessedAt" to Timestamp.now(),
                "trainingProgress" to newProgress.toDto()
            )

            transaction.update(userRef, updates)
        }.await()
    }
}
