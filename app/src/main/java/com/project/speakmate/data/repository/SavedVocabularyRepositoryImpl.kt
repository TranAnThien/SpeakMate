package com.project.speakmate.data.repository

import com.google.firebase.auth.FirebaseAuth
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
    private val auth: FirebaseAuth
) : SavedVocabularyRepository {

    override fun getSavedVocabularies(): Flow<List<VocabularyItem>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val userRef = firestore.collection("users").document(uid)

        val listener = userRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val dataList = snapshot.get("savedVocabularies") as? List<*> ?: emptyList<Any>()
            val vocabList = dataList.mapNotNull {
                (it as? Map<*, *>)?.let(::mapToVocabularyItem)
            }
            trySend(vocabList)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun saveVocabulary(item: VocabularyItem): Result<VocabularySaveResult> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("User not logged in"))

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

        val userRef = firestore.collection("users").document(uid)

        return try {
            val saveResult = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentDataList =
                    snapshot.get("savedVocabularies") as? List<*> ?: emptyList<Any>()
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
                        currentList.removeLast()
                        finalResult = VocabularySaveResult.SAVED_REPLACING_OLDEST
                    } else {
                        finalResult = VocabularySaveResult.SAVED
                    }
                }

                transaction.update(userRef, "savedVocabularies", currentList)
                finalResult
            }.await()

            Result.success(saveResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeVocabulary(
        word: String,
        partOfSpeech: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("User not logged in"))

        val userRef = firestore.collection("users").document(uid)

        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentDataList =
                    snapshot.get("savedVocabularies") as? List<*> ?: emptyList<Any>()
                val currentList = currentDataList.mapNotNull {
                    (it as? Map<*, *>)?.let(::mapToVocabularyItem)
                }.toMutableList()

                val removed = currentList.removeAll {
                    it.word == word.trim().lowercase() && it.partOfSpeech == partOfSpeech.trim()
                }

                if (removed) {
                    transaction.update(userRef, "savedVocabularies", currentList)
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToVocabularyItem(map: Map<*, *>): VocabularyItem? {
        val word = map["word"] as? String ?: return null
        val mean = map["mean"] as? String ?: return null

        val exampleMap = map["example"] as? Map<*, *>
        val example = LearningExample(
            sentence = exampleMap?.get("sentence") as? String ?: "",
            translate = exampleMap?.get("translate") as? String ?: ""
        )

        return VocabularyItem(
            word = word,
            mean = mean,
            phonetic = map["phonetic"] as? String ?: "",
            partOfSpeech = map["partOfSpeech"] as? String ?: "",
            example = example
        )
    }
}