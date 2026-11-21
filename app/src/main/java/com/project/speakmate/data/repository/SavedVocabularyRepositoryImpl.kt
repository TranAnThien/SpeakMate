package com.project.speakmate.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.data.remote.firebase.FirebaseAuthDataSource
import com.project.speakmate.data.remote.firebase.FirestoreCollection
import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.VocabularySaveResult
import com.project.speakmate.domain.repository.SavedVocabularyRepository
import com.project.speakmate.domain.rules.SavedVocabularyRules
import com.project.speakmate.utils.extension.suspendRunCatching
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SavedVocabularyRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authDataSource: FirebaseAuthDataSource
) : SavedVocabularyRepository {
    override fun observeSavedVocabularies(): Flow<List<VocabularyItem>> = callbackFlow {
        val uid = try {
            authDataSource.requireCurrentUserId()
        } catch (failure: IllegalStateException) {
            close(failure)
            return@callbackFlow
        }
        val listener = firestore.collection(FirestoreCollection.USERS).document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            try {
                checkNotNull(snapshot)
                check(snapshot.exists())
                check(authDataSource.requireCurrentUserId() == uid)
                val fields = checkNotNull(snapshot.data)
                trySend(SavedVocabularyMapper.getRaw(fields).mapNotNull(SavedVocabularyMapper::toItem))
            } catch (failure: Exception) {
                close(failure)
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun saveVocabulary(item: VocabularyItem): Result<VocabularySaveResult> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            val uid = authDataSource.requireCurrentUserId()
            val normalized = SavedVocabularyRules.normalize(item)
            val userRef = firestore.collection(FirestoreCollection.USERS).document(uid)
            firestore.runTransaction { transaction ->
                check(authDataSource.requireCurrentUserId() == uid)
                val snapshot = transaction.get(userRef)
                val prepared = SavedVocabularyMapper.prepareSave(checkNotNull(snapshot.data), userRef.path, normalized)
                transaction.update(userRef, FirestoreField.SAVED_VOCABULARIES, prepared.first)
                prepared.second
            }.await()
        }
    }

    override suspend fun removeVocabulary(word: String, partOfSpeech: String): Result<Unit> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            val uid = authDataSource.requireCurrentUserId()
            val userRef = firestore.collection(FirestoreCollection.USERS).document(uid)
            firestore.runTransaction { transaction ->
                check(authDataSource.requireCurrentUserId() == uid)
                val fields = checkNotNull(transaction.get(userRef).data)
                val current = SavedVocabularyMapper.getRaw(fields)
                val remaining = SavedVocabularyMapper.prepareRemoval(fields, word, partOfSpeech)
                if (remaining.size != current.size) transaction.update(userRef, FirestoreField.SAVED_VOCABULARIES, remaining)
                Unit
            }.await()
        }
    }
}
