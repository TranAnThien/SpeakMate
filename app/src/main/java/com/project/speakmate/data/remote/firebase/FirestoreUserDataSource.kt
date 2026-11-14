package com.project.speakmate.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.data.dto.UserDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreUserDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")

    suspend fun getOrCreateUser(userDto: UserDto): UserDto {
        val docRef = usersCollection.document(userDto.userId)
        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            if (snapshot.exists()) {
                snapshot.toObject(UserDto::class.java)
                    ?: throw IllegalStateException("User document exists but cannot be parsed")
            } else {
                transaction.set(docRef, userDto)
                userDto
            }
        }.await()
    }

    suspend fun getUser(userId: String): UserDto? {
        val documentSnapshot = usersCollection.document(userId).get().await()
        return if (documentSnapshot.exists()) {
            documentSnapshot.toObject(UserDto::class.java)
        } else {
            null
        }
    }
}
