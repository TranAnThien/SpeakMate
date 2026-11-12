package com.project.speakmate.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.data.dto.AssessmentTaskDto
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreAssessmentTaskDataSource @Inject constructor(private val firestore: FirebaseFirestore) {
    companion object {
        private const val COLLECTION_NAME = "assessmentTasks"
    }

    suspend fun getAssessmentTasks(): List<AssessmentTaskDto> = withContext(Dispatchers.IO) {
        firestore.collection(COLLECTION_NAME).get().await().documents.mapNotNull { document ->
            document.toObject(AssessmentTaskDto::class.java)?.withDocumentId(document.id)
        }
    }
}
