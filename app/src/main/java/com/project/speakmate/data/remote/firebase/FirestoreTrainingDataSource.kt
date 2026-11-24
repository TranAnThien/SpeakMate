package com.project.speakmate.data.remote.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.domain.model.SpeakingLevel
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class FirestoreTrainingDataSource @Inject constructor(private val firestore: FirebaseFirestore) {
    private val paths
        get() = firestore.collection(FirestoreCollection.TRAINING_PATHS)

    private val goals
        get() = firestore.collection(FirestoreCollection.TRAINING_GOALS)

    private val units
        get() = firestore.collection(FirestoreCollection.TRAINING_UNITS)

    private val assessments
        get() = firestore.collection(FirestoreCollection.GOAL_ASSESSMENTS)

    suspend fun getTrainingPath(level: SpeakingLevel, requireServer: Boolean = false): DocumentSnapshot =
        paths
            .document(level.name)
            .get(requireServer.toFirestoreSource())
            .await()
            .required("Training path ${level.name}")

    suspend fun getTrainingGoal(goalId: String, requireServer: Boolean = false): DocumentSnapshot =
        goals.document(goalId).get(requireServer.toFirestoreSource()).await().required("Training goal $goalId")

    suspend fun getTrainingUnit(unitId: String, requireServer: Boolean = false): DocumentSnapshot =
        units.document(unitId).get(requireServer.toFirestoreSource()).await().required("Training unit $unitId")

    suspend fun getAssessment(assessmentId: String, requireServer: Boolean = false): DocumentSnapshot =
        assessments
            .document(assessmentId)
            .get(requireServer.toFirestoreSource())
            .await()
            .required("Goal assessment $assessmentId")

    suspend fun getVocabulary(id: String): DocumentSnapshot? =
        firestore.collection(FirestoreCollection.VOCABULARIES).document(id).get().await().takeIf { it.exists() }

    suspend fun getGrammar(id: String): DocumentSnapshot? =
        firestore
            .collection(FirestoreCollection.GRAMMAR_POINTS)
            .document(id)
            .get()
            .await()
            .takeIf { it.exists() }

    suspend fun getPattern(id: String): DocumentSnapshot? =
        firestore
            .collection(FirestoreCollection.SENTENCE_PATTERNS)
            .document(id)
            .get()
            .await()
            .takeIf { it.exists() }
}
