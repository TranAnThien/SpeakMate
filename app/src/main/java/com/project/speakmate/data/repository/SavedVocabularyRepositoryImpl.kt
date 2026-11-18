package com.project.speakmate.data.repository

import com.project.speakmate.data.remote.firebase.FirestoreCollection
import com.project.speakmate.data.remote.firebase.FirestoreField
import kotlinx.coroutines.CancellationException

import com.project.speakmate.data.remote.firebase.FirebaseAuthDataSource
import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.VocabularySaveResult
import com.project.speakmate.domain.repository.SavedVocabularyRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SavedVocabularyRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authDataSource: FirebaseAuthDataSource
) : SavedVocabularyRepository {

    override fun observeSavedVocabularies(): Flow<List<VocabularyItem>> = callbackFlow {
        val uid = try {
            authDataSource.requireCurrentUserId()
        } catch (_: IllegalStateException) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val userRef = firestore.collection(FirestoreCollection.USERS).document(uid)

        val listener = userRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val dataList = snapshot.get(FirestoreField.SAVED_VOCABULARIES) as? List<*> ?: emptyList<Any>()
            val vocabList = dataList.mapNotNull {
                (it as? Map<*, *>)?.let(::mapToVocabularyItem)
            }
            trySend(vocabList)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun saveVocabulary(item: VocabularyItem): Result<VocabularySaveResult> {
        val uid = try {
            authDataSource.requireCurrentUserId()
        } catch (failure: IllegalStateException) {
            return Result.failure(failure)
        }

        val normalizedWord = item.word.trim().lowercase()
        val normalizedMean = item.mean.trim()

        if (normalizedWord.isBlank() || normalizedMean.isBlank()) {
            return Result.failure(Exception("Word and mean cannot be empty"))
        }

        val validItem = item.copy(
            word = normalizedWord,
            mean = normalizedMean,
            phonetic = item.phonetic.trim().ifEmpty { "" },
            partOfSpeech = item.partOfSpeech.trim().ifEmpty { "" },
            example = if (item.example.sentence.isBlank()) {
                LearningExample(sentence = "", translate = "")
            } else {
                item.example
            }
        )

        val userRef = firestore.collection(FirestoreCollection.USERS).document(uid)

        return try {
            val saveResult = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentDataList =
                    snapshot.get(FirestoreField.SAVED_VOCABULARIES) as? List<*> ?: emptyList<Any>()
                val currentList = currentDataList.mapNotNull {
                    (it as? Map<*, *>)?.let(::mapToVocabularyItem)
                }.toMutableList()

                val existingIndex = currentList.indexOfFirst {
                    it.word == validItem.word &&
                            it.partOfSpeech == validItem.partOfSpeech
                }

                val finalResult: VocabularySaveResult

                if (existingIndex != -1) {
                    currentList.removeAt(existingIndex)
                    currentList.add(0, validItem)
                    finalResult = VocabularySaveResult.MOVED_TO_TOP
                } else {
                    currentList.add(0, validItem)
                    if (currentList.size > 1000) {
                        currentList.removeAt(currentList.lastIndex)
                        finalResult = VocabularySaveResult.SAVED_REPLACING_OLDEST
                    } else {
                        finalResult = VocabularySaveResult.SAVED
                    }
                }

                transaction.update(userRef, FirestoreField.SAVED_VOCABULARIES, currentList)
                finalResult
            }.await()

            Result.success(saveResult)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeVocabulary(
        word: String,
        partOfSpeech: String
    ): Result<Unit> {
        val uid = try {
            authDataSource.requireCurrentUserId()
        } catch (failure: IllegalStateException) {
            return Result.failure(failure)
        }

        val userRef = firestore.collection(FirestoreCollection.USERS).document(uid)

        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentDataList =
                    snapshot.get(FirestoreField.SAVED_VOCABULARIES) as? List<*> ?: emptyList<Any>()
                val currentList = currentDataList.mapNotNull {
                    (it as? Map<*, *>)?.let(::mapToVocabularyItem)
                }.toMutableList()

                val removed = currentList.removeAll {
                    it.word == word.trim().lowercase() && it.partOfSpeech == partOfSpeech.trim()
                }

                if (removed) {
                    transaction.update(userRef, FirestoreField.SAVED_VOCABULARIES, currentList)
                }
            }.await()

            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToVocabularyItem(map: Map<*, *>): VocabularyItem? {
        val word = map[FirestoreField.WORD] as? String ?: return null
        val mean = map[FirestoreField.MEAN] as? String ?: return null

        val exampleMap = map[FirestoreField.EXAMPLE] as? Map<*, *>
        val example = LearningExample(
            sentence = exampleMap?.get(FirestoreField.SENTENCE) as? String ?: "",
            translate = exampleMap?.get(FirestoreField.TRANSLATE) as? String ?: ""
        )

        return VocabularyItem(
            word = word,
            mean = mean,
            phonetic = map[FirestoreField.PHONETIC] as? String ?: "",
            partOfSpeech = map[FirestoreField.PART_OF_SPEECH] as? String ?: "",
            example = example
        )
    }
}
