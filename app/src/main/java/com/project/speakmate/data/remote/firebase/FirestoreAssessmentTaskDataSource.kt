package com.project.speakmate.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.project.speakmate.data.dto.AssessmentTaskDto
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreAssessmentTaskDataSource @Inject constructor(private val firestore: FirebaseFirestore) {
    suspend fun getAssessmentTasks(): List<AssessmentTaskDto> = withContext(Dispatchers.IO) {
        firestore.collection(FirestoreCollection.ASSESSMENT_TASKS).get(Source.SERVER).await().documents.mapNotNull { document ->
            document.toObject(AssessmentTaskDto::class.java)?.withDocumentId(document.id)
        }
    }
}
