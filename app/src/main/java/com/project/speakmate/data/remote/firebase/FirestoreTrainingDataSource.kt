package com.project.speakmate.data.remote.firebase

import com.google.firebase.firestore.Source
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.data.remote.firebase.FirestoreCollection
import com.project.speakmate.data.remote.firebase.required
import com.project.speakmate.domain.model.SpeakingLevel
import kotlinx.coroutines.tasks.await

class FirestoreTrainingDataSource(private val firestore: FirebaseFirestore) {
    private val paths get() = firestore.collection(FirestoreCollection.TRAINING_PATHS)
    private val goals get() = firestore.collection(FirestoreCollection.TRAINING_GOALS)
    private val units get() = firestore.collection(FirestoreCollection.TRAINING_UNITS)
    private val assessments get() = firestore.collection(FirestoreCollection.GOAL_ASSESSMENTS)

    suspend fun getTrainingPath(level: SpeakingLevel, requireServer: Boolean = false): DocumentSnapshot =
        paths.document(level.name).get(if (requireServer) Source.SERVER else Source.DEFAULT).await().required("Training path ${level.name}")

    suspend fun getTrainingGoal(goalId: String, requireServer: Boolean = false): DocumentSnapshot =
        goals.document(goalId).get(if (requireServer) Source.SERVER else Source.DEFAULT).await().required("Training goal $goalId")

    suspend fun getTrainingUnit(unitId: String, requireServer: Boolean = false): DocumentSnapshot =
        units.document(unitId).get(if (requireServer) Source.SERVER else Source.DEFAULT).await().required("Training unit $unitId")

    suspend fun getAssessment(assessmentId: String, requireServer: Boolean = false): DocumentSnapshot =
        assessments.document(assessmentId).get(if (requireServer) Source.SERVER else Source.DEFAULT).await().required("Goal assessment $assessmentId")

    suspend fun getVocabulary(id: String): DocumentSnapshot =
        firestore.collection(FirestoreCollection.VOCABULARIES).document(id).get().await().required("Vocabulary $id")

    suspend fun getGrammar(id: String): DocumentSnapshot =
        firestore.collection(FirestoreCollection.GRAMMAR_POINTS).document(id).get().await().required("Grammar point $id")

    suspend fun getPattern(id: String): DocumentSnapshot =
        firestore.collection(FirestoreCollection.SENTENCE_PATTERNS).document(id).get().await().required("Sentence pattern $id")

}
