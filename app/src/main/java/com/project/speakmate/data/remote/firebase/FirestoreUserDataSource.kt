package com.project.speakmate.data.remote.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.project.speakmate.data.dto.SpeakingCriteriaResultDto
import com.project.speakmate.data.dto.TrainingProgressDto
import com.project.speakmate.data.dto.UserDto
import com.project.speakmate.data.dto.toDomain
import com.project.speakmate.domain.model.InvalidUserProfileException
import com.project.speakmate.domain.model.SpeakingLevel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreUserDataSource
@Inject
constructor(private val firestore: FirebaseFirestore, private val authDataSource: FirebaseAuthDataSource) {
    private val usersCollection = firestore.collection(FirestoreCollection.USERS)

    suspend fun getOrCreateUser(userDto: UserDto): UserDto {
        val docRef = usersCollection.document(userDto.userId)
        return firestore
            .runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    snapshot.requireUserDto()
                } else {
                    transaction.set(docRef, userDto)
                    userDto
                }
            }
            .await()
    }

    suspend fun getUser(userId: String, requireServer: Boolean = false): UserDto? {
        val documentSnapshot = usersCollection.document(userId).get(requireServer.toFirestoreSource()).await()
        if (requireServer) check(documentSnapshot.metadata.isServerConfirmed())
        return if (documentSnapshot.exists()) {
            documentSnapshot.requireUserDto()
        } else {
            null
        }
    }

    fun observeUser(userId: String): Flow<UserDto?> = flow {
        check(authDataSource.requireCurrentUserId() == userId)
        emitAll(
            usersCollection.document(userId).snapshots().map { snapshot ->
                if (snapshot.exists()) snapshot.requireUserDto() else null
            }
        )
    }

    suspend fun saveAssessedLevel(
        level: String,
        criteria: SpeakingCriteriaResultDto,
        assessedAt: Date,
        trainingProgress: TrainingProgressDto
    ) {
        val document = usersCollection.document(authDataSource.requireCurrentUserId())
        firestore
            .runTransaction { transaction ->
                val current = transaction.get(document).requireUserDto()
                current.toDomain()
                val previousAssessment = current.lastAssessedAt
                if (previousAssessment == assessedAt) {
                    check(current.level == level && current.latestAssessmentCriteria == criteria)
                    return@runTransaction
                }
                check(previousAssessment == null || assessedAt.after(previousAssessment))
                transaction.update(
                    document,
                    mapOf(
                        FirestoreField.LEVEL to level,
                        FirestoreField.LATEST_ASSESSMENT_CRITERIA to criteria,
                        FirestoreField.LAST_ASSESSED_AT to assessedAt,
                        FirestoreField.TRAINING_PROGRESS to trainingProgress
                    )
                )
                Unit
            }
            .await()
    }

    suspend fun markNotAssessed() {
        val document = usersCollection.document(authDataSource.requireCurrentUserId())
        firestore
            .runTransaction { transaction ->
                val current = transaction.get(document).requireUserDto().toDomain()
                check(current.level == SpeakingLevel.NOT_ASSESSED)
                transaction.update(document, FirestoreField.LEVEL, SpeakingLevel.NOT_ASSESSED.name)
            }
            .await()
    }

    private fun DocumentSnapshot.requireUserDto(): UserDto {
        if (contains(FirestoreField.LEVEL) && get(FirestoreField.LEVEL) !is String) {
            throw InvalidUserProfileException(FirestoreField.LEVEL)
        }
        val progress = get(FirestoreField.TRAINING_PROGRESS) as? Map<*, *>
        if (progress?.containsKey(FirestoreField.STAGE) == true && progress[FirestoreField.STAGE] !is String) {
            throw InvalidUserProfileException(FirestoreField.STAGE)
        }
        return checkNotNull(toObject(UserDto::class.java)) { "User document cannot be parsed" }
    }
}
